package com.tomasulo.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents a single cache block/line.
 * Each block contains:
 * - Valid bit: indicates if the block contains valid data
 * - Dirty bit: indicates if the block has been modified (for write-back policy)
 * - Tag: used for address matching
 * - Data: the actual cached bytes
 */
public class CacheBlock {

    private final byte[] data; // Raw byte storage for the block
    private final int blockSize; // Size in bytes
    private boolean valid; // Valid bit
    private boolean dirty; // Dirty bit (for write-back policy)
    private int tag; // Tag bits from the address

    /**
     * Creates a new cache block with the specified size.
     * 
     * @param blockSizeInBytes Size of the block in bytes
     */
    public CacheBlock(int blockSizeInBytes) {
        this.blockSize = blockSizeInBytes;
        this.data = new byte[blockSizeInBytes];
        this.valid = false;
        this.dirty = false;
        this.tag = -1;
    }

    // ==================== Basic Getters/Setters ====================

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public String getDataHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Get data as decimal double values.
     * Interprets each 8 bytes as a double-precision float.
     * 
     * @return String representation of doubles separated by commas
     */
    public String getDataAsDoubles() {
        StringBuilder sb = new StringBuilder();
        int numDoubles = blockSize / 8;
        for (int i = 0; i < numDoubles; i++) {
            try {
                double value = readDouble(i * 8);
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(String.format("%.2f", value));
            } catch (Exception e) {
                // If can't read as double, skip
            }
        }
        return sb.toString();
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getBlockSize() {
        return blockSize;
    }

    // ==================== Raw Byte Access ====================

    /**
     * Get all data bytes in this block (for write-back to memory).
     * 
     * @return Copy of the block's data
     */
    public byte[] getAllData() {
        byte[] copy = new byte[blockSize];
        System.arraycopy(data, 0, copy, 0, blockSize);
        return copy;
    }

    /**
     * Set all data bytes in this block (for loading from memory).
     * 
     * @param newData Data to copy into the block
     */
    public void setAllData(byte[] newData) {
        if (newData.length != blockSize) {
            throw new IllegalArgumentException("Data size mismatch: expected " + blockSize + ", got " + newData.length);
        }
        System.arraycopy(newData, 0, data, 0, blockSize);
    }

    /**
     * Read a single byte at the given offset within the block.
     * 
     * @param offset Byte offset within the block
     * @return The byte value
     */
    public byte readByte(int offset) {
        if (offset < 0 || offset >= blockSize) {
            throw new IllegalArgumentException("Invalid block offset: " + offset);
        }
        return data[offset];
    }

    /**
     * Write a single byte at the given offset within the block.
     * 
     * @param offset Byte offset within the block
     * @param value  The byte value to write
     */
    public void writeByte(int offset, byte value) {
        if (offset < 0 || offset >= blockSize) {
            throw new IllegalArgumentException("Invalid block offset: " + offset);
        }
        data[offset] = value;
        dirty = true;
    }

    // ==================== Word (32-bit) Access ====================

    /**
     * Read a 32-bit word at the given offset.
     * Used by: LW instruction
     * 
     * @param offset Byte offset within the block (should be 4-byte aligned)
     * @return The 32-bit integer value
     */
    public int readWord(int offset) {
        if (offset < 0 || offset + 4 > blockSize) {
            throw new IllegalArgumentException("Invalid word offset: " + offset);
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }

    /**
     * Write a 32-bit word at the given offset.
     * Used by: SW instruction
     * 
     * @param offset Byte offset within the block (should be 4-byte aligned)
     * @param value  The 32-bit integer value to write
     */
    public void writeWord(int offset, int value) {
        if (offset < 0 || offset + 4 > blockSize) {
            throw new IllegalArgumentException("Invalid word offset: " + offset);
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(value);
        dirty = true;
    }

    // ==================== Long/Doubleword (64-bit) Access ====================

    /**
     * Read a 64-bit doubleword at the given offset.
     * Used by: LD instruction
     * 
     * @param offset Byte offset within the block (should be 8-byte aligned)
     * @return The 64-bit long value
     */
    public long readLong(int offset) {
        if (offset < 0 || offset + 8 > blockSize) {
            throw new IllegalArgumentException("Invalid long offset: " + offset);
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    /**
     * Write a 64-bit doubleword at the given offset.
     * Used by: SD instruction
     * 
     * @param offset Byte offset within the block (should be 8-byte aligned)
     * @param value  The 64-bit long value to write
     */
    public void writeLong(int offset, long value) {
        if (offset < 0 || offset + 8 > blockSize) {
            throw new IllegalArgumentException("Invalid long offset: " + offset);
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(value);
        dirty = true;
    }

    // ==================== Float (32-bit) Access ====================

    /**
     * Read a single-precision float at the given offset.
     * Used by: L.S instruction
     * 
     * @param offset Byte offset within the block (should be 4-byte aligned)
     * @return The float value
     */
    public float readFloat(int offset) {
        if (offset < 0 || offset + 4 > blockSize) {
            throw new IllegalArgumentException("Invalid float offset: " + offset);
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getFloat();
    }

    /**
     * Write a single-precision float at the given offset.
     * Used by: S.S instruction
     * 
     * @param offset Byte offset within the block (should be 4-byte aligned)
     * @param value  The float value to write
     */
    public void writeFloat(int offset, float value) {
        if (offset < 0 || offset + 4 > blockSize) {
            throw new IllegalArgumentException("Invalid float offset: " + offset);
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putFloat(value);
        dirty = true;
    }

    // ==================== Double (64-bit) Access ====================

    /**
     * Read a double-precision float at the given offset.
     * Used by: L.D instruction
     * 
     * @param offset Byte offset within the block (should be 8-byte aligned)
     * @return The double value
     */
    public double readDouble(int offset) {
        if (offset < 0 || offset + 8 > blockSize) {
            throw new IllegalArgumentException("Invalid double offset: " + offset);
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getDouble();
    }

    /**
     * Write a double-precision float at the given offset.
     * Used by: S.D instruction
     * 
     * @param offset Byte offset within the block (should be 8-byte aligned)
     * @param value  The double value to write
     */
    public void writeDouble(int offset, double value) {
        if (offset < 0 || offset + 8 > blockSize) {
            throw new IllegalArgumentException("Invalid double offset: " + offset);
        }
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putDouble(value);
        dirty = true;
    }

    // ==================== Block Operations ====================

    /**
     * Invalidate this cache block.
     * Clears valid and dirty bits, resets tag.
     */
    public void invalidate() {
        valid = false;
        dirty = false;
        tag = -1;
    }

    /**
     * Load this block with data from memory.
     * 
     * @param newTag    The tag for this block
     * @param blockData The data bytes from memory
     */
    public void loadFromMemory(int newTag, byte[] blockData) {
        this.tag = newTag;
        setAllData(blockData);
        this.valid = true;
        this.dirty = false;
    }

    @Override
    public String toString() {
        return String.format("CacheBlock[valid=%b, dirty=%b, tag=%d, size=%d bytes]",
                valid, dirty, tag, blockSize);
    }
}
