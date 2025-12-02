package com.tomasulo.gui;

/**
 * Holds all configuration parameters for the Tomasulo simulator.
 * These values are set by the user before simulation starts.
 */
public class SimulatorConfig {

    // Cache Configuration
    private int cacheHitLatency = 1;
    private int cacheMissPenalty = 10;
    private int cacheBlockSize = 64;    // bytes
    private int cacheSizeBytes = 1024;  // bytes

    // Instruction Latencies
    private int fpAddLatency = 2;
    private int fpSubLatency = 2;
    private int fpMulLatency = 10;
    private int fpDivLatency = 40;
    private int intAddLatency = 1;
    private int intSubLatency = 1;
    private int intMulLatency = 3;
    private int intDivLatency = 10;
    private int loadLatency = 2;
    private int storeLatency = 2;
    private int branchLatency = 1;

    // Reservation Station and Buffer Sizes
    private int fpAddSubStations = 3;
    private int fpMulDivStations = 2;
    private int intAluStations = 3;
    private int intMulDivStations = 2;
    private int loadBuffers = 3;
    private int storeBuffers = 3;

    // Functional Unit Counts
    private int fpAddSubUnits = 1;
    private int fpMulDivUnits = 1;
    private int intAluUnits = 1;
    private int intMulDivUnits = 1;

    // Register Configuration
    private int numIntRegisters = 32;
    private int numFpRegisters = 32;

    // --- Getters and Setters ---

    public int getCacheHitLatency() { return cacheHitLatency; }
    public void setCacheHitLatency(int cacheHitLatency) { this.cacheHitLatency = cacheHitLatency; }

    public int getCacheMissPenalty() { return cacheMissPenalty; }
    public void setCacheMissPenalty(int cacheMissPenalty) { this.cacheMissPenalty = cacheMissPenalty; }

    public int getCacheBlockSize() { return cacheBlockSize; }
    public void setCacheBlockSize(int cacheBlockSize) { this.cacheBlockSize = cacheBlockSize; }

    public int getCacheSizeBytes() { return cacheSizeBytes; }
    public void setCacheSizeBytes(int cacheSizeBytes) { this.cacheSizeBytes = cacheSizeBytes; }

    public int getFpAddLatency() { return fpAddLatency; }
    public void setFpAddLatency(int fpAddLatency) { this.fpAddLatency = fpAddLatency; }

    public int getFpSubLatency() { return fpSubLatency; }
    public void setFpSubLatency(int fpSubLatency) { this.fpSubLatency = fpSubLatency; }

    public int getFpMulLatency() { return fpMulLatency; }
    public void setFpMulLatency(int fpMulLatency) { this.fpMulLatency = fpMulLatency; }

    public int getFpDivLatency() { return fpDivLatency; }
    public void setFpDivLatency(int fpDivLatency) { this.fpDivLatency = fpDivLatency; }

    public int getIntAddLatency() { return intAddLatency; }
    public void setIntAddLatency(int intAddLatency) { this.intAddLatency = intAddLatency; }

    public int getIntSubLatency() { return intSubLatency; }
    public void setIntSubLatency(int intSubLatency) { this.intSubLatency = intSubLatency; }

    public int getIntMulLatency() { return intMulLatency; }
    public void setIntMulLatency(int intMulLatency) { this.intMulLatency = intMulLatency; }

    public int getIntDivLatency() { return intDivLatency; }
    public void setIntDivLatency(int intDivLatency) { this.intDivLatency = intDivLatency; }

    public int getLoadLatency() { return loadLatency; }
    public void setLoadLatency(int loadLatency) { this.loadLatency = loadLatency; }

    public int getStoreLatency() { return storeLatency; }
    public void setStoreLatency(int storeLatency) { this.storeLatency = storeLatency; }

    public int getBranchLatency() { return branchLatency; }
    public void setBranchLatency(int branchLatency) { this.branchLatency = branchLatency; }

    public int getFpAddSubStations() { return fpAddSubStations; }
    public void setFpAddSubStations(int fpAddSubStations) { this.fpAddSubStations = fpAddSubStations; }

    public int getFpMulDivStations() { return fpMulDivStations; }
    public void setFpMulDivStations(int fpMulDivStations) { this.fpMulDivStations = fpMulDivStations; }

    public int getIntAluStations() { return intAluStations; }
    public void setIntAluStations(int intAluStations) { this.intAluStations = intAluStations; }

    public int getIntMulDivStations() { return intMulDivStations; }
    public void setIntMulDivStations(int intMulDivStations) { this.intMulDivStations = intMulDivStations; }

    public int getLoadBuffers() { return loadBuffers; }
    public void setLoadBuffers(int loadBuffers) { this.loadBuffers = loadBuffers; }

    public int getStoreBuffers() { return storeBuffers; }
    public void setStoreBuffers(int storeBuffers) { this.storeBuffers = storeBuffers; }

    public int getFpAddSubUnits() { return fpAddSubUnits; }
    public void setFpAddSubUnits(int fpAddSubUnits) { this.fpAddSubUnits = fpAddSubUnits; }

    public int getFpMulDivUnits() { return fpMulDivUnits; }
    public void setFpMulDivUnits(int fpMulDivUnits) { this.fpMulDivUnits = fpMulDivUnits; }

    public int getIntAluUnits() { return intAluUnits; }
    public void setIntAluUnits(int intAluUnits) { this.intAluUnits = intAluUnits; }

    public int getIntMulDivUnits() { return intMulDivUnits; }
    public void setIntMulDivUnits(int intMulDivUnits) { this.intMulDivUnits = intMulDivUnits; }

    public int getNumIntRegisters() { return numIntRegisters; }
    public void setNumIntRegisters(int numIntRegisters) { this.numIntRegisters = numIntRegisters; }

    public int getNumFpRegisters() { return numFpRegisters; }
    public void setNumFpRegisters(int numFpRegisters) { this.numFpRegisters = numFpRegisters; }
}
