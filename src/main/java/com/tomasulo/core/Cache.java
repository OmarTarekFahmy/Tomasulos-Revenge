package com.tomasulo.core;

/**
 * Direct-Mapped Cache implementation.
 * 
 * The cache sits between the CPU (Load/Store buffers) and Main Memory.
 * It implements the IMemory interface, so it can be used transparently.
 * 
 * Address breakdown for direct-mapped cache:
 * | TAG | INDEX | BLOCK OFFSET |
 * 
 * - Block Offset: log2(blockSize) bits - selects byte within block
 * - Index: log2(numBlocks) bits - selects which cache line
 * - Tag: remaining upper bits - used for matching
 * 
 * Write Policy: Write-back with write-allocate
 * - On write hit: update cache, mark dirty
 * - On write miss: fetch block, then write (write-allocate)
 * - On eviction of dirty block: write back to memory
 */
public class Cache implements IMemory {

    private final CacheBlock[] blocks;       // Array of cache blocks
    private final MainMemory mainMemory;     // Reference to main memory (next level)
    
    private final int cacheSize;             // Total cache size in bytes
    private final int blockSize;             // Size of each block in bytes
    private final int numBlocks;             // Number of blocks in cache
    
    private final int blockOffsetBits;       // Number of bits for block offset
    private final int indexBits;             // Number of bits for index
    
    private final int hitLatency;            // Cycles for cache hit
    private final int missLatency;           // Additional cycles for cache miss (memory access)
    
    // Statistics
    private int hits = 0;
    private int misses = 0;
    private int writebacks = 0;

    /**
     * Creates a new direct-mapped cache.
     * 
     * @param cacheSize   Total cache size in bytes (must be power of 2)
     * @param blockSize   Size of each block in bytes (must be power of 2)
     * @param hitLatency  Number of cycles for a cache hit
     * @param missLatency Additional cycles for a cache miss
     * @param mainMemory  Reference to main memory
     */
    public Cache(int cacheSize, int blockSize, int hitLatency, int missLatency, MainMemory mainMemory) {
        // Validate parameters
        if (!isPowerOfTwo(cacheSize) || !isPowerOfTwo(blockSize)) {
            throw new IllegalArgumentException("Cache size and block size must be powers of 2");
        }
        if (blockSize > cacheSize) {
            throw new IllegalArgumentException("Block size cannot be larger than cache size");
        }
        
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.numBlocks = cacheSize / blockSize;
        this.hitLatency = hitLatency;
        this.missLatency = missLatency;
        this.mainMemory = mainMemory;
        
        // Calculate bit widths
        this.blockOffsetBits = log2(blockSize);
        this.indexBits = log2(numBlocks);
        
        // Initialize cache blocks
        this.blocks = new CacheBlock[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            blocks[i] = new CacheBlock(blockSize);
        }
    }

    // ==================== Address Decomposition ====================

    /**
     * Extract the block offset from an address.
     * @param address The memory address
     * @return The block offset (byte position within block)
     */
    private int getBlockOffset(int address) {
        return address & ((1 << blockOffsetBits) - 1);
    }

    /**
     * Extract the cache index from an address.
     * @param address The memory address
     * @return The cache index (which block in the cache)
     */
    private int getIndex(int address) {
        return (address >> blockOffsetBits) & ((1 << indexBits) - 1);
    }

    /**
     * Extract the tag from an address.
     * @param address The memory address
     * @return The tag (for matching)
     */
    private int getTag(int address) {
        return address >> (blockOffsetBits + indexBits);
    }

    /**
     * Reconstruct the memory address of a block given its index and tag.
     * @param tag The tag of the block
     * @param index The index of the block
     * @return The starting memory address of the block
     */
    private int getBlockAddress(int tag, int index) {
        return (tag << (blockOffsetBits + indexBits)) | (index << blockOffsetBits);
    }

    // ==================== Cache Operations ====================

    /**
     * Check if an address hits in the cache.
     * @param address The memory address
     * @return true if cache hit, false if miss
     */
    public boolean isHit(int address) {
        int index = getIndex(address);
        int tag = getTag(address);
        CacheBlock block = blocks[index];
        return block.isValid() && block.getTag() == tag;
    }

    /**
     * Handle a cache miss by fetching the block from memory.
     * If the current block is dirty, write it back first.
     * @param address The memory address that caused the miss
     */
    private void handleMiss(int address) {
        int index = getIndex(address);
        int tag = getTag(address);
        CacheBlock block = blocks[index];
        
        // Write back if dirty
        if (block.isValid() && block.isDirty()) {
            writeBackBlock(index);
        }
        
        // Fetch new block from memory
        int blockStartAddress = address & ~((1 << blockOffsetBits) - 1); // Align to block boundary
        byte[] data = mainMemory.readBytes(blockStartAddress, blockSize);
        block.loadFromMemory(tag, data);
        
        misses++;
    }

    /**
     * Write back a dirty block to main memory.
     * @param index The index of the block to write back
     */
    private void writeBackBlock(int index) {
        CacheBlock block = blocks[index];
        if (block.isValid() && block.isDirty()) {
            int blockAddress = getBlockAddress(block.getTag(), index);
            mainMemory.writeBytes(blockAddress, block.getAllData());
            block.setDirty(false);
            writebacks++;
        }
    }

    /**
     * Ensure a block is in the cache (fetch if necessary).
     * @param address The memory address
     */
    private void ensureBlockPresent(int address) {
        if (!isHit(address)) {
            handleMiss(address);
        } else {
            hits++;
        }
    }

