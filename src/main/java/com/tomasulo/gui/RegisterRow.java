package com.tomasulo.gui;

/**
 * Row model for Register tables.
 */
public class RegisterRow {
    private String name;
    private String value;
    private String qi;

    public RegisterRow(String name, String value, String qi) {
        this.name = name;
        this.value = value;
        this.qi = qi;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getQi() { return qi; }
    public void setQi(String qi) { this.qi = qi; }
}
