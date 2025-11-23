package core;

public class Cache implements IMemory {

    private final IMemory nextLevel;
    private final int hitLatency;
    private final int missLatency;

    public Cache(IMemory nextLevel, int hitLatency, int missLatency) {
        this.nextLevel = nextLevel;
        this.hitLatency = hitLatency;
        this.missLatency = missLatency;
    }

    @Override
    public double loadDouble(long address) {
        // TODO: implement real cache tags and timing
        // For now just forward to next level
        return nextLevel.loadDouble(address);
    }

    @Override
    public void storeDouble(long address, double value) {
        // TODO: implement write policy
        nextLevel.storeDouble(address, value);
    }

    public int getHitLatency() { return hitLatency; }
    public int getMissLatency() { return missLatency; }
}
