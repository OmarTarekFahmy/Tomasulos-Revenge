package core;

import java.util.HashMap;
import java.util.Map;

public class MainMemory implements IMemory {

    // super simple memory to be enhanced later
    private final Map<Long, Double> mem = new HashMap<>();

    @Override
    public double loadDouble(long address) {
        return mem.getOrDefault(address, 0.0);
    }

    @Override
    public void storeDouble(long address, double value) {
        mem.put(address, value);
    }
}
