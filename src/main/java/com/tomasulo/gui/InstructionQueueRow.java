package com.tomasulo.gui;

/**
 * Row model for the Instruction Queue table.
 */
public class InstructionQueueRow {
    private int index;
    private String instruction;
    private String issueStatus = "Pending";
    private int issueCycle = -1;
    private int execStartCycle = -1;
    private int execEndCycle = -1;
    private int writeBackCycle = -1;

    public InstructionQueueRow(int index, String instruction) {
        this.index = index;
        this.instruction = instruction;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public String getIssueStatus() { return issueStatus; }
    public void setIssueStatus(String issueStatus) { this.issueStatus = issueStatus; }

    public int getIssueCycle() { return issueCycle; }
    public void setIssueCycle(int issueCycle) { this.issueCycle = issueCycle; }

    public int getExecStartCycle() { return execStartCycle; }
    public void setExecStartCycle(int execStartCycle) { this.execStartCycle = execStartCycle; }

    public int getExecEndCycle() { return execEndCycle; }
    public void setExecEndCycle(int execEndCycle) { this.execEndCycle = execEndCycle; }

    public int getWriteBackCycle() { return writeBackCycle; }
    public void setWriteBackCycle(int writeBackCycle) { this.writeBackCycle = writeBackCycle; }

    // String versions for display
    public String getIssueCycleString() { return issueCycle >= 0 ? String.valueOf(issueCycle) : "-"; }
    public String getExecStartCycleString() { return execStartCycle >= 0 ? String.valueOf(execStartCycle) : "-"; }
    public String getExecEndCycleString() { return execEndCycle >= 0 ? String.valueOf(execEndCycle) : "-"; }
    public String getWriteBackCycleString() { return writeBackCycle >= 0 ? String.valueOf(writeBackCycle) : "-"; }
}
