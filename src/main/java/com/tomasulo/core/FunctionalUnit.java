package com.tomasulo.core;

import com.tomasulo.core.Instruction.Opcode;

public class FunctionalUnit {

    public enum Type {
        INT_ALU,        // integer add/sub, logical, shifts, SLT...
        INT_MULDIV,     // integer multiply/divide, MADD
        FP_ADD_SUB,     // ADD.D, SUB.D
        FP_MUL_DIV      // MUL.D, DIV.D
    }

    private final Type type;
    private boolean busy = false;
    private ReservationStation current;
    private int remainingCycles;

    // Configurable latencies
    private int addLatency = 2;
    private int subLatency = 2;
    private int mulLatency = 10;
    private int divLatency = 40;

    public FunctionalUnit(Type type) {
        this.type = type;
    }

    public void setLatencies(int add, int sub, int mul, int div) {
        this.addLatency = add;
        this.subLatency = sub;
        this.mulLatency = mul;
        this.divLatency = div;
    }

    public boolean isFree() {
        return !busy;
    }

    public Type getType() {
        return type;
    }

    /**
     * Does this FU support executing the given opcode?
     */
    public boolean supports(Opcode op) {
        return switch (type) {
            case INT_ALU    -> isIntAluOp(op);
            case INT_MULDIV -> isIntMulDivOp(op);
            case FP_ADD_SUB -> isFpAddSubOp(op);
            case FP_MUL_DIV -> isFpMulDivOp(op);
        };
    }

    // ---- classification helpers ----

    private boolean isIntAluOp(Opcode op) {
        return switch (op) {
            case DADD, DADDI, DADDIU,
                 DSUB, DSUBI, DSUBU,
                 OR, ORI,
                 XOR, XORI,
                 LUI,
                 DSLL, DSRL, DSRA, DSSLV, DSRLV, DSRAV,
                 SLT, SLTI, SLTIU, SLTU -> true;
            default -> false;
        };
    }

    private boolean isIntMulDivOp(Opcode op) {
        return switch (op) {
            case DMUL, DMULI, DDIV, DDIVU, MADD -> true;
            default -> false;
        };
    }

    private boolean isFpAddSubOp(Opcode op) {
        return switch (op) {
            case ADD_D, SUB_D, ADD_S, SUB_S -> true;
            default -> false;
        };
    }

    private boolean isFpMulDivOp(Opcode op) {
        return switch (op) {
            case MUL_D, DIV_D, MUL_S, DIV_S -> true;
            default -> false;
        };
    }

    // ---- latency table (per type + opcode) ----

    private int latencyFor(Opcode op) {
        return switch (type) {
            case INT_ALU -> addLatency; // All INT ALU ops use add latency
            case INT_MULDIV -> switch (op) {
                case DMUL, DMULI, MADD -> mulLatency;
                case DDIV, DDIVU -> divLatency;
                default -> mulLatency;
            };
            case FP_ADD_SUB -> switch (op) {
                case ADD_D, ADD_S -> addLatency;
                case SUB_D, SUB_S -> subLatency;
                default -> addLatency;
            };
            case FP_MUL_DIV -> switch (op) {
                case MUL_D, MUL_S -> mulLatency;
                case DIV_D, DIV_S -> divLatency;
                default -> mulLatency;
            };
        };
    }

    // ---- execution entry point ----

    public void start(ReservationStation rs) {
        this.busy = true;
        this.current = rs;
        this.remainingCycles = latencyFor(rs.getOpcode());
        rs.onStartExecution();
    }

    /**
     * Tick one cycle. When finished, returns a CdbMessage.
     */
    public CdbMessage tick() {
        if (!busy) return null;

        remainingCycles--;
        if (remainingCycles > 0) return null;

        // Execution done this cycle
        Opcode op = current.getOpcode();
        double vj = current.getVj();
        double vk = current.getVk();
        int imm = current.getImmediate();

        double result = switch (type) {
            case INT_ALU    -> execIntAlu(op, vj, vk, imm);
            case INT_MULDIV -> execIntMulDiv(op, vj, vk, imm);
            case FP_ADD_SUB -> execFpAddSub(op, vj, vk);
            case FP_MUL_DIV -> execFpMulDiv(op, vj, vk);
        };

        current.onExecutionFinished();
        CdbMessage msg = current.buildCdbMessage(result);

        busy = false;
        current = null;
        return msg;
    }

    // ---- integer execution ----

    private double execIntAlu(Opcode op, double vj, double vk, int imm) {
        long a = (long) vj;
        long b = (long) vk;

        return switch (op) {
            case DADD, DADDI, DADDIU -> a + b;
            case DSUB, DSUBI, DSUBU  -> a - b;
            case OR, ORI             -> a | b;
            case XOR, XORI           -> a ^ b;
            case LUI                 -> (long) imm << 16;
            case DSLL                -> a << imm;
            case DSRL                -> a >>> imm;
            case DSRA                -> a >> imm;
            case DSSLV, DSRLV, DSRAV -> {
                int shamt = (int) (b & 0x3F);
                yield switch (op) {
                    case DSSLV -> a << shamt;
                    case DSRLV -> a >>> shamt;
                    case DSRAV -> a >> shamt;
                    default    -> 0L;
                };
            }
            case SLT, SLTI, SLTIU, SLTU -> (a < b) ? 1L : 0L;
            default -> 0L;
        };
    }

    private double execIntMulDiv(Opcode op, double vj, double vk, int imm) {
        long a = (long) vj;
        long b = (long) vk;

        return switch (op) {
            case DMUL, DMULI -> a * b;
            case DDIV        -> (b != 0) ? a / b : 0;
            case DDIVU       -> (b != 0) ? Long.divideUnsigned(a, b) : 0;
            case MADD        -> a * b; // TODO: add HI/LO accumulation if needed
            default          -> 0L;
        };
    }

    // ---- FP execution ----

    private double execFpAddSub(Opcode op, double vj, double vk) {
        return switch (op) {
            case ADD_D, ADD_S -> vj + vk;
            case SUB_D, SUB_S -> vj - vk;
            default           -> 0.0;
        };
    }

    private double execFpMulDiv(Opcode op, double vj, double vk) {
        return switch (op) {
            case MUL_D, MUL_S -> vj * vk;
            case DIV_D, DIV_S -> vj / vk;
            default           -> 0.0;
        };
    }
}
