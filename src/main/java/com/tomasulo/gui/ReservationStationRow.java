package com.tomasulo.gui;

/**
 * Row model for Reservation Station tables.
 */
public class ReservationStationRow {
    private String name;
    private String busy;
    private String op;
    private String vj;
    private String vk;
    private String qj;
    private String qk;
    private String state;

    public ReservationStationRow(String name, boolean busy, String op, String vj, String vk, String qj, String qk, String state) {
        this.name = name;
        this.busy = busy ? "Yes" : "No";
        this.op = op;
        this.vj = vj;
        this.vk = vk;
        this.qj = qj;
        this.qk = qk;
        this.state = state;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBusy() { return busy; }
    public void setBusy(String busy) { this.busy = busy; }

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public String getVj() { return vj; }
    public void setVj(String vj) { this.vj = vj; }

    public String getVk() { return vk; }
    public void setVk(String vk) { this.vk = vk; }

    public String getQj() { return qj; }
    public void setQj(String qj) { this.qj = qj; }

    public String getQk() { return qk; }
    public void setQk(String qk) { this.qk = qk; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
