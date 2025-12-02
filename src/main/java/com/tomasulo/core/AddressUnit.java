package com.tomasulo.core;

import java.util.List;

public class AddressUnit {

    private boolean busy;
    private int remainingCycles;
    private Runnable onFinish; // callback to write EA into buffer

    // latency configurable
    private final int addressLatency;

    public AddressUnit(int addressLatency) {
        this.addressLatency = addressLatency;
    }

    public boolean isFree() {
        return !busy;
    }

    public void startForLoad(LoadBuffer lb, RegisterFile regFile) {
        busy = true;
        remainingCycles = addressLatency;
        int base = regFile.get(lb.getBaseRegIndex()).getIntValue();
        int offset = lb.getOffset();
        long ea = (long) base + offset;
        onFinish = () -> lb.setEffectiveAddress(ea);
    }

    public void startForStore(StoreBuffer sb, RegisterFile regFile) {
        busy = true;
        remainingCycles = addressLatency;
        int base = regFile.get(sb.getBaseRegIndex()).getIntValue();
        int offset = sb.getOffset();
        long ea = (long) base + offset;
        onFinish = () -> sb.setEffectiveAddress(ea);
    }

    public void tick() {
        if (!busy) return;
        remainingCycles--;
        if (remainingCycles <= 0) {
            busy = false;
            if (onFinish != null) {
                onFinish.run();
                onFinish = null;
            }
        }
    }

    /**
     * Check if a load can start memory access based on address clashes.
     * "This load must wait for preceding stores with same address."
     */
    public static boolean canLoadGoToMemory(LoadBuffer load,
                                            List<StoreBuffer> stores) {
        for (StoreBuffer sb : stores) {
            if (!sb.isFree()
                && sb.getSequenceNumber() < load.getSequenceNumber()
                && sb.getEffectiveAddress() == load.getEffectiveAddress()
                && sb.getState() != StoreBuffer.State.FREE) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check store address clash with earlier loads/stores.
     */
    public static boolean canStoreGoToMemory(StoreBuffer store,
                                             List<LoadBuffer> loads,
                                             List<StoreBuffer> stores) {
        for (StoreBuffer sb : stores) {
            if (!sb.isFree()
                && sb.getSequenceNumber() < store.getSequenceNumber()
                && sb.getEffectiveAddress() == store.getEffectiveAddress()
                && sb != store) {
                return false;
            }
        }
        for (LoadBuffer lb : loads) {
            if (!lb.isFree()
                && lb.getSequenceNumber() < store.getSequenceNumber()
                && lb.getEffectiveAddress() == store.getEffectiveAddress()) {
                return false;
            }
        }
        return true;
    }
}