    // ==================== IMemory Interface Implementation ====================

    /**
     * Load a 32-bit word from cache.
     * Used by: LW instruction
     */
    @Override
    public int loadWord(int address) {
        ensureBlockPresent(address);
        int index = getIndex(address);
        int offset = getBlockOffset(address);
        return blocks[index].readWord(offset);
    }

    /**
     * Load a 64-bit doubleword from cache.
     * Used by: LD instruction
     */
    @Override
    public long loadLong(int address) {
        ensureBlockPresent(address);
        int index = getIndex(address);
        int offset = getBlockOffset(address);
        return blocks[index].readLong(offset);
    }

    /**
     * Load a single-precision float from cache.
     * Used by: L.S instruction
     */
    @Override
    public float loadFloat(int address) {
        ensureBlockPresent(address);
        int index = getIndex(address);
        int offset = getBlockOffset(address);
        return blocks[index].readFloat(offset);
    }

    /**
     * Load a double-precision float from cache.
     * Used by: L.D instruction
     */
    @Override
    public double loadDouble(int address) {
        ensureBlockPresent(address);
        int index = getIndex(address);
        int offset = getBlockOffset(address);
        return blocks[index].readDouble(offset);
    }

    /**
     * Store a 32-bit word to cache.
     * Used by: SW instruction
     */
    @Override
    public boolean storeWord(int address, int value) {
        ensureBlockPresent(address);  // Write-allocate: fetch block on miss
        int index = getIndex(address);
        int offset = getBlockOffset(address);
        blocks[index].writeWord(offset, value);
        return true;
    }

    /**
     * Store a 64-bit doubleword to cache.
     * Used by: SD instruction
     */
    @Override
    public boolean storeLong(int address, long value) {
        ensureBlockPresent(address);
        int index = getIndex(address);
        int offset = getBlockOffset(address);
        blocks[index].writeLong(offset, value);
        return true;
    }

    /**
     * Store a single-precision float to cache.
     * Used by: S.S instruction
     */
    @Override
    public boolean storeFloat(int address, float value) {
        ensureBlockPresent(address);
        int index = getIndex(address);
        int offset = getBlockOffset(address);
        blocks[index].writeFloat(offset, value);
        return true;
    }

    /**
     * Store a double-precision float to cache.
     * Used by: S.D instruction
     */
    @Override
    public boolean storeDouble(int address, double value) {
        ensureBlockPresent(address);
        int index = getIndex(address);
        int offset = getBlockOffset(address);
        blocks[index].writeDouble(offset, value);
        return true;
    }

    // ==================== Latency Methods ====================

    /**
     * Get the access latency for a given address.
     * @param address The memory address
     * @return Number of cycles for this access
     */
    public int getAccessLatency(int address) {
        if (isHit(address)) {
            return hitLatency;
        } else {
            return hitLatency + missLatency;
        }
    }

    public int getHitLatency() {
        return hitLatency;
    }

    public int getMissLatency() {
        return missLatency;
    }

    // ==================== Statistics ====================

    public int getHits() {
        return hits;
    }

    public int getMisses() {
        return misses;
    }

    public int getWritebacks() {
        return writebacks;
    }

    public double getHitRate() {
        int total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    public void resetStatistics() {
        hits = 0;
        misses = 0;
        writebacks = 0;
    }

    // ==================== Cache Management ====================

    /**
     * Flush all dirty blocks to main memory.
     * Call this at end of simulation to ensure memory consistency.
     */
    public void flush() {
        for (int i = 0; i < numBlocks; i++) {
            writeBackBlock(i);
        }
    }

    /**
     * Invalidate all cache blocks.
     */
    public void invalidateAll() {
        for (CacheBlock block : blocks) {
            block.invalidate();
        }
    }

    // ==================== Configuration Getters ====================

    public int getCacheSize() {
        return cacheSize;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getNumBlocks() {
        return numBlocks;
    }

    // ==================== Utility Methods ====================

    private static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    private static int log2(int n) {
        return (int) (Math.log(n) / Math.log(2));
    }

    // ==================== Debug Output ====================

    /**
     * Print cache configuration and statistics.
     */
    public void printStats() {
        System.out.println("=== Cache Statistics ===");
        System.out.println("Configuration:");
        System.out.printf("  Cache Size: %d bytes%n", cacheSize);
        System.out.printf("  Block Size: %d bytes%n", blockSize);
        System.out.printf("  Number of Blocks: %d%n", numBlocks);
        System.out.printf("  Hit Latency: %d cycles%n", hitLatency);
        System.out.printf("  Miss Penalty: %d cycles%n", missLatency);
        System.out.println("Statistics:");
        System.out.printf("  Hits: %d%n", hits);
        System.out.printf("  Misses: %d%n", misses);
        System.out.printf("  Hit Rate: %.2f%%%n", getHitRate() * 100);
        System.out.printf("  Writebacks: %d%n", writebacks);
    }

    /**
     * Print the contents of all valid cache blocks.
     */
    public void printContents() {
        System.out.println("=== Cache Contents ===");
        for (int i = 0; i < numBlocks; i++) {
            CacheBlock block = blocks[i];
            if (block.isValid()) {
                int blockAddr = getBlockAddress(block.getTag(), i);
                System.out.printf("Block[%d]: tag=%d, addr=0x%X, dirty=%b%n",
                        i, block.getTag(), blockAddr, block.isDirty());
            }
        }
    }
}
