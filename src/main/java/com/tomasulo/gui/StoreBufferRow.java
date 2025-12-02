package com.tomasulo.gui;

/**
 * Row model for Store Buffer table.
 */
public class StoreBufferRow {
    private String name;
    private String busy;
    private String address;
    private String value;
    private String state;
    private String srcReg;

    public StoreBufferRow(String name, boolean busy, String address, String value, String state, String srcReg) {
        this.name = name;
        this.busy = busy ? "Yes" : "No";
        this.address = address;
        this.value = value;
        this.state = state;
        this.srcReg = srcReg;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBusy() { return busy; }
    public void setBusy(String busy) { this.busy = busy; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getSrcReg() { return srcReg; }
    public void setSrcReg(String srcReg) { this.srcReg = srcReg; }
}
