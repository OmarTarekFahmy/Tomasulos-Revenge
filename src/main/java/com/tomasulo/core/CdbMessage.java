package com.tomasulo.core;

public class CdbMessage {
    private final Tag tag;
    private final double value;
    private final int destRegIndex;

    public CdbMessage(Tag tag, double value, int destRegIndex) {
        this.tag = tag;
        this.value = value;
        this.destRegIndex = destRegIndex;
    }

    public String toString() {
        return String.format("CdbMessage(tag=%s, value=%.2f, destReg=%d)", tag, value, destRegIndex);
    }

    public Tag tag() { return tag; }
    public double value() { return value; }
    public int destRegIndex() { return destRegIndex; }
}
