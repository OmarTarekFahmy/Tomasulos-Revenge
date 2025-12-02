package com.tomasulo.parser;

import java.io.*;
import java.util.Properties;

/**
 * Loads configuration from a properties file or provides defaults.
 */
public class ConfigLoader {

    private Properties properties = new Properties();

    // Default values
    private int numIntAluUnits = 1;
    private int numIntMulDivUnits = 1;
    private int numFpAddSubUnits = 1;
    private int numFpMulDivUnits = 1;
    private int numIntRS = 3;
    private int numFpRS = 3;
    private int numLoadBuffers = 2;
    private int numStoreBuffers = 2;
    private int addLatency = 2;
    private int subLatency = 2;
    private int mulLatency = 10;
    private int divLatency = 40;
    private int loadLatency = 2;
    private int storeLatency = 2;
    private int cacheSize = 1024;
    private int blockSize = 64;

    public ConfigLoader() {
        // Use defaults
    }

    public ConfigLoader(String filePath) throws IOException {
        load(filePath);
    }

    public void load(String filePath) throws IOException {
        try (InputStream is = new FileInputStream(filePath)) {
            properties.load(is);
            parseProperties();
        }
    }

    private void parseProperties() {
        numIntAluUnits = getInt("fu.int.alu", numIntAluUnits);
        numIntMulDivUnits = getInt("fu.int.muldiv", numIntMulDivUnits);
        numFpAddSubUnits = getInt("fu.fp.addsub", numFpAddSubUnits);
        numFpMulDivUnits = getInt("fu.fp.muldiv", numFpMulDivUnits);
        numIntRS = getInt("rs.int", numIntRS);
        numFpRS = getInt("rs.fp", numFpRS);
        numLoadBuffers = getInt("buffer.load", numLoadBuffers);
        numStoreBuffers = getInt("buffer.store", numStoreBuffers);
        addLatency = getInt("latency.add", addLatency);
        subLatency = getInt("latency.sub", subLatency);
        mulLatency = getInt("latency.mul", mulLatency);
        divLatency = getInt("latency.div", divLatency);
        loadLatency = getInt("latency.load", loadLatency);
        storeLatency = getInt("latency.store", storeLatency);
        cacheSize = getInt("cache.size", cacheSize);
        blockSize = getInt("cache.blockSize", blockSize);
    }

    private int getInt(String key, int defaultValue) {
        String val = properties.getProperty(key);
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                // ignore, use default
            }
        }
        return defaultValue;
    }

    // Getters
    public int getNumIntAluUnits() { return numIntAluUnits; }
    public int getNumIntMulDivUnits() { return numIntMulDivUnits; }
    public int getNumFpAddSubUnits() { return numFpAddSubUnits; }
    public int getNumFpMulDivUnits() { return numFpMulDivUnits; }
    public int getNumIntRS() { return numIntRS; }
    public int getNumFpRS() { return numFpRS; }
    public int getNumLoadBuffers() { return numLoadBuffers; }
    public int getNumStoreBuffers() { return numStoreBuffers; }
    public int getAddLatency() { return addLatency; }
    public int getSubLatency() { return subLatency; }
    public int getMulLatency() { return mulLatency; }
    public int getDivLatency() { return divLatency; }
    public int getLoadLatency() { return loadLatency; }
    public int getStoreLatency() { return storeLatency; }
    public int getCacheSize() { return cacheSize; }
    public int getBlockSize() { return blockSize; }
}
