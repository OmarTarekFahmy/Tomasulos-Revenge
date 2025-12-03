package core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Main Memory implementation using a byte-addressable array.
 * Supports word (32-bit), doubleword (64-bit), single-precision float, 
 * and double-precision float load/store operations.
 * 
 * Memory is byte-addressable with big-endian byte ordering (MIPS convention).
 */
public class MainMemory implements IMemory {

    private final byte[] memory;
    private final int size;

    /**
     * Creates a new MainMemory with the specified size in bytes.
     * @param sizeInBytes The size of memory in bytes
     */
    public MainMemory(int sizeInBytes) {
        this.size = sizeInBytes;
        this.memory = new byte[sizeInBytes];
    }

    /**
     * Checks if an address is valid and within bounds.
     * @param address The address to check
     * @param numBytes Number of bytes being accessed
     * @return true if the address range is valid
     */
    private boolean isValidAddress(int address, int numBytes) {
        return address >= 0 && (address + numBytes) <= size;
    }

    /**
     * Load a 32-bit word (4 bytes) from memory.
     * Used by: LW instruction
     * @param address The byte address (should be 4-byte aligned for best performance)
     * @return The 32-bit integer value at the address
     */
    @Override
    public int loadWord(int address) {
        if (!isValidAddress(address, 4)) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        ByteBuffer buffer = ByteBuffer.wrap(memory, address, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }

    /**
     * Load a 64-bit doubleword (8 bytes) from memory.
     * Used by: LD instruction
     * @param address The byte address (should be 8-byte aligned for best performance)
     * @return The 64-bit long value at the address
     */
    @Override
    public long loadLong(int address) {
        if (!isValidAddress(address, 8)) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        ByteBuffer buffer = ByteBuffer.wrap(memory, address, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    /**
     * Load a single-precision float (4 bytes) from memory.
     * Used by: L.S instruction
     * @param address The byte address (should be 4-byte aligned for best performance)
     * @return The float value at the address
     */
    @Override
    public float loadFloat(int address) {
        if (!isValidAddress(address, 4)) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        ByteBuffer buffer = ByteBuffer.wrap(memory, address, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getFloat();
    }

    /**
     * Load a double-precision float (8 bytes) from memory.
     * Used by: L.D instruction
     * @param address The byte address (should be 8-byte aligned for best performance)
     * @return The double value at the address
     */
    @Override
    public double loadDouble(int address) {
        if (!isValidAddress(address, 8)) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        ByteBuffer buffer = ByteBuffer.wrap(memory, address, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getDouble();
    }

    /**
     * Store a 32-bit word (4 bytes) to memory.
     * Used by: SW instruction
     * @param address The byte address (should be 4-byte aligned for best performance)
     * @param value The 32-bit integer value to store
     * @return true if the store was successful
     */
    @Override
    public boolean storeWord(int address, int value) {
        if (!isValidAddress(address, 4)) {
            return false;
        }
        ByteBuffer buffer = ByteBuffer.wrap(memory, address, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(value);
        return true;
    }

    /**
     * Store a 64-bit doubleword (8 bytes) to memory.
     * Used by: SD instruction
     * @param address The byte address (should be 8-byte aligned for best performance)
     * @param value The 64-bit long value to store
     * @return true if the store was successful
     */
    @Override
    public boolean storeLong(int address, long value) {
        if (!isValidAddress(address, 8)) {
            return false;
        }
        ByteBuffer buffer = ByteBuffer.wrap(memory, address, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(value);
        return true;
    }

    /**
     * Store a single-precision float (4 bytes) to memory.
     * Used by: S.S instruction
     * @param address The byte address (should be 4-byte aligned for best performance)
     * @param value The float value to store
     * @return true if the store was successful
     */
    @Override
    public boolean storeFloat(int address, float value) {
        if (!isValidAddress(address, 4)) {
            return false;
        }
        ByteBuffer buffer = ByteBuffer.wrap(memory, address, 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putFloat(value);
        return true;
    }

    /**
     * Store a double-precision float (8 bytes) to memory.
     * Used by: S.D instruction
     * @param address The byte address (should be 8-byte aligned for best performance)
     * @param value The double value to store
     * @return true if the store was successful
     */
    @Override
    public boolean storeDouble(int address, double value) {
        if (!isValidAddress(address, 8)) {
            return false;
        }
        ByteBuffer buffer = ByteBuffer.wrap(memory, address, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putDouble(value);
        return true;
    }

    /**
     * Read raw bytes from memory (used by cache for block transfers).
     * @param address Starting byte address
     * @param numBytes Number of bytes to read
     * @return Byte array containing the data
     */
    public byte[] readBytes(int address, int numBytes) {
        if (!isValidAddress(address, numBytes)) {
            throw new IllegalArgumentException("Invalid memory address range: " + address + " to " + (address + numBytes));
        }
        byte[] data = new byte[numBytes];
        System.arraycopy(memory, address, data, 0, numBytes);
        return data;
    }

    /**
     * Write raw bytes to memory (used by cache for block transfers/write-back).
     * @param address Starting byte address
     * @param data Byte array to write
     */
    public void writeBytes(int address, byte[] data) {
        if (!isValidAddress(address, data.length)) {
            throw new IllegalArgumentException("Invalid memory address range: " + address + " to " + (address + data.length));
        }
        System.arraycopy(data, 0, memory, address, data.length);
    }

    /**
     * Get the total size of memory in bytes.
     * @return Memory size in bytes
     */
    public int getSize() {
        return size;
    }

    /**
     * Debug method to dump a range of memory.
     * @param startAddress Starting address
     * @param numBytes Number of bytes to display
     */
    public void dump(int startAddress, int numBytes) {
        System.out.println("Memory dump from address " + startAddress + ":");
        for (int i = 0; i < numBytes; i++) {
            if (i % 16 == 0) {
                System.out.printf("%n%08X: ", startAddress + i);
            }
            System.out.printf("%02X ", memory[startAddress + i] & 0xFF);
        }
        System.out.println();
    }
}
