package com.tomasulo.core;

public class LoadBuffer {

    public enum State {
        FREE,
        ISSUED, // Just issued this cycle, will transition next cycle
        WAITING_FOR_ADDRESS, // Waiting for base register to compute EA
        EXECUTING, // Address ready, accessing memory
        RESULT_READY // Memory access complete, ready to broadcast on CDB
    }

    private final String name; // e.g. "L1" (for printing)
    private final int memLatency; // fixed latency for loads

    // Tomasulo tag (this is what goes into Qi and onto the CDB)
    private Tag tag;

    private boolean busy;
    private long sequenceNumber;
    private Instruction.Opcode opcode; // Track the load type (LD, LW, L.D, L.S)

    private int destRegIndex = -1;
    private int baseRegIndex = -1;
    private int offset = 0;

    private long effectiveAddress = 0;
    private int remainingCycles = 0;
    private State state = State.FREE;
    private boolean executionStarted = false; // Track if execution has started
    private boolean addressReady = false; // Track if effective address is computed

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

    @Override
    public String toString() {
        return name;
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
     * 
     * @param instr         The load instruction
     * @param regFile       The register file
     * @param producerTag   Tag for this load operation
     * @param seqNum        Sequence number for ordering
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
        this.opcode = instr.getOpcode(); // Store the load type

        this.destRegIndex = instr.getDestReg();
        this.baseRegIndex = instr.getBaseReg();
        this.offset = instr.getOffset();

        this.state = State.ISSUED; // Start in ISSUED state for one cycle
        this.remainingCycles = accessLatency; // Use provided latency
        this.addressReady = false;
        this.executionStarted = false;

        // Mark RF: Qi(dest) = our tag
        // R0 (index 0) is hardwired to 0 and cannot be written to.
        if (destRegIndex > 0) {
            regFile.get(destRegIndex).setQi(tag);
        }
    }

    public boolean isAddressReady() {
        return addressReady;
    }

    public void setEffectiveAddress(long ea) {
        this.effectiveAddress = ea;
        this.addressReady = true;
        // Don't transition to EXECUTING here - let tick() handle it with memory
        // ordering checks
    }

    /**
     * Called every cycle from the simulator.
     * Handles state transitions: ISSUED -> WAITING_FOR_ADDRESS or EXECUTING
     * Execution starts the cycle AFTER state becomes EXECUTING.
     */
    public void tick(IMemory memory, java.util.List<StoreBuffer> storeBuffers) {
        if (!busy)
            return;

        // Transition from ISSUED to next state after one cycle
        if (state == State.ISSUED) {
            if (addressReady && AddressUnit.canLoadGoToMemory(this, storeBuffers)) {
                // Address computed and no memory ordering conflicts, go straight to EXECUTING
                state = State.EXECUTING;
                executionStarted = false;
            } else if (addressReady) {
                // Address ready but waiting for preceding stores
                state = State.WAITING_FOR_ADDRESS; // Reuse this state for memory ordering wait
            } else {
                // Still waiting for address
                state = State.WAITING_FOR_ADDRESS;
            }
            return;
        }

        // If in WAITING_FOR_ADDRESS, check if we can now proceed to EXECUTING
        if (state == State.WAITING_FOR_ADDRESS && addressReady && AddressUnit.canLoadGoToMemory(this, storeBuffers)) {
            state = State.EXECUTING;
            executionStarted = false;
            // Fall through to execution logic below
        }

        if (state != State.EXECUTING)
            return;

        // First tick in EXECUTING state - mark started but still decrement
        // (The ISSUED cycle was the "issue" cycle, now we start counting down)
        if (!executionStarted) {
            executionStarted = true;
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

        double value;
        // Check opcode to determine load type and use appropriate memory operation
        switch (opcode) {
            case LW:
                // Load word (32-bit integer)
                int intValue = memory.loadWord((int) effectiveAddress);
                value = (double) intValue;
                break;
            case LD:
                // Load doubleword (64-bit integer)
                long longValue = memory.loadLong((int) effectiveAddress);
                value = (double) longValue;
                break;
            case L_S:
                // Load single-precision float
                float floatValue = memory.loadFloat((int) effectiveAddress);
                value = (double) floatValue;
                break;
            case L_D:
                // Load double-precision float
                value = memory.loadDouble((int) effectiveAddress);
                break;
            default:
                throw new IllegalStateException("Unsupported load opcode: " + opcode);
        }

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
        opcode = null;
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
        if (regIndex < 0)
            return "none";
        if (regIndex >= 32) {
            return "F" + (regIndex - 32);
        } else {
            return "R" + regIndex;
        }
    }
}
