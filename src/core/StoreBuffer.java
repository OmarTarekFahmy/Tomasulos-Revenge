package core;

public class StoreBuffer {

    public enum State {
        FREE,
        WAITING_FOR_ADDRESS,
        EXECUTING
    }

    private final String name;      // e.g. "S1"
    private final int memLatency;

    private Tag tag;                // for completeness; not used on CDB
    private boolean busy;
    private long sequenceNumber;

    private int baseRegIndex = -1;
    private int srcRegIndex = -1;
    private int offset = 0;

    private long effectiveAddress = 0;
    private double valueToStore = 0.0;
    private int remainingCycles = 0;
    private State state = State.FREE;

    public StoreBuffer(Tag initialTag, int memLatency) {
        this.name = initialTag.name();
        this.memLatency = memLatency;
        this.tag = initialTag;
    }

    public boolean isBusy() {
        return busy;
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


    // ************ matches simulator call ************
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

        // In this simplified version we don't track dependences for the store value;
        // we just read it at issue time.
        if (srcRegIndex >= 0) {
            valueToStore = regFile.get(srcRegIndex).getValue();
        }

        this.state = State.WAITING_FOR_ADDRESS;
        this.remainingCycles = memLatency;
    }

    public void setEffectiveAddress(long ea) {
        this.effectiveAddress = ea;
        if (state == State.WAITING_FOR_ADDRESS) {
            state = State.EXECUTING;
        }
    }

    
    /**
     * Called every cycle from the simulator.
     * When finished, the store actually writes to memory.
     */
    public void tick(IMemory memory) {
        if (!busy || state != State.EXECUTING) return;

        remainingCycles--;
        if (remainingCycles <= 0) {
            memory.storeDouble(effectiveAddress, valueToStore);
            busy = false;
            state = State.FREE;
            baseRegIndex = -1;
            srcRegIndex = -1;
            offset = 0;
            effectiveAddress = 0;
        }
    }

    /**
     * CDB calls this but in this simple model stores don't wait on values via tags,
     * so it can be a no-op for now.
     */
    public void onCdbBroadcast(Tag producerTag, double value) {
        // no-op in this simplified version
    }

    public String debugString() {
        return String.format(
                "%s(tag=%s, busy=%s, state=%s, EA=%d, src=R%d, seq=%d)",
                name, tag, busy, state, effectiveAddress, srcRegIndex, sequenceNumber
        );
    }
}
