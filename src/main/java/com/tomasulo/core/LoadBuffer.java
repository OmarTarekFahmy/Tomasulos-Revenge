package com.tomasulo.core;

public class LoadBuffer {

    public enum State {
        FREE,
        WAITING_FOR_ADDRESS,
        EXECUTING,
        RESULT_READY
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

    // ************ THIS is the missing method ************
    public void issue(Instruction instr,
            RegisterFile regFile,
            Tag producerTag,
            long seqNum) {
        this.tag = producerTag;
        this.sequenceNumber = seqNum;
        this.busy = true;

        this.destRegIndex = instr.getDestReg();
        this.baseRegIndex = instr.getBaseReg();
        this.offset = instr.getOffset();

        this.state = State.WAITING_FOR_ADDRESS;
        this.remainingCycles = memLatency;

        // Mark RF: Qi(dest) = our tag
        if (destRegIndex >= 0) {
            regFile.get(destRegIndex).setQi(tag);
        }
    }

    public void setEffectiveAddress(long ea) {
        this.effectiveAddress = ea;
        if (state == State.WAITING_FOR_ADDRESS) {
            state = State.EXECUTING;
        }
    }

    /**
     * Called every cycle from the simulator.
     */
    public void tick(IMemory memory) {
        if (!busy || state != State.EXECUTING)
            return;

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
    }

    public String debugString() {
        return String.format(
                "%s(tag=%s, busy=%s, state=%s, EA=%d, dest=R%d, seq=%d)",
                name, tag, busy, state, effectiveAddress, destRegIndex, sequenceNumber);
    }
}
