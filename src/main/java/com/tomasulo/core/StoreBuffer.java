package com.tomasulo.core;

public class StoreBuffer {

    public enum State {
        FREE,
        ISSUED, // Just issued this cycle, will transition next cycle
        WAITING_FOR_ADDRESS, // Address not yet computed
        WAITING_FOR_VALUE, // Address ready, but waiting for source value via CDB
        EXECUTING // Both address and value ready, performing memory access
    }

    private final String name; // e.g. "S1"
    private final Cache cache; // Reference to cache for determining hit/miss latency

    private Tag tag; // for completeness; not used on CDB
    private boolean busy;
    private long sequenceNumber;

    private int baseRegIndex = -1;
    private int srcRegIndex = -1;
    private int offset = 0;

    private long effectiveAddress = 0;
    private double valueToStore = 0.0;
    private Tag sourceTag = null; // Tag we're waiting on for the value (Q)
    private boolean valueReady = false; // True when we have the value to store
    private int remainingCycles = 0;
    private State state = State.FREE;
    private boolean executionStarted = false; // Track if execution has started
    private boolean addressReady = false; // True when effective address is computed
    private boolean justReceivedOperand = false; // True if operand received from CDB this cycle

    public StoreBuffer(Tag initialTag, Cache cache) {
        this.name = initialTag.name();
        this.cache = cache;
        this.tag = initialTag;
    }

    public double getValueToStore() {
        return valueToStore;
    }

    public Tag getSourceTag() {
        return sourceTag;
    }

    public boolean isBusy() {
        return busy;
    }

    @Override
    public String toString() {
        return name;
    }

