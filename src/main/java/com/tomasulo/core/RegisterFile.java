package com.tomasulo.core;

public class RegisterFile {
    private final Register[] regs;

    public RegisterFile(int numRegs) {
        this.regs = new Register[numRegs];
        for (int i = 0; i < numRegs; i++) {
            regs[i] = new Register();
        }
    }

    public Register get(int index) {
        return regs[index];
    }

    public int size() {
        return regs.length;
    }
}
