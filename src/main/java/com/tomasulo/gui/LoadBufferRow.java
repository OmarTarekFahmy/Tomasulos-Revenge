package com.tomasulo.gui;

/**
 * Row model for Load Buffer table.
 */
public class LoadBufferRow {
    private String name;
    private String busy;
    private String address;
    private String state;
    private String destReg;

    public LoadBufferRow(String name, boolean busy, String address, String state, String destReg) {
        this.name = name;
        this.busy = busy ? "Yes" : "No";
        this.address = address;
        this.state = state;
        this.destReg = destReg;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBusy() { return busy; }
    public void setBusy(String busy) { this.busy = busy; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getDestReg() { return destReg; }
    public void setDestReg(String destReg) { this.destReg = destReg; }
}
