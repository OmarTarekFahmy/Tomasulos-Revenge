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
    private final int numFpAddSubRs; // Separate RS for FP ADD/SUB
    private final int numFpMulDivRs; // Separate RS for FP MUL/DIV
    private final int numIntRs;
    private final int numLoadBuffers;
    private final int numStoreBuffers;
    private final int numBranchHandlers;

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
    private final List<ReservationStation> fpAddSubStations = new ArrayList<>(); // For ADD.D, SUB.D
    private final List<ReservationStation> fpMulDivStations = new ArrayList<>(); // For MUL.D, DIV.D
    private final List<ReservationStation> intStations = new ArrayList<>(); // For INT ALU ops

    private final List<FunctionalUnit> fpUnits = new ArrayList<>();
    private final List<FunctionalUnit> intUnits = new ArrayList<>();

    private final List<LoadBuffer> loadBuffers = new ArrayList<>();
    private final List<StoreBuffer> storeBuffers = new ArrayList<>();

    // Branch handling
    private final List<BranchHandler> branchHandlers = new ArrayList<>();
    private int programCounter = 0; // Current PC (index into program)
    private boolean branchPending = false; // True if a branch is in-flight
    private boolean branchTakenThisCycle = false; // True if branch taken in current cycle

    private final CommonDataBus cdb = new CommonDataBus();
    private final List<AddressUnit> addressUnits = new ArrayList<>();

    private int cycle = 0;
    private long nextSeqNum = 0; // for load/store ordering

    private final List<String> cycleLog = new ArrayList<>();
    private final List<CdbMessage> pendingCdbMessages = new ArrayList<>(); // Messages waiting for CDB

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
        public int numFpAddSubRs = 3; // RS for FP ADD/SUB
        public int numFpMulDivRs = 3; // RS for FP MUL/DIV
        public int numIntRs = 3;
        public int numLoadBuffers = 2;
        public int numStoreBuffers = 2;
        public int numBranchHandlers = 1; // Branch unit(s)
        public int numAddressUnits = 2; // Address calculation units

        // Functional unit latencies
        public int intAluLatency = 1;
        public int fpAddSubLatency = 3;
        public int fpMulLatency = 5;
        public int fpDivLatency = 12;
        public int branchLatency = 1; // Latency for branch evaluation
        public int addressLatency = 1; // Latency for address calculation

        // Cache configuration
        public int cacheSize = 256; // bytes
        public int blockSize = 8; // bytes
        public int cacheHitLatency = 1; // cycles
        public int cacheMissPenalty = 10; // additional cycles on miss

        // Memory size
        public int memorySize = 65536; // 64KB
    }

    /**
     * Constructor with full configuration
     */
    public TomasuloSimulator(List<Instruction> program, Config config) {
        this.program = program;

        // Store configuration
        this.numFpAddSubRs = config.numFpAddSubRs;
        this.numFpMulDivRs = config.numFpMulDivRs;
        this.numIntRs = config.numIntRs;
        this.numLoadBuffers = config.numLoadBuffers;
        this.numStoreBuffers = config.numStoreBuffers;
        this.numBranchHandlers = config.numBranchHandlers;

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
        mainMemory.storeDouble(0, 8);
        mainMemory.storeDouble(8, 6);
        mainMemory.storeDouble(16, 4);
        mainMemory.storeDouble(24, 2);
        mainMemory.storeDouble(32, 1);
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
        for (int i = 0; i < numFpAddSubRs; i++) {
            fpUnits.add(new FunctionalUnit(FunctionalUnit.Type.FP_ADD_SUB, fpAddSubLatency, fpAddSubLatency));
        }
        for (int i = 0; i < numFpMulDivRs; i++) {
            fpUnits.add(new FunctionalUnit(FunctionalUnit.Type.FP_MUL_DIV, fpMulLatency, fpDivLatency));
        }

        // INT FUs
        for (int i = 0; i < numIntRs; i++) {
            intUnits.add(new FunctionalUnit(FunctionalUnit.Type.INT_ALU, intAluLatency, intAluLatency));
        }

        // Load/Store buffers - initial latency will be updated based on cache hit/miss
        for (int i = 0; i < numLoadBuffers; i++) {
            loadBuffers.add(new LoadBuffer(new Tag("L" + (i + 1)), cacheHitLatency));
        }
        for (int i = 0; i < numStoreBuffers; i++) {
            storeBuffers.add(new StoreBuffer(new Tag("S" + (i + 1)), cache));
        }

        // Branch handlers
        for (int i = 0; i < numBranchHandlers; i++) {
            branchHandlers.add(new BranchHandler(new Tag("B" + (i + 1))));
        }

        // Address units
        for (int i = 0; i < numLoadBuffers + numStoreBuffers; i++) { // Default to 2 address units
            addressUnits.add(new AddressUnit(0)); // 1 cycle latency
        }
    }

    private long nextSeqNum() {
        return nextSeqNum++;
    }

    // --- main simulation loop ---

    public boolean isFinished() {
        return iq.isEmpty() && !anyBusy() && !branchPending;
    }

    public void step() {
        cycle++;
        cycleLog.clear();
        log("\n========== CYCLE " + cycle + " ==========");

        // Reset branch taken flag at start of cycle
        branchTakenThisCycle = false;

        // 0) Tick all RS, branch handlers, and address units to advance state machines
        tickReservationStations();
        tickBranchHandlers();
        tickAddressUnits();

        // 1) Execute FUs + memory and collect finished results
        List<CdbMessage> readyMessages = new ArrayList<>();

        // Add any pending messages from previous cycles first (they have priority)
        readyMessages.addAll(pendingCdbMessages);
        pendingCdbMessages.clear();

        tickFunctionalUnits(readyMessages);
        tickLoadsStores(readyMessages);

        // 2) Broadcast at most one result on CDB (intelligent priority policy)
        if (!readyMessages.isEmpty()) {
            CdbMessage chosen = chooseMessageForCdb(readyMessages);
            log("[CDB] Broadcasting " + chosen);
            broadcastOnCdb(chosen);
            // free the producer structures
            handleProducerFree(chosen.tag());

            // Save remaining messages for next cycle
            readyMessages.remove(chosen);
            pendingCdbMessages.addAll(readyMessages);

            if (!readyMessages.isEmpty()) {
                log("[CDB] " + readyMessages.size() + " message(s) deferred to next cycle: " +
                        readyMessages.stream().map(m -> m.tag().toString()).reduce((a, b) -> a + ", " + b).orElse(""));
            }
        }

        // 3) Evaluate branches that are ready
        evaluateBranches();

        // 4) Wake up RS (already done via CDB) and dispatch RS -> FU
        dispatchReadyRsToFus();

        // 5) Issue from IQ
        issueFromQueue();

        // 6) Log currently computing instructions
        logComputingInstructions();

        // 7) Debug prints
        printState();
    }

    private void logComputingInstructions() {
        List<String> computing = new ArrayList<>();
        for (FunctionalUnit fu : fpUnits) {
            if (!fu.isFree())
                computing.add(fu.debugString());
        }
        for (FunctionalUnit fu : intUnits) {
            if (!fu.isFree())
                computing.add(fu.debugString());
        }
        for (LoadBuffer lb : loadBuffers) {
            if (lb.isBusy() && lb.getState() == LoadBuffer.State.EXECUTING)
                computing.add("LoadBuffer " + lb.getTag() + " Executing");
        }
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy() && sb.getState() == StoreBuffer.State.EXECUTING)
                computing.add("StoreBuffer " + sb.getTag() + " Executing");
        }

        if (!computing.isEmpty()) {
            log("[EXECUTING] \n" + String.join(", ", computing));
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

    /**
     * Tick all branch handlers to advance their state machines
     */
    private void tickBranchHandlers() {
        for (BranchHandler bh : branchHandlers) {
            bh.tick();
        }
    }

    /**
     * Tick all address units to advance address calculations
     */
    private void tickAddressUnits() {
        for (AddressUnit au : addressUnits) {
            au.tick();
        }
    }

    /**
     * Evaluate branches that are ready and handle taken branches
     */
    private void evaluateBranches() {
        for (BranchHandler bh : branchHandlers) {
            if (bh.isReadyToEvaluate()) {
                boolean taken = bh.evaluate();
                Instruction instr = bh.getInstruction();

                log("[BRANCH] " + instr.getOpcode() + " evaluated: " + (taken ? "TAKEN" : "NOT TAKEN") +
                        ", nextPC=" + bh.getNextPC());

                if (taken) {
                    // Jump to target - reload instruction queue from target PC
                    int targetPC = bh.getNextPC();
                    reloadInstructionQueue(targetPC);
                    branchTakenThisCycle = true;
                }

                // Free the branch handler
                bh.free();
                branchPending = false;
            }
        }
    }

    /**
     * Reload the instruction queue starting from the given PC (instruction index)
     */
    private void reloadInstructionQueue(int targetPC) {
        // Clear current instruction queue
        while (!iq.isEmpty()) {
            iq.dequeue();
        }

        // Reload from target PC
        programCounter = targetPC;
        for (int i = targetPC; i < program.size(); i++) {
            iq.enqueue(program.get(i));
        }

        log("[BRANCH] Reloaded IQ from PC=" + targetPC + ", " + iq.size() + " instructions remaining");
    }

    private boolean anyBusy() {
        return fpAddSubStations.stream().anyMatch(rs -> !rs.isFree())
                || fpMulDivStations.stream().anyMatch(rs -> !rs.isFree())
                || intStations.stream().anyMatch(rs -> !rs.isFree())
                || loadBuffers.stream().anyMatch(LoadBuffer::isBusy)
                || storeBuffers.stream().anyMatch(StoreBuffer::isBusy)
                || fpUnits.stream().anyMatch(fu -> !fu.isFree())
                || intUnits.stream().anyMatch(fu -> !fu.isFree())
                || branchHandlers.stream().anyMatch(BranchHandler::isBusy);
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
            lb.tick(cache, storeBuffers); // Pass storeBuffers for memory ordering
            if (lb.isCdbReady()) {
                CdbMessage msg = lb.produceCdbMessage(cache);
                if (msg != null) {
                    readyMessages.add(msg);
                }
            }
        }
        // Stores: just advance memory commit
        for (StoreBuffer sb : storeBuffers) {
            sb.tick(cache, loadBuffers, storeBuffers); // Pass both lists for memory ordering
        }
    }

    private String cdbStatus = "";

    public String getCdbStatus() {
        return cdbStatus;
    }

    // --- 2) choose one message to broadcast ---

    private CdbMessage chooseMessageForCdb(List<CdbMessage> readyMessages) {
        if (readyMessages.size() == 1) {
            cdbStatus = "Broadcasting " + readyMessages.get(0).tag();
            return readyMessages.get(0);
        }

        // Multiple messages ready - use intelligent prioritization
        // Priority 1: Count how many instructions are waiting for each tag
        CdbMessage bestMsg = null;
        int maxDependents = -1;
        int maxReadyDependents = -1;
        String arbitrationReason = "";

        // Debug: log all candidates with their dependent counts
        StringBuilder debugLog = new StringBuilder("[CDB PRIORITY] Candidates: ");
        for (CdbMessage msg : readyMessages) {
            int dependentCount = countDependents(msg.tag());
            int readyDependentCount = countReadyDependents(msg.tag());
            debugLog.append(msg.tag()).append("(").append(dependentCount).append(" deps, ")
                    .append(readyDependentCount).append(" ready) ");

            if (dependentCount > maxDependents) {
                maxDependents = dependentCount;
                maxReadyDependents = readyDependentCount;
                bestMsg = msg;
                arbitrationReason = "most dependents (" + dependentCount + ")";
            } else if (dependentCount == maxDependents) {
                // Tie-breaker: Choose the one with most dependents that can start immediately
                if (readyDependentCount > maxReadyDependents) {
                    maxReadyDependents = readyDependentCount;
                    bestMsg = msg;
                    arbitrationReason = "tie-break: most ready dependents (" + readyDependentCount + " of "
                            + dependentCount + ")";
                }
                // If still tied, keep the first one (FCFS)
            }
        }
        log(debugLog.toString());

        // If no dependents for any, just take first (FCFS)
        if (bestMsg == null) {
            bestMsg = readyMessages.get(0);
            arbitrationReason = "First Come First Served";
        }

        cdbStatus = "Conflict! " + readyMessages.size() + " instructions ready. Selected " +
                bestMsg.tag() + " (" + arbitrationReason + ")";

        return bestMsg;
    }

    /**
     * Count how many dependents are ready to start execution immediately
     * (only waiting for this one operand)
     */
    private int countReadyDependents(Tag tag) {
        int count = 0;

        // Check reservation stations that are waiting for ONLY this tag
        for (ReservationStation rs : allRs()) {
            if (rs.isBusy() && rs.getState() == ReservationStation.State.WAITING_FOR_OPERANDS) {
                boolean waitingForThisTag = false;
                boolean hasOtherDependency = false;

                if (tag.equals(rs.getQj())) {
                    waitingForThisTag = true;
                }
                if (tag.equals(rs.getQk())) {
                    waitingForThisTag = true;
                }

                // Check if there are other dependencies
                if (!tag.equals(rs.getQj()) && rs.getQj() != null && rs.getQj() != Tag.NONE) {
                    hasOtherDependency = true;
                }
                if (!tag.equals(rs.getQk()) && rs.getQk() != null && rs.getQk() != Tag.NONE) {
                    hasOtherDependency = true;
                }

                // Count if waiting for this tag and has no other dependencies
                if (waitingForThisTag && !hasOtherDependency) {
                    count++;
                }
            }
        }

        // Check store buffers that are waiting for ONLY this value
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy() && tag.equals(sb.getSourceTag())) {
                // Check if address is ready (no other dependency)
                if (sb.getState() == StoreBuffer.State.WAITING_FOR_VALUE) {
                    count++;
                }
            }
        }

        // Check branch handlers waiting for ONLY this tag
        for (BranchHandler bh : branchHandlers) {
            if (bh.isBusy() && bh.getState() == BranchHandler.State.WAITING_FOR_OPERANDS) {
                boolean waitingForThisTag = false;
                boolean hasOtherDependency = false;

                if (tag.equals(bh.getQj())) {
                    waitingForThisTag = true;
                }
                if (tag.equals(bh.getQk())) {
                    waitingForThisTag = true;
                }

                // Check if there are other dependencies
                if (!tag.equals(bh.getQj()) && bh.getQj() != null && bh.getQj() != Tag.NONE) {
                    hasOtherDependency = true;
                }
                if (!tag.equals(bh.getQk()) && bh.getQk() != null && bh.getQk() != Tag.NONE) {
                    hasOtherDependency = true;
                }

                // Count if waiting for this tag and has no other dependencies
                if (waitingForThisTag && !hasOtherDependency) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Count how many instructions are waiting for this tag
     */
    private int countDependents(Tag tag) {
        int count = 0;

        StringBuilder debugInfo = new StringBuilder("[COUNT_DEPS for " + tag + "] ");

        // Check all reservation stations
        for (ReservationStation rs : allRs()) {
            if (rs.isBusy()) {
                if (tag.equals(rs.getQj())) {
                    count++;
                    debugInfo.append(rs.getTag()).append(".Qj ");
                }
                if (tag.equals(rs.getQk())) {
                    count++;
                    debugInfo.append(rs.getTag()).append(".Qk ");
                }
            }
        }

        // Check load buffers - currently EA is computed at issue, so no base
        // dependencies
        // but checking state for waiting on address in case of future refactoring
        for (LoadBuffer lb : loadBuffers) {
            if (lb.isBusy() && lb.getState() == LoadBuffer.State.WAITING_FOR_ADDRESS) {
                // If base register has a dependency on this tag, count it
                int baseReg = lb.getBaseRegIndex();
                if (baseReg >= 0) {
                    Register baseRegister = registerFile.get(baseReg);
                    if (tag.equals(baseRegister.getQi())) {
                        count++;
                        debugInfo.append(lb.getTag()).append(".base ");
                    }
                }
            }
        }

        // Check store buffers waiting for value or address
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy()) {
                // Check if waiting for source value
                if (tag.equals(sb.getSourceTag())) {
                    count++;
                    debugInfo.append(sb.getTag()).append(".src ");
                }
                // Check if waiting for base address calculation
                if (sb.getState() == StoreBuffer.State.WAITING_FOR_ADDRESS) {
                    int baseReg = sb.getBaseRegIndex();
                    if (baseReg >= 0) {
                        Register baseRegister = registerFile.get(baseReg);
                        if (tag.equals(baseRegister.getQi())) {
                            count++;
                            debugInfo.append(sb.getTag()).append(".base ");
                        }
                    }
                }
            }
        }

        // Check branch handlers
        for (BranchHandler bh : branchHandlers) {
            if (bh.isBusy()) {
                if (tag.equals(bh.getQj())) {
                    count++;
                    debugInfo.append(bh.getTag()).append(".Qj ");
                }
                if (tag.equals(bh.getQk())) {
                    count++;
                    debugInfo.append(bh.getTag()).append(".Qk ");
                }
            }
        }

        if (count > 0) {
            debugInfo.append("= ").append(count).append(" dependents");
            System.out.println(debugInfo.toString());
        }

        return count;
    }

    private void broadcastOnCdb(CdbMessage msg) {
        // write value into RF and wake all RS / store buffers / branch handlers waiting
        // on this tag
        cdb.broadcastOne(msg, allRs(), loadBuffers, storeBuffers, registerFile);

        // Also broadcast to branch handlers
        for (BranchHandler bh : branchHandlers) {
            bh.onCdbBroadcast(msg.tag(), msg.value());
        }
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
                    // Note: fu.start() marks the FU as busy, so it won't be selected again this
                    // cycle
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
                    // Note: fu.start() marks the FU as busy, so it won't be selected again this
                    // cycle
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
                    // Note: fu.start() marks the FU as busy, so it won't be selected again this
                    // cycle
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

        // Don't issue in the same cycle a branch is taken
        if (branchTakenThisCycle) {
            log("[ISSUE] Stall: branch taken this cycle, will issue next cycle");
            return;
        }

        Instruction instr = iq.peek();
        Opcode op = instr.getOpcode();

        if (instr.isLoad()) {
            // find free load buffer
            LoadBuffer lb = findFreeLoadBuffer();
            if (lb == null) {
                log("[ISSUE] Stall: no free LoadBuffer for " + op);
                return;
            }

            // Find free address unit
            AddressUnit au = findFreeAddressUnit();
            if (au == null) {
                log("[ISSUE] Stall: no free AddressUnit for " + op);
                return;
            }

            long seq = nextSeqNum();

            // Determine latency based on cache hit/miss (estimate with base reg value)
            int base = instr.getBaseReg();
            int offset = instr.getOffset();
            long estimatedEa = (long) registerFile.get(base).getIntValue() + offset;
            int accessLatency = cache.getAccessLatency((int) estimatedEa);

            log("[ISSUE] " + op + " -> LoadBuffer " + lb.getTag() +
                    " (tag=" + lb.getTag() + ", seq=" + seq + ", latency=" + accessLatency +
                    (cache.isHit((int) estimatedEa) ? " HIT" : " MISS") + ")");

            lb.issue(instr, registerFile, lb.getTag(), seq, accessLatency);

            // Start address calculation in address unit
            au.startForLoad(lb, registerFile);

            iq.dequeue();
            programCounter++;
            return;
        }

        if (instr.isStore()) {
            StoreBuffer sb = findFreeStoreBuffer();
            if (sb == null) {
                log("[ISSUE] Stall: no free StoreBuffer for " + op);
                return;
            }

            // Find free address unit
            AddressUnit au = findFreeAddressUnit();
            if (au == null) {
                log("[ISSUE] Stall: no free AddressUnit for " + op);
                return;
            }

            long seq = nextSeqNum();

            // Note: Cache hit/miss latency will be determined when execution starts
            // (when the value to store is ready)
            log("[ISSUE] " + op + " -> StoreBuffer " + sb.getTag() +
                    " (tag=" + sb.getTag() + ", seq=" + seq + ")");

            sb.issue(instr, registerFile, sb.getTag(), seq);

            // Start address calculation in address unit
            au.startForStore(sb, registerFile);

            iq.dequeue();
            programCounter++;
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
            programCounter++;
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
            programCounter++;
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
            programCounter++;
            return;
        }

        // Branches (BEQ, BNE)
        if (instr.isBranch()) {
            // If a branch is already pending, stall
            if (branchPending) {
                log("[ISSUE] Stall: branch already pending for " + op);
                return;
            }

            // Find free branch handler
            BranchHandler bh = findFreeBranchHandler();
            if (bh == null) {
                log("[ISSUE] Stall: no free BranchHandler for " + op);
                return;
            }

            log("[ISSUE] " + op + " -> BranchHandler " + bh.getTag() + " (PC=" + programCounter + ")");
            bh.issue(instr, registerFile, programCounter);
            branchPending = true;
            iq.dequeue();
            programCounter++;
            return;
        }

        // Unknown instruction type
        log("[ISSUE] Unknown instruction type, skipping: " + op);
        iq.dequeue();
        programCounter++;
    }

    private BranchHandler findFreeBranchHandler() {
        for (BranchHandler bh : branchHandlers) {
            if (bh.isFree())
                return bh;
        }
        return null;
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

    private AddressUnit findFreeAddressUnit() {
        for (AddressUnit au : addressUnits) {
            if (au.isFree())
                return au;
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

        System.out.println("[STATE] Branch Handlers:");
        for (BranchHandler bh : branchHandlers) {
            System.out.println("  " + bh.debugString());
        }
        System.out.println("[STATE] Branch Pending: " + branchPending + ", PC: " + programCounter);

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

    public List<BranchHandler> getBranchHandlers() {
        return branchHandlers;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public boolean isBranchPending() {
        return branchPending;
    }

    public InstructionQueue getInstructionQueue() {
        return iq;
    }

    public MainMemory getMainMemory() {
        return mainMemory;
    }

    // --- Main entry point for testing ---

    /*
     * public static void main(String[] args) {
     * // Create configuration
     * Config config = new Config();
     * config.numFpAddSubRs = 3;
     * config.numFpMulDivRs = 3;
     * config.numIntRs = 3;
     * config.numLoadBuffers = 2;
     * config.numStoreBuffers = 2;
     * config.numFpAddSubUnits = 1;
     * config.numFpMulDivUnits = 1;
     * config.numIntAluUnits = 1;
     * 
     * // Set latencies
     * config.intAluLatency = 1;
     * config.fpAddSubLatency = 3;
     * config.fpMulLatency = 5;
     * config.fpDivLatency = 12;
     * 
     * // Cache configuration
     * config.cacheSize = 1024;
     * config.blockSize = 64;
     * config.cacheHitLatency = 1;
     * config.cacheMissPenalty = 10;
     * 
     * System.out.println("===== Running Tomasulo Simulator =====");
     * List<Instruction> program =
     * com.tomasulo.parser.InstructionParser.parseFile("src/main/resources/test.txt"
     * );
     * TomasuloSimulator sim = new TomasuloSimulator(program, config);
     * 
     * // Initialize registers and memory for testing
     * // For the loop program: R1 starts at 0, gets 24 added, decrements by 8 until
     * // R1==R2
     * // R2 starts at 0 (loop termination condition)
     * sim.setIntRegister(1, 0); // R1 = 0 (will become 24 after DADDI)
     * sim.setIntRegister(2, 0); // R2 = 0 (loop termination value)
     * 
     * // Memory locations that will be accessed: 8(R1) where R1 = 24, 16, 8
     * // So addresses: 32, 24, 16
     * sim.setMemoryDouble(32, 1.0); // 8 + 24 = 32
     * sim.setMemoryDouble(24, 2.0); // 8 + 16 = 24
     * sim.setMemoryDouble(16, 3.0); // 8 + 8 = 16
     * 
     * sim.setFpRegister(2, 2.0); // F2 = 2.0 (multiplier)
     * 
     * sim.run(100);
     * }
     */
}
