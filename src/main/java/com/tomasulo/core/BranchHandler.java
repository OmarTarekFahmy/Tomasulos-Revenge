package com.tomasulo.core;

/**
 * Handles branch instructions (BEQ, BNE, J, JAL).
 * Branch prediction and misprediction handling can be added here.
 */
public class BranchHandler {

    private boolean branchPending = false;
    private int targetPC = -1;
    private boolean branchTaken = false;

    /**
     * Call when a branch instruction is issued.
     */
    public void onBranchIssued() {
        branchPending = true;
    }

    /**
     * Call when a branch resolves (from CDB or FU).
     * @param taken whether the branch is taken
     * @param target the target PC if taken
     */
    public void resolveBranch(boolean taken, int target) {
        this.branchTaken = taken;
        this.targetPC = target;
        this.branchPending = false;
    }

    public boolean isBranchPending() {
        return branchPending;
    }

    public boolean wasBranchTaken() {
        return branchTaken;
    }

    public int getTargetPC() {
        return targetPC;
    }

    public void reset() {
        branchPending = false;
        targetPC = -1;
        branchTaken = false;
    }
}
