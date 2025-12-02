package com.tomasulo.gui;

import com.tomasulo.core.IMemory;
import com.tomasulo.core.MainMemory;

/**
 * Direct-mapped cache implementation for the simulator.
 * Implements IMemory to wrap main memory with caching.
 */
public class DataCache implements IMemory {

    private final MainMemory mainMemory;
    private final int blockSize;
    private final int cacheSize;
    private final int numLines;
    private final int hitLatency;
    private final int missPenalty;

    // Cache structure
    private final CacheLine[] lines;

    // Statistics
    private int hits = 0;
    private int misses = 0;

    public DataCache(MainMemory mainMemory, int blockSize, int cacheSize, int hitLatency, int missPenalty) {
        this.mainMemory = mainMemory;
        this.blockSize = blockSize;
        this.cacheSize = cacheSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;
        this.numLines = cacheSize / blockSize;
        this.lines = new CacheLine[numLines];

        for (int i = 0; i < numLines; i++) {
            lines[i] = new CacheLine(blockSize);
        }
    }

    /**
     * Get the cache line index for an address.
     */
    private int getIndex(long address) {
        return (int) ((address / blockSize) % numLines);
    }

    /**
     * Get the tag for an address.
     */
    private long getTag(long address) {
        return address / blockSize / numLines;
    }

    /**
     * Get the block offset for an address.
     */
    private int getBlockOffset(long address) {
        return (int) (address % blockSize);
    }

    /**
     * Check if an address is in the cache.
     */
    public boolean isHit(long address) {
        int index = getIndex(address);
        long tag = getTag(address);
        return lines[index].isValid() && lines[index].getTag() == tag;
    }

    /**
     * Get access latency for an address.
     */
    public int getAccessLatency(long address) {
        if (isHit(address)) {
            hits++;
            return hitLatency;
        } else {
            misses++;
            return hitLatency + missPenalty;
        }
    }

    @Override
    public double loadDouble(long address) {
        int index = getIndex(address);
        long tag = getTag(address);
        CacheLine line = lines[index];

        if (!line.isValid() || line.getTag() != tag) {
            // Cache miss - load block from memory
            loadBlockFromMemory(index, address);
        }

        return mainMemory.loadDouble(address);
    }

    @Override
    public void storeDouble(long address, double value) {
        int index = getIndex(address);
        long tag = getTag(address);
        CacheLine line = lines[index];

        // Write-through policy: always write to memory
        mainMemory.storeDouble(address, value);

        if (line.isValid() && line.getTag() == tag) {
            // Update cache line
            line.storeDouble(getBlockOffset(address), value);
        } else {
            // Write-allocate: load block then update
            loadBlockFromMemory(index, address);
            line.storeDouble(getBlockOffset(address), value);
        }
    }

    @Override
    public int loadWord(long address) {
        int index = getIndex(address);
        long tag = getTag(address);
        CacheLine line = lines[index];

        if (!line.isValid() || line.getTag() != tag) {
            loadBlockFromMemory(index, address);
        }

        return mainMemory.loadWord(address);
    }

    @Override
    public void storeWord(long address, int value) {
        int index = getIndex(address);
        long tag = getTag(address);
        CacheLine line = lines[index];

        mainMemory.storeWord(address, value);

        if (line.isValid() && line.getTag() == tag) {
            line.storeWord(getBlockOffset(address), value);
        } else {
            loadBlockFromMemory(index, address);
            line.storeWord(getBlockOffset(address), value);
        }
    }

    private void loadBlockFromMemory(int index, long address) {
        // Calculate block start address
        long blockStart = (address / blockSize) * blockSize;
        long tag = getTag(address);
        CacheLine line = lines[index];

        // Load entire block from memory
        byte[] blockData = new byte[blockSize];
        for (int i = 0; i < blockSize; i += 8) {
            if (blockStart + i < Integer.MAX_VALUE) {
                double val = mainMemory.loadDouble(blockStart + i);
                long bits = Double.doubleToLongBits(val);
                for (int j = 0; j < 8 && i + j < blockSize; j++) {
                    blockData[i + j] = (byte) ((bits >> (56 - j * 8)) & 0xFF);
                }
            }
        }

        line.load(tag, blockData);
    }

    // Cache statistics
    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public double getHitRate() {
        int total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    public void resetStats() {
        hits = 0;
        misses = 0;
    }

    public int getNumLines() { return numLines; }
    public int getBlockSize() { return blockSize; }
    public int getHitLatency() { return hitLatency; }
    public int getMissPenalty() { return missPenalty; }

    public CacheLine getLine(int index) {
        return index >= 0 && index < numLines ? lines[index] : null;
    }

    /**
     * Inner class representing a single cache line.
     */
    public static class CacheLine {
        private boolean valid = false;
        private long tag = 0;
        private byte[] data;

        public CacheLine(int blockSize) {
            this.data = new byte[blockSize];
        }

        public boolean isValid() { return valid; }
        public long getTag() { return tag; }
        public byte[] getData() { return data; }

        public void load(long tag, byte[] blockData) {
            this.tag = tag;
            this.data = blockData.clone();
            this.valid = true;
        }

        public void invalidate() {
            this.valid = false;
        }

        public void storeDouble(int offset, double value) {
            long bits = Double.doubleToLongBits(value);
            for (int i = 0; i < 8 && offset + i < data.length; i++) {
                data[offset + i] = (byte) ((bits >> (56 - i * 8)) & 0xFF);
            }
        }

        public void storeWord(int offset, int value) {
            for (int i = 0; i < 4 && offset + i < data.length; i++) {
                data[offset + i] = (byte) ((value >> (24 - i * 8)) & 0xFF);
            }
        }
    }
}
