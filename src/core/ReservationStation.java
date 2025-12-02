package core;

public class ReservationStation {

    public enum State {
        FREE,
        WAITING_FOR_OPERANDS,
        WAITING_FOR_FU,
        EXECUTING,
        RESULT_READY
    }

    private final Tag tag;        // e.g. A1, M2...
    private boolean busy;
    private State state = State.FREE;

    private Instruction instruction;
    private Instruction.Opcode opcode; // or String op if you prefer

    private double Vj;
    private double Vk;
    private Tag Qj = Tag.NONE;
    private Tag Qk = Tag.NONE;

    private int destRegIndex = -1; // -1 if no destination

    public ReservationStation(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

    public boolean isFree() {
        return !busy;
    }

    public State getState() {
        return state;
    }

    public boolean isWaitingForFu() {
        return busy && state == State.WAITING_FOR_FU;
    }

    public boolean isResultReady() {
        return busy && state == State.RESULT_READY;
    }
    public Instruction.Opcode getOpcode()     { return opcode; }
    public double getVj()         { return Vj; }
    public double getVk()         { return Vk; }
    public int getImmediate()     { return instruction.getImmediate(); }


    /**
     * Issue an instruction into this reservation station.
     * Called from the Issue stage when we have a free RS for this opcode.
     */
    public void issue(Instruction instr, RegisterFile regFile) {
        this.busy = true;
        this.instruction = instr;
        this.opcode = instr.getOpcode();
        this.destRegIndex = instr.getDestReg();

        // Source 1
        int s1 = instr.getSrc1Reg();
        if (s1 >= 0) {
            Register r1 = regFile.get(s1);
            if (r1.getQi() == Tag.NONE) {
                Vj = r1.getValue();
                Qj = Tag.NONE;
            } else {
                Qj = r1.getQi();
            }
        } else {
            Qj = Tag.NONE;
        }

        // Source 2
        if (instr.usesSecondSource()) {
            int s2 = instr.getSrc2Reg();
            Register r2 = regFile.get(s2);
            if (r2.getQi() == Tag.NONE) {
                Vk = r2.getValue();
                Qk = Tag.NONE;
            } else {
                Qk = r2.getQi();
            }
        } else {
            Qk = Tag.NONE;
        }

        // Destination register gets Qi = our tag
        if (destRegIndex >= 0) {
            regFile.get(destRegIndex).setQi(tag);
        }

        // Initial state: waiting for operands;
        // if they are already ready (no Q’s), we will move to WAITING_FOR_FU.
        state = State.WAITING_FOR_OPERANDS;
        updateReadyForFu();
    }

    /**
     * Called when CDB broadcasts a value; if we were waiting on that tag, capture it.
     */
    public void onCdbBroadcast(Tag producerTag, double value) {
        if (Qj.equals(producerTag)) {
            Qj = Tag.NONE;
            Vj = value;
        }
        if (Qk.equals(producerTag)) {
            Qk = Tag.NONE;
            Vk = value;
        }

        if (state == State.WAITING_FOR_OPERANDS) {
            updateReadyForFu();
        }
    }

    /**
     * Internal helper: check if both operands are ready and move to WAITING_FOR_FU.
     */
    private void updateReadyForFu() {
        if (Qj == Tag.NONE && Qk == Tag.NONE && state == State.WAITING_FOR_OPERANDS) {
            state = State.WAITING_FOR_FU;
        }
    }

    /**
     * Called exactly once when a FunctionalUnit actually starts executing this RS.
     */
    public void onStartExecution() {
        if (state == State.WAITING_FOR_FU) {
            state = State.EXECUTING;
        }
    }

    /**
     * Called by the FunctionalUnit when its latency is over.
     * We do not compute the result here yet; we just mark that the result is ready for CDB.
     */
    public void onExecutionFinished() {
        if (state == State.EXECUTING) {
            state = State.RESULT_READY;
        }
    }

    /**
     * Build the <tag, value, destReg> message for the CDB.
     * Called once when we decide this RS "wins" the CDB this cycle.
     */
    public CdbMessage buildCdbMessage(double result) {
        if (state != State.RESULT_READY) {
            return null;
        }

        return new CdbMessage(tag, result, destRegIndex);
    }

    /**
     * Called after the CDB broadcast and RF update have been done.
     * Frees this reservation station for reuse.
     */
    public void free() {
        busy = false;
        state = State.FREE;
        instruction = null;
        opcode = null;
        destRegIndex = -1;
        Qj = Tag.NONE;
        Qk = Tag.NONE;
        // Vj/Vk can stay garbage
    }
    public String debugString() {
        return String.format("RS %s [busy=%b, state=%s, op=%s, Vj=%.2f, Vk=%.2f, Qj=%s, Qk=%s, dest=%d]",
                tag, busy, state, opcode, Vj, Vk, Qj, Qk, destRegIndex);
    }

    public int getDestRegIndex() {
        return destRegIndex;
    }
}
