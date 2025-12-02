package com.tomasulo.core;

public interface IMemory {
    double loadDouble(long address);
    void storeDouble(long address, double value);
    
    // Integer word operations
    int loadWord(long address);
    void storeWord(long address, int value);
}
