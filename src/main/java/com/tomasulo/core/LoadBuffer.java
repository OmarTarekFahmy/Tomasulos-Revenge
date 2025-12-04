package com.tomasulo.core;

public class LoadBuffer {

    public enum State {
        FREE,
        ISSUED,              // Just issued this cycle, will transition next cycle
        WAITING_FOR_ADDRESS,  // Waiting for base register to compute EA
        EXECUTING,            // Address ready, accessing memory
        RESULT_READY          // Memory access complete, ready to broadcast on CDB
    }

    private final String name; // e.g. "L1" (for printing)
    private final int memLatency; // fixed latency for loads

    // Tomasulo tag (this is what goes into Qi and onto the CDB)
    private Tag tag;

    private boolean busy;
    private long sequenceNumber;

    private int destRegIndex = -1;
    private int baseRegIndex = -1;
    private int offset = 0;

    private long effectiveAddress = 0;
    private int remainingCycles = 0;
    private State state = State.FREE;
    private boolean executionStarted = false;  // Track if execution has started
    private boolean addressReady = false;      // Track if effective address is computed

    public LoadBuffer(Tag initialTag, int memLatency) {
        this.name = initialTag.name(); // just for debug
        this.memLatency = memLatency;
        this.tag = initialTag; // will be overwritten on issue()
    }

    public boolean isBusy() {
        return busy;
    }

    public int getOffset() {
        return offset;
    }

    public int getBaseRegIndex() {
        return baseRegIndex;
    }

    public int getEffectiveAddress() {
        return (int) effectiveAddress;
    }

    public State getState() {
        return state;
    }

    public boolean isFree() {
        return !busy;
    }

    public Tag getTag() {
        return tag;
    }

    public long getSequenceNumber() {


        return sequenceNumber;
    }

    /**
     * Issue with default latency (from constructor)
     */
    public void issue(Instruction instr,
            RegisterFile regFile,
            Tag producerTag,
            long seqNum) {
        issue(instr, regFile, producerTag, seqNum, memLatency);
    }

    /**
     * Issue with specific access latency (based on cache hit/miss)
     * @param instr The load instruction
     * @param regFile The register file
     * @param producerTag Tag for this load operation
     * @param seqNum Sequence number for ordering
     * @param accessLatency Latency in cycles (cache hit or miss penalty)
     */
    public void issue(Instruction instr,
            RegisterFile regFile,
            Tag producerTag,
            long seqNum,
            int accessLatency) {
        this.tag = producerTag;
        this.sequenceNumber = seqNum;
        this.busy = true;

        this.destRegIndex = instr.getDestReg();
        this.baseRegIndex = instr.getBaseReg();
        this.offset = instr.getOffset();

        this.state = State.ISSUED;  // Start in ISSUED state for one cycle
        this.remainingCycles = accessLatency;  // Use provided latency
        this.addressReady = false;
        this.executionStarted = false;

        // Mark RF: Qi(dest) = our tag
        // R0 (index 0) is hardwired to 0 and cannot be written to.
        if (destRegIndex > 0) {
            regFile.get(destRegIndex).setQi(tag);
        }
    }

    public void setEffectiveAddress(long ea) {
        this.effectiveAddress = ea;
        this.addressReady = true;
        // If already past ISSUED state, transition to EXECUTING
        if (state == State.WAITING_FOR_ADDRESS) {
            state = State.EXECUTING;
            executionStarted = false;  // Reset for new execution
        }
    }

    /**
     * Called every cycle from the simulator.
     * Handles state transitions: ISSUED -> WAITING_FOR_ADDRESS or EXECUTING
     * Execution starts the cycle AFTER state becomes EXECUTING.
     */
    public void tick(IMemory memory) {
        if (!busy)
            return;

        // Transition from ISSUED to next state after one cycle
        if (state == State.ISSUED) {
            if (addressReady) {
                // Address already computed, go straight to EXECUTING
                state = State.EXECUTING;
                executionStarted = false;
            } else {
                // Still waiting for address
                state = State.WAITING_FOR_ADDRESS;
            }
            return;
        }

        if (state != State.EXECUTING)
            return;

        // First tick after entering EXECUTING state just marks execution started
        if (!executionStarted) {
            executionStarted = true;
            return;  // Don't decrement on the first cycle
        }

        remainingCycles--;
        if (remainingCycles <= 0) {
            state = State.RESULT_READY;
        }
    }

    public boolean isCdbReady() {
        return busy && state == State.RESULT_READY;
    }

    /**
     * Create the CDB message once the load has finished.
     */
    public CdbMessage produceCdbMessage(IMemory memory) {
        if (!isCdbReady())
            return null;
        double value = memory.loadDouble((int) effectiveAddress);
        return new CdbMessage(tag, value, destRegIndex);
    }

    /**
     * After the CDB broadcast, clean up Qi and free this buffer.
     */
    public void onCdbWrittenBack(RegisterFile regFile) {
        if (destRegIndex >= 0) {
            Register r = regFile.get(destRegIndex);
            if (tag.equals(r.getQi())) {
                r.setQi(Tag.NONE);
            }
        }
        busy = false;
        state = State.FREE;
        destRegIndex = -1;
        baseRegIndex = -1;
        offset = 0;
        effectiveAddress = 0;
        remainingCycles = 0;
        executionStarted = false;
        addressReady = false;
    }

    public String debugString() {
        String destStr = formatRegister(destRegIndex);
        return String.format(
                "%s(tag=%s, busy=%s, state=%s, EA=%d, dest=%s, remaining=%d, seq=%d)",
                name, tag, busy, state, effectiveAddress, destStr, remainingCycles, sequenceNumber);
    }

    /**
     * Format register index as R# or F# depending on whether it's INT or FP
     */
    private String formatRegister(int regIndex) {
        if (regIndex < 0) return "none";
        if (regIndex >= 32) {
            return "F" + (regIndex - 32);
        } else {
            return "R" + regIndex;
        }
    }
}
