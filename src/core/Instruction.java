package core;

public class Instruction {

    public enum Opcode {
        // LOADS/Stores
        LD, LW, LB, LBU, LH,
        L_S, L_D,
        SD, SW, SB, SH, S_S, S_B,

        // Arithmetic (integer)
        DADD, DADDI, DADDIU,
        DSUB, DSUBU,
        DMUL, DMULI, DDIV, DDIVU, MADD,

        // Logical
        OR, ORI,
        XOR, XORI,
        LUI,
        DSLL, DSRL, DSRA, DSSLV, DSRLV, DSRAV,
        SLT, SLTI, SLTIU, SLTU,

        // Branches/Jumps
        BEQZ, BNEZ, BEQ, BNE, MOVN, MOVZ,
        J, JR, JAL, JALR,

        // FP operations
        ADD_D, SUB_D, MUL_D, DIV_D,
    }

    private final Opcode opcode;
    private final int destReg;   // -1 if none
    private final int src1Reg;   // -1 if none
    private final int src2Reg;   // -1 if none
    private final int baseReg;   // for loads/stores
    private final int offset;    // mem offset
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

    public Opcode getOpcode()    { return opcode; }
    public int getDestReg()      { return destReg; }
    public int getSrc1Reg()      { return src1Reg; }
    public int getSrc2Reg()      { return src2Reg; }
    public int getBaseReg()      { return baseReg; }
    public int getOffset()       { return offset; }
    public int getImmediate()    { return immediate; }

    // --- classification helpers ---

    public boolean isLoad() {
        return switch (opcode) {
            case LD, LW, LB, LBU, LH, L_S, L_D -> true;
            default -> false;
        };
    }

    public boolean isStore() {
        return switch (opcode) {
            case SD, SW, SB, SH, S_S, S_B -> true;
            default -> false;
        };
    }

    public boolean isBranchOrJump() {
        return switch (opcode) {
            case BEQZ, BNEZ, BEQ, BNE, MOVN, MOVZ,
                 J, JR, JAL, JALR -> true;
            default -> false;
        };
    }

    public boolean isFp() {
        return switch (opcode) {
            case ADD_D, SUB_D, MUL_D, DIV_D -> true;
            default -> false;
        };
    }

    public boolean isIntArithOrLogic() {
        return !isLoad() && !isStore() && !isBranchOrJump() && !isFp();
    }

    // Instructions where the 2nd operand is an immediate, not a reg
    public boolean isImmediateType() {
        return switch (opcode) {
            case DADDI, DADDIU,
                 DMULI,
                 ORI, XORI,
                 LUI,
                 SLTI, SLTIU -> true;
            default -> false;
        };
    }

    // For RS: does this instruction conceptually use a second operand?
    public boolean usesSecondSource() {
        // Everything binary + immediates, excluding pure jumps
        return switch (opcode) {
            // integer arithmetic
            case DADD, DADDI, DADDIU,
                 DSUB, DSUBU,
                 DMUL, DMULI, DDIV, DDIVU, MADD,
            // logical / shifts / set
                 OR, ORI, XOR, XORI,
                 DSLL, DSRL, DSRA, DSSLV, DSRLV, DSRAV,
                 SLT, SLTI, SLTIU, SLTU,
            // FP binary
                 ADD_D, SUB_D, MUL_D, DIV_D -> true;
            default -> false;
        };
    }
}
