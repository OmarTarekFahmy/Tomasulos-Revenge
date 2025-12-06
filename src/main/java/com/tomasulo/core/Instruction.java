package com.tomasulo.core;

public class Instruction {

    public enum Opcode {
        // LOADS/Stores
        LD, LW, L_S, L_D,
        SD, SW, S_S, S_D,

        // Arithmetic (integer)
        DADDI, DSUBI,

        // Branches (conditional)
        BEQ, BNE,

        // FP ADD/SUB (double-precision)
        ADD_D, SUB_D,

        // FP ADD/SUB (single-precision)
        ADD_S, SUB_S,

        // FP MUL/DIV (double-precision)
        MUL_D, DIV_D,

        // FP MUL/DIV (single-precision)
        MUL_S, DIV_S,
    }

    private final Opcode opcode;
    private final int destReg; // -1 if none
    private final int src1Reg; // -1 if none
    private final int src2Reg; // -1 if none
    private final int baseReg; // for loads/stores
    private final int offset; // mem offset
    private final int immediate; // for immediates (DADDI, ORI, etc.)

    public Instruction(Opcode opcode,
            int destReg,
            int src1Reg,
            int src2Reg,
            int baseReg,
            int offset,
            int immediate) {
        this.opcode = opcode;
        this.destReg = destReg;
        this.src1Reg = src1Reg;
        this.src2Reg = src2Reg;
        this.baseReg = baseReg;
        this.offset = offset;
        this.immediate = immediate;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public int getDestReg() {
        return destReg;
    }

    public int getSrc1Reg() {
        return src1Reg;
    }

    public int getSrc2Reg() {
        return src2Reg;
    }

    public int getBaseReg() {
        return baseReg;
    }

    public int getOffset() {
        return offset;
    }

    public int getImmediate() {
        return immediate;
    }

    // --- classification helpers ---

    public boolean isLoad() {
        return switch (opcode) {
            case LD, LW, L_S, L_D -> true;
            default -> false;
        };
    }

    public boolean isStore() {
        return switch (opcode) {
            case SD, SW, S_S, S_D -> true;
            default -> false;
        };
    }

    public boolean isBranch() {
        return switch (opcode) {
            case BEQ, BNE -> true;
            default -> false;
        };
    }

    public boolean isFpAddSub() {
        return switch (opcode) {
            case ADD_D, SUB_D, ADD_S, SUB_S -> true;
            default -> false;
        };
    }

    public boolean isFpMulDiv() {
        return switch (opcode) {
            case MUL_D, DIV_D, MUL_S, DIV_S -> true;
            default -> false;
        };
    }

    public boolean isIntArithmetic() {
        return switch (opcode) {
            case DADDI, DSUBI -> true;
            default -> false;
        };
    }

    // For RS: does this instruction conceptually use a second operand?
    public boolean usesSecondSource() {
        // Everything binary + immediates, excluding pure jumps
        return switch (opcode) {
            case
                    // FP binary (double-precision)
                    ADD_D, SUB_D, MUL_D, DIV_D,
                    // FP binary (single-precision)
                    ADD_S, SUB_S, MUL_S, DIV_S,
                    // branches (comparing two registers)
                    BEQ, BNE ->
                true;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return opcode.toString();
    }
}
