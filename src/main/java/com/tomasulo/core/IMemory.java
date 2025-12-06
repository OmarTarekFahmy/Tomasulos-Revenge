package com.tomasulo.core;

public interface IMemory {
    byte loadByte(int address);
    int loadWord(int address);
    long loadLong(int address);
    float loadFloat(int address);
    double loadDouble(int address);

    boolean storeByte(int address, byte value);
    boolean storeWord(int address, int value);
    boolean storeLong(int address, long value);
    boolean storeFloat(int address, float value);
    boolean storeDouble(int address, double value);
}
