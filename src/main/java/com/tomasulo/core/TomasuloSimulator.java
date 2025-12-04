package com.tomasulo.core;

import java.util.ArrayList;
import java.util.List;

import com.tomasulo.core.Instruction.Opcode;

/**
 * Tomasulo Algorithm Simulator
 * - Configurable number of FP and INT functional units
 * - Configurable reservation stations, load/store buffers
 * - Cache with configurable hit/miss latencies
 * - User-defined latencies for each functional unit type
 */
public class TomasuloSimulator {

    // --- config ---
    private static final int NUM_REGS = 64; // 0..31 -> "R"; 32..63 -> "F"
    private static final int FP_BASE = 32; // F0 == reg[32], F6 == reg[38], etc.

    // Configurable parameters (set via constructor)
    private final int numIntAluUnits;
    private final int numFpAddSubUnits;
    private final int numFpMulDivUnits;
    private final int numFpAddSubRs;   // Separate RS for FP ADD/SUB
    private final int numFpMulDivRs;   // Separate RS for FP MUL/DIV
    private final int numIntRs;
    private final int numLoadBuffers;
    private final int numStoreBuffers;

    // Latencies for functional units (set by user)
    private final int intAluLatency;
    private final int fpAddSubLatency;
    private final int fpMulLatency;
    private final int fpDivLatency;

    // Cache configuration
    private final int cacheSize;
    private final int blockSize;
    private final int cacheHitLatency;
    private final int cacheMissPenalty;

    // --- core state ---
    private final InstructionQueue iq = new InstructionQueue();
    private final List<Instruction> program;

    private final RegisterFile registerFile = new RegisterFile(NUM_REGS);
    private final MainMemory mainMemory;
    private final Cache cache;

    // Separate reservation station pools
    private final List<ReservationStation> fpAddSubStations = new ArrayList<>();  // For ADD.D, SUB.D
    private final List<ReservationStation> fpMulDivStations = new ArrayList<>();  // For MUL.D, DIV.D
    private final List<ReservationStation> intStations = new ArrayList<>();       // For INT ALU ops

    private final List<FunctionalUnit> fpUnits = new ArrayList<>();
    private final List<FunctionalUnit> intUnits = new ArrayList<>();

    private final List<LoadBuffer> loadBuffers = new ArrayList<>();
    private final List<StoreBuffer> storeBuffers = new ArrayList<>();

    private final CommonDataBus cdb = new CommonDataBus();

    private int cycle = 0;
    private long nextTagId = 1;
    private long nextSeqNum = 0; // for load/store ordering

    private final List<String> cycleLog = new ArrayList<>();

    public List<String> getCycleLog() {
        return new ArrayList<>(cycleLog);
    }

    private void log(String msg) {
        cycleLog.add(msg);
        System.out.println(msg);
    }

    /**
     * Configuration class for simulator parameters
     */
    public static class Config {
        public int numIntAluUnits = 1;
        public int numFpAddSubUnits = 1;
        public int numFpMulDivUnits = 1;
        public int numFpAddSubRs = 3;    // RS for FP ADD/SUB
        public int numFpMulDivRs = 3;    // RS for FP MUL/DIV
        public int numIntRs = 3;
        public int numLoadBuffers = 2;
        public int numStoreBuffers = 2;
        
        // Functional unit latencies
        public int intAluLatency = 1;
        public int fpAddSubLatency = 3;
        public int fpMulLatency = 5;
        public int fpDivLatency = 12;
        
        // Cache configuration
        public int cacheSize = 1024;      // bytes
        public int blockSize = 64;         // bytes
        public int cacheHitLatency = 1;    // cycles
        public int cacheMissPenalty = 10;  // additional cycles on miss
        
        // Memory size
        public int memorySize = 65536;     // 64KB
    }

