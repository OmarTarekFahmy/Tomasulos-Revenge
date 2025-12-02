package com.tomasulo.core;

import java.util.HashMap;
import java.util.Map;

public class MainMemory implements IMemory {

    // super simple memory to be enhanced later
    private final Map<Long, Double> mem = new HashMap<>();
    private final Map<Long, Integer> wordMem = new HashMap<>();

    @Override
    public double loadDouble(long address) {
        return mem.getOrDefault(address, 0.0);
    }

    @Override
    public void storeDouble(long address, double value) {
        mem.put(address, value);
    }
    
    @Override
    public int loadWord(long address) {
        return wordMem.getOrDefault(address, 0);
    }
    
    @Override
    public void storeWord(long address, int value) {
        wordMem.put(address, value);
    }
}
