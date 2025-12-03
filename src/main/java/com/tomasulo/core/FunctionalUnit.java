package com.tomasulo.core;

import com.tomasulo.core.Instruction.Opcode;

public class FunctionalUnit {

    public enum Type {
        INT_ALU, // integer add/sub, logical, shifts, SLT...
        FP_ADD_SUB, // ADD.D, SUB.D
        FP_MUL_DIV // MUL.D, DIV.D
    }

    private final Type type;
    private boolean busy = false;
    private ReservationStation current;
    private int remainingCycles;

    public FunctionalUnit(Type type) {
        this.type = type;
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
            case INT_ALU -> isIntAluOp(op);
            case FP_ADD_SUB -> isFpAddSubOp(op);
            case FP_MUL_DIV -> isFpMulDivOp(op);
        };
    }

    // ---- classification helpers ----

    private boolean isIntAluOp(Opcode op) {
        return switch (op) {
            case DADDI, DSUBI -> true;
            default -> false;
        };
    }

    private boolean isFpAddSubOp(Opcode op) {
        return switch (op) {
            case ADD_D, SUB_D -> true;
            default -> false;
        };
    }

    private boolean isFpMulDivOp(Opcode op) {
        return switch (op) {
            case MUL_D, DIV_D -> true;
            default -> false;
        };
    }

    // ---- latency table (per type + opcode) ----

    private int latencyFor(Opcode op) {
        return switch (type) {
            case INT_ALU -> 1; // DADDI, DSUBI
            case FP_ADD_SUB -> 3; // ADD_D, SUB_D
            case FP_MUL_DIV -> switch (op) {
                case MUL_D -> 5;
                case DIV_D -> 12;
                default -> 5;
            };
        };
    }

    // ---- execution entry point ----

    public void start(ReservationStation rs) {
        this.busy = true;
        this.current = rs;
        this.remainingCycles = latencyFor(rs.getOpcode());
        rs.onStartExecution();
        System.out.println("[FU " + type + "] start " + rs.getTag() + " (" + rs.getOpcode() + ")");
    }

    /**
     * Tick one cycle. When finished, returns a CdbMessage.
     */
    public CdbMessage tick() {
        if (!busy)
            return null;

        remainingCycles--;
        if (remainingCycles > 0)
            return null;

        // Execution done this cycle
        Opcode op = current.getOpcode();
        double vj = current.getVj();
        double vk = current.getVk();
        int imm = current.getImmediate();

        double result = switch (type) {
            case INT_ALU -> execIntAlu(op, vj, vk, imm);
            case FP_ADD_SUB -> execFpAddSub(op, vj, vk);
            case FP_MUL_DIV -> execFpMulDiv(op, vj, vk);
        };

        current.onExecutionFinished();
        CdbMessage msg = current.buildCdbMessage(result);

        System.out.println("[FU " + type + "] finish " + current.getTag() +
                " (" + op + "), result=" + result);

        busy = false;
        current = null;
        return msg;
    }

    // ---- integer execution ----

    private double execIntAlu(Opcode op, double vj, double vk, int imm) {
        long a = (long) vj;
        long b = (long) imm; // For DADDI/DSUBI, second operand is immediate

        return switch (op) {
            case DADDI -> a + b;
            case DSUBI -> a - b;
            default -> 0L;
        };
    }

    // ---- FP execution ----

    private double execFpAddSub(Opcode op, double vj, double vk) {
        return switch (op) {
            case ADD_D -> vj + vk;
            case SUB_D -> vj - vk;
            default -> 0.0;
        };
    }

    private double execFpMulDiv(Opcode op, double vj, double vk) {
        return switch (op) {
            case MUL_D -> vj * vk;
            case DIV_D -> vj / vk;
            default -> 0.0;
        };
    }
}