    /**
     * Constructor with full configuration
     */
    public TomasuloSimulator(List<Instruction> program, Config config) {
        this.program = program;
        
        // Store configuration
        this.numIntAluUnits = config.numIntAluUnits;
        this.numFpAddSubUnits = config.numFpAddSubUnits;
        this.numFpMulDivUnits = config.numFpMulDivUnits;
        this.numFpAddSubRs = config.numFpAddSubRs;
        this.numFpMulDivRs = config.numFpMulDivRs;
        this.numIntRs = config.numIntRs;
        this.numLoadBuffers = config.numLoadBuffers;
        this.numStoreBuffers = config.numStoreBuffers;
        
        this.intAluLatency = config.intAluLatency;
        this.fpAddSubLatency = config.fpAddSubLatency;
        this.fpMulLatency = config.fpMulLatency;
        this.fpDivLatency = config.fpDivLatency;
        
        this.cacheSize = config.cacheSize;
        this.blockSize = config.blockSize;
        this.cacheHitLatency = config.cacheHitLatency;
        this.cacheMissPenalty = config.cacheMissPenalty;
        
        // Initialize memory hierarchy
        this.mainMemory = new MainMemory(config.memorySize);
        this.cache = new Cache(cacheSize, blockSize, cacheHitLatency, cacheMissPenalty, mainMemory);
        
        // Load program into IQ
        for (Instruction instr : program) {
            iq.enqueue(instr);
        }
        initStructures();
    }

    /**
     * Constructor with default configuration (for backward compatibility)
     */
    public TomasuloSimulator(List<Instruction> program) {
        this(program, new Config());
    }

    // --- helper: mapping reg names ---
    private static int r(int num) {
        return num;
    } // integer reg

    private static int f(int num) {
        return FP_BASE + num;
    } // FP reg

    // --- init microarchitectural structures ---
    private void initStructures() {
        // FP ADD/SUB Reservation Stations (A1, A2, A3...)
        for (int i = 0; i < numFpAddSubRs; i++) {
            fpAddSubStations.add(new ReservationStation(new Tag("A" + (i + 1)), ReservationStation.Type.FP_ADD_SUB));
        }
        // FP MUL/DIV Reservation Stations (M1, M2, M3...)
        for (int i = 0; i < numFpMulDivRs; i++) {
            fpMulDivStations.add(new ReservationStation(new Tag("M" + (i + 1)), ReservationStation.Type.FP_MUL_DIV));
        }
        // INT RS
        for (int i = 0; i < numIntRs; i++) {
            intStations.add(new ReservationStation(new Tag("I" + (i + 1)), ReservationStation.Type.INT_ALU));
        }

        // FUs - pass latency configuration
        // FP FUs: separate ADD/SUB and MUL/DIV
        for (int i = 0; i < numFpAddSubUnits; i++) {
            fpUnits.add(new FunctionalUnit(FunctionalUnit.Type.FP_ADD_SUB, fpAddSubLatency, fpAddSubLatency));
        }
        for (int i = 0; i < numFpMulDivUnits; i++) {
            fpUnits.add(new FunctionalUnit(FunctionalUnit.Type.FP_MUL_DIV, fpMulLatency, fpDivLatency));
        }

        // INT FUs
        for (int i = 0; i < numIntAluUnits; i++) {
            intUnits.add(new FunctionalUnit(FunctionalUnit.Type.INT_ALU, intAluLatency, intAluLatency));
        }

        // Load/Store buffers - initial latency will be updated based on cache hit/miss
        for (int i = 0; i < numLoadBuffers; i++) {
            loadBuffers.add(new LoadBuffer(new Tag("L" + (i + 1)), cacheHitLatency));
        }
        for (int i = 0; i < numStoreBuffers; i++) {
            storeBuffers.add(new StoreBuffer(new Tag("S" + (i + 1)), cache));
        }
    }

    private Tag nextTag() {
        return new Tag("T" + (nextTagId++));
    }

    private long nextSeqNum() {
        return nextSeqNum++;
    }

    // --- main simulation loop ---

    public void run(int maxCycles) {
        while (cycle < maxCycles && !isFinished()) {
            step();
        }

        System.out.println("\nSimulation finished at cycle " + cycle);
        printFinalRegisters();
    }

    public boolean isFinished() {
        return iq.isEmpty() && !anyBusy();
    }

