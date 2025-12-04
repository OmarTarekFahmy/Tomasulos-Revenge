package com.tomasulo.core;

/**
 * Handles BEQ and BNE branch instructions in the Tomasulo simulator.
 * 
 * Branch handling strategy:
 * - Branches are issued to a dedicated branch handler
 * - They wait for operands like other instructions
 * - When operands are ready, the branch is evaluated
 * - If the branch is taken, PC is updated
 */
public class BranchHandler {

    public enum State {
        FREE,
        ISSUED,              // Just issued this cycle
        WAITING_FOR_OPERANDS,// Waiting for source register values
        READY,               // Operands ready, can evaluate
        RESOLVED             // Branch has been evaluated
    }

    private final String name;
    private final Tag tag;
    
    private State state = State.FREE;
    private boolean busy = false;
    
    private Instruction instruction;
    private Instruction.Opcode opcode;
    
    // Source operands
    private double Vj;           // First source value
    private double Vk;           // Second source value
    private Tag Qj = Tag.NONE;   // Tag for first source
    private Tag Qk = Tag.NONE;   // Tag for second source
    
    // Branch info
    private int currentPC;       // PC of this branch instruction
    private int targetAddress;   // Target if branch is taken (from immediate/label)
    
    // Result
    private boolean branchTaken = false;
    private int nextPC;          // Computed next PC after evaluation
    
    public BranchHandler(Tag tag) {
        this.tag = tag;
        this.name = tag.name();
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean isFree() {
        return !busy;
    }

    public State getState() {
        return state;
    }

    public Tag getTag() {
        return tag;
    }

    public boolean isBranchTaken() {
        return branchTaken;
    }

    public int getNextPC() {
        return nextPC;
    }

    public int getTargetAddress() {
        return targetAddress;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    /**
     * Issue a BEQ or BNE branch instruction to this handler.
     * 
     * @param instr The branch instruction
     * @param regFile Register file for reading operands
     * @param pc Current program counter (index of this instruction)
     */
    public void issue(Instruction instr, RegisterFile regFile, int pc) {
        this.busy = true;
        this.instruction = instr;
        this.opcode = instr.getOpcode();
        this.currentPC = pc;
        this.branchTaken = false;
        
        // Target address is stored in immediate field (label index)
        this.targetAddress = instr.getImmediate();
        
        // Read first source operand
        int s1 = instr.getSrc1Reg();
        if (s1 >= 0) {
            Register r1 = regFile.get(s1);
            if (r1.getQi() == Tag.NONE || r1.getQi() == null) {
                Vj = r1.getValue();
                Qj = Tag.NONE;
            } else {
                Qj = r1.getQi();
            }
        } else {
            Vj = 0;
            Qj = Tag.NONE;
        }
        
        // Read second source for BEQ, BNE
        int s2 = instr.getSrc2Reg();
        if (s2 >= 0) {
            Register r2 = regFile.get(s2);
            if (r2.getQi() == Tag.NONE || r2.getQi() == null) {
                Vk = r2.getValue();
                Qk = Tag.NONE;
            } else {
                Qk = r2.getQi();
            }
        } else {
            Vk = 0;
            Qk = Tag.NONE;
        }
        
        this.state = State.ISSUED;
    }

    /**
     * Called each cycle to advance state machine
     */
    public void tick() {
        if (!busy) return;
        
        if (state == State.ISSUED) {
            state = State.WAITING_FOR_OPERANDS;
            checkOperandsReady();
        }
    }

    /**
     * Called when CDB broadcasts a value
     */
    public void onCdbBroadcast(Tag producerTag, double value) {
        if (!busy) return;
        
        if (Qj != null && Qj.equals(producerTag)) {
            Qj = Tag.NONE;
            Vj = value;
        }
        if (Qk != null && Qk.equals(producerTag)) {
            Qk = Tag.NONE;
            Vk = value;
        }
        
        if (state == State.WAITING_FOR_OPERANDS || state == State.ISSUED) {
            checkOperandsReady();
        }
    }

    /**
     * Check if all operands are ready and transition to READY state
     */
    private void checkOperandsReady() {
        boolean jReady = (Qj == null || Qj == Tag.NONE);
        boolean kReady = (Qk == null || Qk == Tag.NONE);
        
        if (jReady && kReady && state == State.WAITING_FOR_OPERANDS) {
            state = State.READY;
        }
    }

    /**
     * Check if branch is ready to be evaluated
     */
    public boolean isReadyToEvaluate() {
        return busy && state == State.READY;
    }

    /**
     * Check if branch has been resolved
     */
    public boolean isResolved() {
        return busy && state == State.RESOLVED;
    }

    /**
     * Evaluate the branch and determine if taken and next PC.
     * Returns true if branch was taken.
     */
    public boolean evaluate() {
        if (state != State.READY) {
            return false;
        }

        long a = (long) Vj;
        long b = (long) Vk;
        
        switch (opcode) {
            case BEQ:
                branchTaken = (a == b);
                nextPC = branchTaken ? targetAddress : (currentPC + 1);
                break;
                
            case BNE:
                branchTaken = (a != b);
                nextPC = branchTaken ? targetAddress : (currentPC + 1);
                break;
                
            default:
                branchTaken = false;
                nextPC = currentPC + 1;
        }
        
        state = State.RESOLVED;
        return branchTaken;
    }

    /**
     * Free this branch handler for reuse
     */
    public void free() {
        busy = false;
        state = State.FREE;
        instruction = null;
        opcode = null;
        Qj = Tag.NONE;
        Qk = Tag.NONE;
        branchTaken = false;
    }

    public String debugString() {
        if (!busy) {
            return String.format("BranchHandler %s [FREE]", name);
        }
        return String.format("BranchHandler %s [state=%s, op=%s, Vj=%.0f, Vk=%.0f, Qj=%s, Qk=%s, target=%d, taken=%s]",
                name, state, opcode, Vj, Vk, Qj, Qk, targetAddress, branchTaken);
    }
}