    public Tag getTag() {
        return tag;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
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

    /**
     * Issue store instruction to this buffer.
     * Latency is NOT determined here - it will be determined when execution starts
     * (when both address and value are ready) based on cache hit/miss.
     * 
     * @param instr       The store instruction
     * @param regFile     The register file
     * @param producerTag Tag for this store operation
     * @param seqNum      Sequence number for ordering
     */
    public void issue(Instruction instr,
            RegisterFile regFile,
            Tag producerTag,
            long seqNum) {
        this.tag = producerTag;
        this.sequenceNumber = seqNum;
        this.busy = true;

        this.baseRegIndex = instr.getBaseReg();
        this.offset = instr.getOffset();
        this.srcRegIndex = instr.getSrc1Reg();

        // Check if source register has a pending producer (dependency)
        if (srcRegIndex >= 0) {
            Register srcReg = regFile.get(srcRegIndex);
            Tag srcProducer = srcReg.getQi();
            if (srcProducer != null && !Tag.NONE.equals(srcProducer)) {
                // Source value not ready, wait for CDB
                this.sourceTag = srcProducer;
                this.valueReady = false;
                this.valueToStore = 0.0;
            } else {
                // Source value is ready, read it now
                this.sourceTag = null;
                this.valueReady = true;
                this.valueToStore = srcReg.getValue();
            }
        } else {
            // No source register (shouldn't happen for store, but handle it)
            this.sourceTag = null;
            this.valueReady = true;
            this.valueToStore = 0.0;
        }

        this.addressReady = false;
        this.state = State.ISSUED; // Start in ISSUED state for one cycle
        this.remainingCycles = 0; // Will be set when execution starts
    }

    public void setEffectiveAddress(long ea) {
        this.effectiveAddress = ea;
        this.addressReady = true;
        updateState();
    }

    /**
     * Update state based on whether address and value are ready.
     * When both are ready, transition to EXECUTING and determine cache latency.
     * Note: This should NOT be called while in ISSUED state - wait for tick() to
     * transition first.
     * Note: This is called from onCdbBroadcast, so it doesn't have access to buffer
     * lists.
     * Memory ordering will be enforced in tick() instead.
     */
    private void updateState() {
        if (!busy || state == State.EXECUTING || state == State.FREE || state == State.ISSUED) {
            return;
        }

        // Don't transition to EXECUTING here - let tick() handle it with memory
        // ordering checks
        if (addressReady && valueReady && !justReceivedOperand) {
            // NOW we can determine cache hit/miss since we're ready to write
            int accessLatency = cache.getAccessLatency((int) effectiveAddress);
            this.remainingCycles = accessLatency;
            this.executionStarted = false; // Reset for new execution
            state = State.EXECUTING;

            // Log the cache access
            boolean isHit = cache.isHit((int) effectiveAddress);
            System.out.println("[STORE] " + name + " starting execution at EA=" + effectiveAddress +
                    " (latency=" + accessLatency + (isHit ? " HIT" : " MISS") + ")");
        } else if (addressReady && !valueReady) {
            state = State.WAITING_FOR_VALUE;
        }
        // If address not ready, stay in WAITING_FOR_ADDRESS
    }

    /**
     * Called every cycle from the simulator.
     * Handles state transitions: ISSUED -> appropriate next state
     * Execution starts the cycle AFTER state becomes EXECUTING.
     * When finished, the store actually writes to memory.
     */
    public void tick(IMemory memory, java.util.List<LoadBuffer> loadBuffers, java.util.List<StoreBuffer> storeBuffers) {
        if (!busy)
            return;

        // Clear the flag at the start of each cycle
        justReceivedOperand = false;

        // Transition from ISSUED to next appropriate state after one cycle
        if (state == State.ISSUED) {
            if (addressReady && valueReady && AddressUnit.canStoreGoToMemory(this, loadBuffers, storeBuffers)) {
                // Both ready and no memory ordering conflicts, go straight to EXECUTING
                int accessLatency = cache.getAccessLatency((int) effectiveAddress);
                this.remainingCycles = accessLatency;
                this.executionStarted = false;
                state = State.EXECUTING;

                boolean isHit = cache.isHit((int) effectiveAddress);
                System.out.println("[STORE] " + name + " starting execution at EA=" + effectiveAddress +
                        " (latency=" + accessLatency + (isHit ? " HIT" : " MISS") + ")");
            } else if (addressReady && !valueReady) {
                // Address ready but waiting for value
                state = State.WAITING_FOR_VALUE;
            } else {
                // Waiting for address
                state = State.WAITING_FOR_ADDRESS;
            }
            return;
        }

        // If in WAITING_FOR_ADDRESS or WAITING_FOR_VALUE, check if we can now
        // transition to EXECUTING
        if ((state == State.WAITING_FOR_ADDRESS || state == State.WAITING_FOR_VALUE)
                && addressReady && valueReady && !justReceivedOperand
                && AddressUnit.canStoreGoToMemory(this, loadBuffers, storeBuffers)) {
            // Both ready now and no memory ordering conflicts, transition to EXECUTING
            int accessLatency = cache.getAccessLatency((int) effectiveAddress);
            this.remainingCycles = accessLatency;
            this.executionStarted = false;
            state = State.EXECUTING;

            boolean isHit = cache.isHit((int) effectiveAddress);
            System.out.println("[STORE] " + name + " starting execution at EA=" + effectiveAddress +
                    " (latency=" + accessLatency + (isHit ? " HIT" : " MISS") + ")");
        }

        if (state != State.EXECUTING)
            return;

        // First tick in EXECUTING state - mark started but still decrement
        if (!executionStarted) {
            executionStarted = true;
        }

        remainingCycles--;
        if (remainingCycles <= 0) {
            memory.storeDouble((int) effectiveAddress, valueToStore);
            clear();
        }
    }

    /**
     * Clear this buffer and reset to FREE state
     */
    private void clear() {
        busy = false;
        state = State.FREE;
        baseRegIndex = -1;
        srcRegIndex = -1;
        offset = 0;
        effectiveAddress = 0;
        executionStarted = false;
        addressReady = false;
        valueReady = false;
        sourceTag = null;
        valueToStore = 0.0;
        justReceivedOperand = false;
    }

    /**
     * CDB broadcast handler - capture the value if we're waiting for it
     */
    public void onCdbBroadcast(Tag producerTag, double value) {
        if (!busy || sourceTag == null) {
            return;
        }

        if (sourceTag.equals(producerTag)) {
            // Got the value we were waiting for
            valueToStore = value;
            valueReady = true;
            sourceTag = null;
            justReceivedOperand = true; // Set flag to delay execution by one cycle
            updateState();
        }
    }

    public String debugString() {
        String srcStr = formatRegister(srcRegIndex);
        String qStr = (sourceTag != null) ? sourceTag.name() : "0";
        return String.format(
                "%s(tag=%s, busy=%s, state=%s, EA=%d, src=%s, Q=%s, remaining=%d, seq=%d)",
                name, tag, busy, state, effectiveAddress, srcStr, qStr, remainingCycles, sequenceNumber);
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