    public void step() {
        cycle++;
        cycleLog.clear();
        log("\n========== CYCLE " + cycle + " ==========");

        // 0) Tick all RS to advance from ISSUED -> WAITING_FOR_OPERANDS
        tickReservationStations();

        // 1) Execute FUs + memory and collect finished results
        List<CdbMessage> readyMessages = new ArrayList<>();
        tickFunctionalUnits(readyMessages);
        tickLoadsStores(readyMessages);

        // 2) Broadcast at most one result on CDB (simple policy)
        if (!readyMessages.isEmpty()) {
            CdbMessage chosen = chooseMessageForCdb(readyMessages);
            log("[CDB] Broadcasting " + chosen);
            broadcastOnCdb(chosen);
            // free the producer structures
            handleProducerFree(chosen.tag());
        }

        // 3) Wake up RS (already done via CDB) and dispatch RS -> FU
        dispatchReadyRsToFus();

        // 4) Issue from IQ
        issueFromQueue();

        // 5) Log currently computing instructions
        logComputingInstructions();

        // 6) Debug prints
        printState();
    }

    private void logComputingInstructions() {
        List<String> computing = new ArrayList<>();
        for (FunctionalUnit fu : fpUnits) {
            if (!fu.isFree()) computing.add(fu.debugString());
        }
        for (FunctionalUnit fu : intUnits) {
            if (!fu.isFree()) computing.add(fu.debugString());
        }
        for (LoadBuffer lb : loadBuffers) {
            if (lb.isBusy() && lb.getState() == LoadBuffer.State.EXECUTING) computing.add("LoadBuffer " + lb.getTag() + " Executing");
        }
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy() && sb.getState() == StoreBuffer.State.EXECUTING) computing.add("StoreBuffer " + sb.getTag() + " Executing");
        }
        
        if (!computing.isEmpty()) {
            log("[EXECUTING] " + String.join(", ", computing));
        }
    }

    /**
     * Tick all reservation stations to advance their state machines
     * (ISSUED -> WAITING_FOR_OPERANDS)
     */
    private void tickReservationStations() {
        for (ReservationStation rs : fpAddSubStations) {
            rs.tick();
        }
        for (ReservationStation rs : fpMulDivStations) {
            rs.tick();
        }
        for (ReservationStation rs : intStations) {
            rs.tick();
        }
    }

    private boolean anyBusy() {
        return fpAddSubStations.stream().anyMatch(rs -> !rs.isFree())
                || fpMulDivStations.stream().anyMatch(rs -> !rs.isFree())
                || intStations.stream().anyMatch(rs -> !rs.isFree())
                || loadBuffers.stream().anyMatch(LoadBuffer::isBusy)
                || storeBuffers.stream().anyMatch(StoreBuffer::isBusy)
                || fpUnits.stream().anyMatch(fu -> !fu.isFree())
                || intUnits.stream().anyMatch(fu -> !fu.isFree());
    }

    // --- 1) Tick functional units ---

    private void tickFunctionalUnits(List<CdbMessage> readyMessages) {
        for (FunctionalUnit fu : fpUnits) {
            CdbMessage msg = fu.tick();
            if (msg != null) {
                readyMessages.add(msg);
            }
        }
        for (FunctionalUnit fu : intUnits) {
            CdbMessage msg = fu.tick();
            if (msg != null) {
                readyMessages.add(msg);
            }
        }
    }

    // --- tick loads/stores and collect load results ---

    private void tickLoadsStores(List<CdbMessage> readyMessages) {
        // Loads: may produce a CDB message
        for (LoadBuffer lb : loadBuffers) {
            lb.tick(cache);
            if (lb.isCdbReady()) {
                CdbMessage msg = lb.produceCdbMessage(cache);
                if (msg != null) {
                    readyMessages.add(msg);
                }
            }
        }
        // Stores: just advance memory commit
        for (StoreBuffer sb : storeBuffers) {
            sb.tick(cache);
        }
    }

    private String cdbStatus = "";

    public String getCdbStatus() {
        return cdbStatus;
    }

    // --- 2) choose one message to broadcast ---

    private CdbMessage chooseMessageForCdb(List<CdbMessage> readyMessages) {
        if (readyMessages.size() > 1) {
            cdbStatus = "Conflict! " + readyMessages.size() + " instructions ready. Arbitrating: Selected " + readyMessages.get(0).tag() + " (First Come First Served)";
        } else {
            cdbStatus = "Broadcasting " + readyMessages.get(0).tag();
        }
        // Simple heuristic:
        // 1) FP results
        // 2) Load results
        // 3) INT results
        // (Here we don't distinguish types in msg, so just take the first)
        return readyMessages.get(0);
    }

    private void broadcastOnCdb(CdbMessage msg) {
        // write value into RF and wake all RS / store buffers waiting on this tag
        cdb.broadcastOne(msg, allRs(), loadBuffers, storeBuffers, registerFile);
    }

    private List<ReservationStation> allRs() {
        List<ReservationStation> all = new ArrayList<>(fpAddSubStations);
        all.addAll(fpMulDivStations);
        all.addAll(intStations);
        return all;
    }

    private void handleProducerFree(Tag tag) {
        // If it was produced by an RS: free that RS
        for (ReservationStation rs : allRs()) {
            if (!rs.isFree() && rs.getTag().equals(tag) && rs.isResultReady()) {
                log("[FREE] RS " + rs.getTag() + " freed");
                rs.free();
                return;
            }
        }
        // If it was produced by a load buffer: mark it done
        for (LoadBuffer lb : loadBuffers) {
            if (lb.getTag().equals(tag) && lb.isCdbReady()) {
                log("[FREE] LoadBuffer " + lb.getTag() + " completed");
                lb.onCdbWrittenBack(registerFile);
                return;
            }
        }
    }

    // --- 3) Dispatch RS to FUs ---

    private void dispatchReadyRsToFus() {
        // FP ADD/SUB RS -> FP ADD/SUB FUs
        for (ReservationStation rs : fpAddSubStations) {
            if (rs.isWaitingForFu()) {
                FunctionalUnit fu = findFreeFuForOpcode(fpUnits, rs.getOpcode());
                if (fu != null) {
                    log("[DISPATCH] RS " + rs.getTag() +
                            " ( " + rs.getOpcode() + " ) -> FP ADD/SUB FU");
                    fu.start(rs);
                }
            }
        }
        // FP MUL/DIV RS -> FP MUL/DIV FUs
        for (ReservationStation rs : fpMulDivStations) {
            if (rs.isWaitingForFu()) {
                FunctionalUnit fu = findFreeFuForOpcode(fpUnits, rs.getOpcode());
                if (fu != null) {
                    log("[DISPATCH] RS " + rs.getTag() +
                            " ( " + rs.getOpcode() + " ) -> FP MUL/DIV FU");
                    fu.start(rs);
                }
            }
        }
        // INT RS -> INT FUs
        for (ReservationStation rs : intStations) {
            if (rs.isWaitingForFu()) {
                FunctionalUnit fu = findFreeFuForOpcode(intUnits, rs.getOpcode());
                if (fu != null) {
                    log("[DISPATCH] RS " + rs.getTag() +
                            " ( " + rs.getOpcode() + " ) -> INT FU");
                    fu.start(rs);
                }
            }
        }
    }

    private FunctionalUnit findFreeFuForOpcode(List<FunctionalUnit> fus, Opcode op) {
        for (FunctionalUnit fu : fus) {
            if (fu.isFree() && fu.supports(op)) {
                return fu;
            }
        }
        return null;
    }

    // --- 4) Issue stage ---

    private void issueFromQueue() {
        if (iq.isEmpty())
            return;

        Instruction instr = iq.peek();
        Opcode op = instr.getOpcode();

        if (instr.isLoad()) {
            // find free load buffer
            LoadBuffer lb = findFreeLoadBuffer();
            if (lb == null) {
                log("[ISSUE] Stall: no free LoadBuffer for " + op);
                return;
            }
            Tag tag = nextTag();
            long seq = nextSeqNum();
            
            // compute EA directly for now (base + offset)
            int base = instr.getBaseReg();
            int offset = instr.getOffset();
            long ea = (long) registerFile.get(base).getIntValue() + offset;
            
            // Determine latency based on cache hit/miss
            int accessLatency = cache.getAccessLatency((int) ea);
            log("[ISSUE] " + op + " -> LoadBuffer " + lb.getTag() +
                    " (tag=" + tag + ", seq=" + seq + ", latency=" + accessLatency + 
                    (cache.isHit((int) ea) ? " HIT" : " MISS") + ")");
            
            lb.issue(instr, registerFile, tag, seq, accessLatency);
            lb.setEffectiveAddress(ea);
            iq.dequeue();
            return;
        }

        if (instr.isStore()) {
            StoreBuffer sb = findFreeStoreBuffer();
            if (sb == null) {
                log("[ISSUE] Stall: no free StoreBuffer for " + op);
                return;
            }
            Tag tag = nextTag();
            long seq = nextSeqNum();
            
            // compute EA directly for now (base + offset)
            int base = instr.getBaseReg();
            int offset = instr.getOffset();
            long ea = (long) registerFile.get(base).getIntValue() + offset;
            
            // Note: Cache hit/miss latency will be determined when execution starts
            // (when the value to store is ready)
            log("[ISSUE] " + op + " -> StoreBuffer " + sb.getTag() +
                    " (tag=" + tag + ", seq=" + seq + ")");
            
            sb.issue(instr, registerFile, tag, seq);
            sb.setEffectiveAddress(ea);
            iq.dequeue();
            return;
        }

        if (instr.isFpAddSub()) {
            ReservationStation rs = findFreeRs(fpAddSubStations);
            if (rs == null) {
                log("[ISSUE] Stall: no free FP ADD/SUB RS for " + op);
                return;
            }
            log("[ISSUE] " + op + " -> RS " + rs.getTag());
            rs.issue(instr, registerFile);
            iq.dequeue();
            return;
        }

        if (instr.isFpMulDiv()) {
            ReservationStation rs = findFreeRs(fpMulDivStations);
            if (rs == null) {
                log("[ISSUE] Stall: no free FP MUL/DIV RS for " + op);
                return;
            }
            log("[ISSUE] " + op + " -> RS " + rs.getTag());
            rs.issue(instr, registerFile);
            iq.dequeue();
            return;
        }

        if (instr.isIntArithmetic()) {
            ReservationStation rs = findFreeRs(intStations);
            if (rs == null) {
                log("[ISSUE] Stall: no free INT RS for " + op);
                return;
            }
            log("[ISSUE] " + op + " -> RS " + rs.getTag());
            rs.issue(instr, registerFile);
            iq.dequeue();
            return;
        }

        // Branches / jumps: not implemented yet
        if (instr.isBranch()) {
            log("[ISSUE] Branch not implemented yet, skipping: " + op);
            iq.dequeue();
        }
    }

    private LoadBuffer findFreeLoadBuffer() {
        for (LoadBuffer lb : loadBuffers) {
            if (!lb.isBusy())
                return lb;
        }
        return null;
    }

    private StoreBuffer findFreeStoreBuffer() {
        for (StoreBuffer sb : storeBuffers) {
            if (!sb.isBusy())
                return sb;
        }
        return null;
    }

    private ReservationStation findFreeRs(List<ReservationStation> list) {
        for (ReservationStation rs : list) {
            if (rs.isFree())
                return rs;
        }
        return null;
    }

    // --- Debug printing of state ---

    private void printState() {
        System.out.println("\n[STATE] Instruction Queue size = " + queueSizeWithHead());

        System.out.println("[STATE] FP ADD/SUB Functional Units:");
        for (FunctionalUnit fu : fpUnits) {
            if (fu.getType() == FunctionalUnit.Type.FP_ADD_SUB) {
                System.out.println("  " + fu.debugString());
            }
        }

        System.out.println("[STATE] FP MUL/DIV Functional Units:");
        for (FunctionalUnit fu : fpUnits) {
            if (fu.getType() == FunctionalUnit.Type.FP_MUL_DIV) {
                System.out.println("  " + fu.debugString());
            }
        }

        System.out.println("[STATE] INT ALU Functional Units:");
        for (FunctionalUnit fu : intUnits) {
            System.out.println("  " + fu.debugString());
        }

        System.out.println("[STATE] FP ADD/SUB Reservation Stations:");
        for (ReservationStation rs : fpAddSubStations) {
            System.out.println("  " + rs.debugString());
        }

        System.out.println("[STATE] FP MUL/DIV Reservation Stations:");
        for (ReservationStation rs : fpMulDivStations) {
            System.out.println("  " + rs.debugString());
        }

        System.out.println("[STATE] INT Reservation Stations:");
        for (ReservationStation rs : intStations) {
            System.out.println("  " + rs.debugString());
        }

        System.out.println("[STATE] Load Buffers:");
        for (LoadBuffer lb : loadBuffers) {
            System.out.println("  " + lb.debugString());
        }

        System.out.println("[STATE] Store Buffers:");
        for (StoreBuffer sb : storeBuffers) {
            System.out.println("  " + sb.debugString());
        }

        System.out.println("[STATE] FP Registers (F0..F10):");
        for (int i = 0; i <= 10; i++) {
            Register r = registerFile.get(f(i));
            System.out.printf("  F%-2d = %8.3f  (Qi=%s)%n", i, r.getValue(), r.getQi());
        }

        System.out.println("[STATE] Integer Registers (R0..R5):");
        for (int i = 0; i <= 5; i++) {
            Register r = registerFile.get(r(i));
            System.out.printf("  R%-2d = %4d    (Qi=%s)%n", i, r.getIntValue(), r.getQi());
        }

        System.out.println("------------------------------------------");
    }

    private String queueSizeWithHead() {
        Instruction head = iq.peek();
        if (head == null)
            return "0";
        return iq.size() + " (head=" + head.getOpcode() + ")";
    }

    private void printFinalRegisters() {
        System.out.println("\n=== FINAL REGISTER STATE ===");
        printState();
        
        // Print cache statistics
        System.out.println("\n=== CACHE STATISTICS ===");
        cache.printStats();
    }

    // --- Public accessors for GUI/external use ---

    public RegisterFile getRegisterFile() {
        return registerFile;
    }

    public Cache getCache() {
        return cache;
    }

    public int getCycle() {
        return cycle;
    }

    public List<ReservationStation> getFpAddSubStations() {
        return fpAddSubStations;
    }

    public List<ReservationStation> getFpMulDivStations() {
        return fpMulDivStations;
    }

    public List<ReservationStation> getIntStations() {
        return intStations;
    }

    public List<LoadBuffer> getLoadBuffers() {
        return loadBuffers;
    }

    public List<StoreBuffer> getStoreBuffers() {
        return storeBuffers;
    }

    public InstructionQueue getInstructionQueue() {
        return iq;
    }

    public MainMemory getMainMemory() {
        return mainMemory;
    }


    /**
     * Set a register value (for initialization)
     */
    public void setRegister(int regIndex, double value) {
        if (regIndex == 0) return; // R0 is hardwired to 0
        registerFile.get(regIndex).setValue(value);
    }

    /**
     * Set an integer register value
     */
    public void setIntRegister(int regNum, int value) {
        if (regNum == 0) return; // R0 is hardwired to 0
        registerFile.get(r(regNum)).setValue(value);
    }

    /**
     * Set a floating-point register value
     */
    public void setFpRegister(int regNum, double value) {
        registerFile.get(f(regNum)).setValue(value);
    }

    /**
     * Store a double value in memory (bypassing cache for initialization)
     */
    public void setMemoryDouble(int address, double value) {
        mainMemory.storeDouble(address, value);
    }

    /**
     * Store a word value in memory (bypassing cache for initialization)
     */
    public void setMemoryWord(int address, int value) {
        mainMemory.storeWord(address, value);
    }

    // --- Main entry point for testing ---

    public static void main(String[] args) {
        // Create configuration
        Config config = new Config();
        config.numFpAddSubRs = 3;
        config.numFpMulDivRs = 3;
        config.numIntRs = 3;
        config.numLoadBuffers = 2;
        config.numStoreBuffers = 2;
        config.numFpAddSubUnits = 1;
        config.numFpMulDivUnits = 1;
        config.numIntAluUnits = 1;
        
        // Set latencies
        config.intAluLatency = 1;
        config.fpAddSubLatency = 3;
        config.fpMulLatency = 5;
        config.fpDivLatency = 12;
        
        // Cache configuration
        config.cacheSize = 1024;
        config.blockSize = 64;
        config.cacheHitLatency = 1;
        config.cacheMissPenalty = 10;
        
        System.out.println("===== Running Tomasulo Simulator =====");
        List<Instruction> program = com.tomasulo.parser.InstructionParser.parseFile("src/main/resources/test.txt");
        TomasuloSimulator sim = new TomasuloSimulator(program, config);
        
        // Initialize registers and memory for testing
        sim.setIntRegister(2, 100);  // R2 = 100 (base address)
        sim.setMemoryDouble(100, 1.0);  // 0(R2)
        sim.setMemoryDouble(108, 2.0);  // 8(R2)
        sim.setMemoryDouble(120, 3.0);  // 20(R2)
        sim.setFpRegister(1, 10.0);
        sim.setFpRegister(2, 2.0);
        sim.setFpRegister(3, 3.0);
        sim.setFpRegister(4, 4.0);
        
        sim.run(50);
    }
}
