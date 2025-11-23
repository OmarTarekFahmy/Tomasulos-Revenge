package core;

public interface IMemory {
    double loadDouble(long address);
    void storeDouble(long address, double value);
}
