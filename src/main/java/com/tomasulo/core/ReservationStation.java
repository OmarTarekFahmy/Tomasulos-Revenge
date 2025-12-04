package com.tomasulo.core;

public class ReservationStation {

    public enum State {
        FREE,
        ISSUED,              // Just issued, will check operands next cycle
        WAITING_FOR_OPERANDS,
        WAITING_FOR_FU,
        EXECUTING,
        EXECUTED             // Execution finished, waiting for CDB
    }

    // Type of RS - determines which FU pool it can use
    public enum Type {
        FP_ADD_SUB,
        FP_MUL_DIV,
        INT_ALU
    }

    private final Tag tag;        // e.g. A1, M2...
    private final Type type;      // What kind of operations this RS handles
    private boolean busy;
    private State state = State.FREE;

    private Instruction instruction;
    private Instruction.Opcode opcode; // or String op if you prefer

    private double Vj;
    private double Vk;
    private Tag Qj = Tag.NONE;
    private Tag Qk = Tag.NONE;

    private int destRegIndex = -1; // -1 if no destination

    public ReservationStation(Tag tag, Type type) {
        this.tag = tag;
        this.type = type;
    }

    /**
     * Constructor for backward compatibility (defaults to INT_ALU)
     */
    public ReservationStation(Tag tag) {
        this(tag, Type.INT_ALU);
    }

    public Tag getTag() {
        return tag;
    }

    public Type getType() {
        return type;
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
        return busy && state == State.EXECUTED;
    }
    public Instruction.Opcode getOpcode()     { return opcode; }
    public double getVj()         { return Vj; }
    public double getVk()         { return Vk; }
    public Tag getQj()            { return Qj; }
    public Tag getQk()            { return Qk; }
    public boolean isBusy()       { return busy; }
    public Instruction getInstruction() { return instruction; }
    public int getImmediate()     { return instruction != null ? instruction.getImmediate() : 0; }




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
            if (Tag.NONE.equals(r1.getQi())) {
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
            if (Tag.NONE.equals(r2.getQi())) {
                Vk = r2.getValue();
                Qk = Tag.NONE;
            } else {
                Qk = r2.getQi();
            }
        } else {
            Qk = Tag.NONE;
        }

        // Destination register gets Qi = our tag
        // R0 (index 0) is hardwired to 0 and cannot be written to, so we don't set Qi for it.
        if (destRegIndex > 0) {
            regFile.get(destRegIndex).setQi(tag);
        }

        // Initial state: ISSUED - will transition to WAITING_FOR_OPERANDS next cycle
        state = State.ISSUED;
    }

    /**
     * Called each cycle to advance state from ISSUED to WAITING_FOR_OPERANDS
     */
    public void tick() {
        if (state == State.ISSUED) {
            state = State.WAITING_FOR_OPERANDS;
            updateReadyForFu();
        }
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

        if (state == State.WAITING_FOR_OPERANDS || state == State.ISSUED) {
            updateReadyForFu();
        }
    }

    /**
     * Internal helper: check if both operands are ready and move to WAITING_FOR_FU.
     */
    private void updateReadyForFu() {
        if (Tag.NONE.equals(Qj) && Tag.NONE.equals(Qk) && state == State.WAITING_FOR_OPERANDS) {
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
            state = State.EXECUTED;
        }
    }

    /**
     * Build the <tag, value, destReg> message for the CDB.
     * Called once when we decide this RS "wins" the CDB this cycle.
     */
    public CdbMessage buildCdbMessage(double result) {
        if (state != State.EXECUTED) {
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
        String destStr = formatRegister(destRegIndex);
        return String.format("RS %s [busy=%b, state=%s, op=%s, Vj=%.2f, Vk=%.2f, Qj=%s, Qk=%s, dest=%s]",
                tag, busy, state, opcode, Vj, Vk, Qj, Qk, destStr);
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

    public int getDestRegIndex() {
        return destRegIndex;
    }
}
