package com.tomasulo.gui;

import com.tomasulo.core.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Controller that bridges the GUI with the core Tomasulo simulator.
 * Manages simulation state and provides data for the tables.
 */
public class SimulatorController {

    private final SimulatorConfig config;
    private MainSimulatorPanel mainPanel;

    // Simulation state
    private int currentCycle = 0;
    private boolean simulationComplete = false;

    // Core simulator components (configured based on SimulatorConfig)
    private RegisterFile registerFile;
    private IMemory memory;
    private DataCache dataCache;
    private InstructionQueue instructionQueue;

    // Reservation Stations by type
    private List<ReservationStation> fpAddSubStations = new ArrayList<>();
    private List<ReservationStation> fpMulDivStations = new ArrayList<>();
    private List<ReservationStation> intAluStations = new ArrayList<>();
    private List<ReservationStation> intMulDivStations = new ArrayList<>();

    // Functional Units by type
    private List<FunctionalUnit> fpAddSubUnits = new ArrayList<>();
    private List<FunctionalUnit> fpMulDivUnits = new ArrayList<>();
    private List<FunctionalUnit> intAluUnits = new ArrayList<>();
    private List<FunctionalUnit> intMulDivUnits = new ArrayList<>();

    // Load/Store Buffers
    private List<LoadBuffer> loadBuffers = new ArrayList<>();
    private List<StoreBuffer> storeBuffers = new ArrayList<>();

    // Common Data Bus
    private CommonDataBus cdb = new CommonDataBus();

    // Instruction tracking for timing display
    private List<InstructionStatus> instructionStatuses = new ArrayList<>();

    // Tag counter
    private long nextTagId = 1;
    private long nextSeqNum = 0;

    public SimulatorController(SimulatorConfig config) {
        this.config = config;
        initializeSimulator();
    }

    public void setMainPanel(MainSimulatorPanel panel) {
        this.mainPanel = panel;
    }

    private void initializeSimulator() {
        // Initialize register file (32 INT + 32 FP registers)
        int totalRegs = config.getNumIntRegisters() + config.getNumFpRegisters();
        registerFile = new RegisterFile(totalRegs);

        // Initialize memory and cache
        MainMemory mainMemory = new MainMemory();
        dataCache = new DataCache(
            mainMemory, 
            config.getCacheBlockSize(),
            config.getCacheSizeBytes(),
            config.getCacheHitLatency(),
            config.getCacheMissPenalty()
        );
        memory = dataCache;

        // Initialize instruction queue
        instructionQueue = new InstructionQueue();

        // Initialize Reservation Stations
        initializeReservationStations();

        // Initialize Functional Units
        initializeFunctionalUnits();

        // Initialize Load/Store Buffers
        initializeLoadStoreBuffers();

        // Set some default register values for testing
        initializeDefaultValues();
    }

    private void initializeReservationStations() {
        fpAddSubStations.clear();
        fpMulDivStations.clear();
        intAluStations.clear();
        intMulDivStations.clear();

        for (int i = 0; i < config.getFpAddSubStations(); i++) {
            fpAddSubStations.add(new ReservationStation(new Tag("Add" + (i + 1))));
        }
        for (int i = 0; i < config.getFpMulDivStations(); i++) {
            fpMulDivStations.add(new ReservationStation(new Tag("Mul" + (i + 1))));
        }
        for (int i = 0; i < config.getIntAluStations(); i++) {
            intAluStations.add(new ReservationStation(new Tag("Int" + (i + 1))));
        }
        for (int i = 0; i < config.getIntMulDivStations(); i++) {
            intMulDivStations.add(new ReservationStation(new Tag("IMD" + (i + 1))));
        }
    }

    private void initializeFunctionalUnits() {
        fpAddSubUnits.clear();
        fpMulDivUnits.clear();
        intAluUnits.clear();
        intMulDivUnits.clear();

        for (int i = 0; i < config.getFpAddSubUnits(); i++) {
            FunctionalUnit fu = new FunctionalUnit(FunctionalUnit.Type.FP_ADD_SUB);
            fu.setLatencies(config.getFpAddLatency(), config.getFpSubLatency(), 
                           config.getFpMulLatency(), config.getFpDivLatency());
            fpAddSubUnits.add(fu);
        }
        for (int i = 0; i < config.getFpMulDivUnits(); i++) {
            FunctionalUnit fu = new FunctionalUnit(FunctionalUnit.Type.FP_MUL_DIV);
            fu.setLatencies(config.getFpAddLatency(), config.getFpSubLatency(), 
                           config.getFpMulLatency(), config.getFpDivLatency());
            fpMulDivUnits.add(fu);
        }
        for (int i = 0; i < config.getIntAluUnits(); i++) {
            FunctionalUnit fu = new FunctionalUnit(FunctionalUnit.Type.INT_ALU);
            fu.setLatencies(config.getIntAddLatency(), config.getIntSubLatency(), 
                           config.getIntMulLatency(), config.getIntDivLatency());
            intAluUnits.add(fu);
        }
        for (int i = 0; i < config.getIntMulDivUnits(); i++) {
            FunctionalUnit fu = new FunctionalUnit(FunctionalUnit.Type.INT_MULDIV);
            fu.setLatencies(config.getIntAddLatency(), config.getIntSubLatency(), 
                           config.getIntMulLatency(), config.getIntDivLatency());
            intMulDivUnits.add(fu);
        }
    }

    private void initializeLoadStoreBuffers() {
        loadBuffers.clear();
        storeBuffers.clear();

        for (int i = 0; i < config.getLoadBuffers(); i++) {
            loadBuffers.add(new LoadBuffer(new Tag("L" + (i + 1)), config.getLoadLatency()));
        }
        for (int i = 0; i < config.getStoreBuffers(); i++) {
            storeBuffers.add(new StoreBuffer(new Tag("S" + (i + 1)), config.getStoreLatency()));
        }
    }

    private void initializeDefaultValues() {
        // Set some default register values for testing
        registerFile.get(2).setValue(100);  // R2 = 100 (base address)
        
        // Initialize some FP registers
        registerFile.get(fpIndex(1)).setValue(10.0);
        registerFile.get(fpIndex(2)).setValue(2.0);
        registerFile.get(fpIndex(3)).setValue(3.0);
        registerFile.get(fpIndex(4)).setValue(4.0);

        // Initialize some memory values
        memory.storeDouble(100, 1.0);   // 0(R2)
        memory.storeDouble(108, 2.0);   // 8(R2)
        memory.storeDouble(120, 3.0);   // 20(R2)
    }

    // Helper to get FP register index
    private int fpIndex(int num) {
        return config.getNumIntRegisters() + num;
    }

    private Tag nextTag() {
        return new Tag("T" + (nextTagId++));
    }

    private long nextSeqNum() {
        return nextSeqNum++;
    }

    // --- Simulation Control ---

    public void step() {
        if (simulationComplete) {
            if (mainPanel != null) {
                mainPanel.appendLog("Simulation already complete.");
            }
            return;
        }

        currentCycle++;
        
        // Execute one cycle of the Tomasulo algorithm
        executeCycle();

        // Check if simulation is complete
        if (isSimulationComplete()) {
            simulationComplete = true;
            if (mainPanel != null) {
                mainPanel.appendLog("Simulation complete!");
            }
        }
    }

    public void runToCompletion() {
        int maxCycles = 1000; // Safety limit
        while (!simulationComplete && currentCycle < maxCycles) {
            step();
        }
        if (currentCycle >= maxCycles) {
            if (mainPanel != null) {
                mainPanel.appendLog("Reached maximum cycle limit (" + maxCycles + ")");
            }
        }
    }

    public void reset() {
        currentCycle = 0;
        simulationComplete = false;
        nextTagId = 1;
        nextSeqNum = 0;
        instructionStatuses.clear();
        
        initializeSimulator();
        
        if (mainPanel != null) {
            mainPanel.appendLog("Simulator reset.");
        }
    }

    private void executeCycle() {
        List<CdbMessage> readyMessages = new ArrayList<>();

        // 1) Tick functional units and collect ready results
        tickFunctionalUnits(readyMessages);
        tickLoadsStores(readyMessages);

        // 2) Broadcast results on CDB (handle conflicts - priority based)
        if (!readyMessages.isEmpty()) {
            CdbMessage chosen = chooseMessageForCdb(readyMessages);
            broadcastOnCdb(chosen);
            handleProducerFree(chosen.tag());
            if (mainPanel != null) {
                mainPanel.appendLog("CDB broadcast: " + chosen.tag() + " = " + chosen.value());
            }
        }

        // 3) Dispatch ready RS to FUs
        dispatchReadyRsToFus();

        // 4) Issue from instruction queue
        issueFromQueue();
    }

    private void tickFunctionalUnits(List<CdbMessage> readyMessages) {
        for (FunctionalUnit fu : getAllFunctionalUnits()) {
            CdbMessage msg = fu.tick();
            if (msg != null) {
                readyMessages.add(msg);
            }
        }
    }

    private void tickLoadsStores(List<CdbMessage> readyMessages) {
        for (LoadBuffer lb : loadBuffers) {
            lb.tick(memory);
            if (lb.isCdbReady()) {
                CdbMessage msg = lb.produceCdbMessage(memory);
                if (msg != null) {
                    readyMessages.add(msg);
                }
            }
        }
        for (StoreBuffer sb : storeBuffers) {
            sb.tick(memory);
        }
    }

    private CdbMessage chooseMessageForCdb(List<CdbMessage> readyMessages) {
        return readyMessages.get(0);
    }

    private void broadcastOnCdb(CdbMessage msg) {
        cdb.broadcastOne(msg, getAllReservationStations(), loadBuffers, storeBuffers, registerFile);
        
        // Update instruction status for write-back cycle
        for (InstructionStatus status : instructionStatuses) {
            if (status.getTag() != null && status.getTag().equals(msg.tag()) && status.getWriteBackCycle() < 0) {
                status.setWriteBackCycle(currentCycle);
                break;
            }
        }
    }

    private void handleProducerFree(Tag tag) {
        for (ReservationStation rs : getAllReservationStations()) {
            if (!rs.isFree() && rs.getTag().equals(tag) && rs.isResultReady()) {
                rs.free();
                return;
            }
        }
        for (LoadBuffer lb : loadBuffers) {
            if (lb.getTag().equals(tag) && lb.isCdbReady()) {
                lb.onCdbWrittenBack(registerFile);
                return;
            }
        }
    }

    private void dispatchReadyRsToFus() {
        dispatchRsToFus(fpAddSubStations, fpAddSubUnits);
        dispatchRsToFus(fpMulDivStations, fpMulDivUnits);
        dispatchRsToFus(intAluStations, intAluUnits);
        dispatchRsToFus(intMulDivStations, intMulDivUnits);
    }

    private void dispatchRsToFus(List<ReservationStation> stations, List<FunctionalUnit> units) {
        for (ReservationStation rs : stations) {
            if (rs.isWaitingForFu()) {
                for (FunctionalUnit fu : units) {
                    if (fu.isFree() && fu.supports(rs.getOpcode())) {
                        fu.start(rs);
                        
                        for (InstructionStatus status : instructionStatuses) {
                            if (status.getTag() != null && status.getTag().equals(rs.getTag()) 
                                && status.getExecStartCycle() < 0) {
                                status.setExecStartCycle(currentCycle);
                                break;
                            }
                        }
                        
                        if (mainPanel != null) {
                            mainPanel.appendLog("Dispatched " + rs.getTag() + " (" + rs.getOpcode() + ") to FU");
                        }
                        break;
                    }
                }
            }
        }
    }

    private void issueFromQueue() {
        if (instructionQueue.isEmpty()) return;

        Instruction instr = instructionQueue.peek();
        Instruction.Opcode op = instr.getOpcode();

        boolean issued = false;

        if (instr.isLoad()) {
            issued = issueLoad(instr);
        } else if (instr.isStore()) {
            issued = issueStore(instr);
        } else if (instr.isFp()) {
            issued = issueFpInstruction(instr);
        } else if (instr.isIntArithOrLogic()) {
            issued = issueIntInstruction(instr);
        } else if (instr.isBranchOrJump()) {
            if (mainPanel != null) {
                mainPanel.appendLog("Branch/Jump: " + op + " - stalling (no prediction)");
            }
            instructionQueue.dequeue();
            issued = true;
        }

        if (issued) {
            instructionQueue.dequeue();
        }
    }

    private boolean issueLoad(Instruction instr) {
        LoadBuffer lb = findFreeLoadBuffer();
        if (lb == null) {
            if (mainPanel != null) {
                mainPanel.appendLog("Stall: No free load buffer");
            }
            return false;
        }

        Tag tag = nextTag();
        long seq = nextSeqNum();
        lb.issue(instr, registerFile, tag, seq);
        
        int base = instr.getBaseReg();
        int offset = instr.getOffset();
        long ea = (long) registerFile.get(base).getIntValue() + offset;
        lb.setEffectiveAddress(ea);

        InstructionStatus status = new InstructionStatus(instr, tag);
        status.setIssueCycle(currentCycle);
        instructionStatuses.add(status);

        if (mainPanel != null) {
            mainPanel.appendLog("Issued " + instr.getOpcode() + " -> " + lb.getTag());
        }
        return true;
    }

    private boolean issueStore(Instruction instr) {
        StoreBuffer sb = findFreeStoreBuffer();
        if (sb == null) {
            if (mainPanel != null) {
                mainPanel.appendLog("Stall: No free store buffer");
            }
            return false;
        }

        Tag tag = nextTag();
        long seq = nextSeqNum();
        sb.issue(instr, registerFile, tag, seq);
        
        int base = instr.getBaseReg();
        int offset = instr.getOffset();
        long ea = (long) registerFile.get(base).getIntValue() + offset;
        sb.setEffectiveAddress(ea);

        InstructionStatus status = new InstructionStatus(instr, tag);
        status.setIssueCycle(currentCycle);
        instructionStatuses.add(status);

        if (mainPanel != null) {
            mainPanel.appendLog("Issued " + instr.getOpcode() + " -> " + sb.getTag());
        }
        return true;
    }

    private boolean issueFpInstruction(Instruction instr) {
        ReservationStation rs = null;
        Instruction.Opcode op = instr.getOpcode();
        
        if (op == Instruction.Opcode.ADD_D || op == Instruction.Opcode.SUB_D ||
            op == Instruction.Opcode.ADD_S || op == Instruction.Opcode.SUB_S) {
            rs = findFreeRs(fpAddSubStations);
        } else if (op == Instruction.Opcode.MUL_D || op == Instruction.Opcode.DIV_D ||
                   op == Instruction.Opcode.MUL_S || op == Instruction.Opcode.DIV_S) {
            rs = findFreeRs(fpMulDivStations);
        }

        if (rs == null) {
            if (mainPanel != null) {
                mainPanel.appendLog("Stall: No free FP RS for " + op);
            }
            return false;
        }

        rs.issue(instr, registerFile);
        
        InstructionStatus status = new InstructionStatus(instr, rs.getTag());
        status.setIssueCycle(currentCycle);
        instructionStatuses.add(status);

        if (mainPanel != null) {
            mainPanel.appendLog("Issued " + op + " -> " + rs.getTag());
        }
        return true;
    }

    private boolean issueIntInstruction(Instruction instr) {
        ReservationStation rs = null;
        Instruction.Opcode op = instr.getOpcode();
        
        if (isIntMulDiv(op)) {
            rs = findFreeRs(intMulDivStations);
        } else {
            rs = findFreeRs(intAluStations);
        }

        if (rs == null) {
            if (mainPanel != null) {
                mainPanel.appendLog("Stall: No free INT RS for " + op);
            }
            return false;
        }

        rs.issue(instr, registerFile);
        
        InstructionStatus status = new InstructionStatus(instr, rs.getTag());
        status.setIssueCycle(currentCycle);
        instructionStatuses.add(status);

        if (mainPanel != null) {
            mainPanel.appendLog("Issued " + op + " -> " + rs.getTag());
        }
        return true;
    }

    private boolean isIntMulDiv(Instruction.Opcode op) {
        return op == Instruction.Opcode.DMUL || op == Instruction.Opcode.DMULI 
            || op == Instruction.Opcode.DDIV || op == Instruction.Opcode.DDIVU
            || op == Instruction.Opcode.MADD;
    }

    private LoadBuffer findFreeLoadBuffer() {
        for (LoadBuffer lb : loadBuffers) {
            if (!lb.isBusy()) return lb;
        }
        return null;
    }

    private StoreBuffer findFreeStoreBuffer() {
        for (StoreBuffer sb : storeBuffers) {
            if (!sb.isBusy()) return sb;
        }
        return null;
    }

    private ReservationStation findFreeRs(List<ReservationStation> list) {
        for (ReservationStation rs : list) {
            if (rs.isFree()) return rs;
        }
        return null;
    }

    private List<ReservationStation> getAllReservationStations() {
        List<ReservationStation> all = new ArrayList<>();
        all.addAll(fpAddSubStations);
        all.addAll(fpMulDivStations);
        all.addAll(intAluStations);
        all.addAll(intMulDivStations);
        return all;
    }

    private List<FunctionalUnit> getAllFunctionalUnits() {
        List<FunctionalUnit> all = new ArrayList<>();
        all.addAll(fpAddSubUnits);
        all.addAll(fpMulDivUnits);
        all.addAll(intAluUnits);
        all.addAll(intMulDivUnits);
        return all;
    }

    private boolean isSimulationComplete() {
        if (!instructionQueue.isEmpty()) return false;
        
        for (ReservationStation rs : getAllReservationStations()) {
            if (!rs.isFree()) return false;
        }
        for (LoadBuffer lb : loadBuffers) {
            if (lb.isBusy()) return false;
        }
        for (StoreBuffer sb : storeBuffers) {
            if (sb.isBusy()) return false;
        }
        for (FunctionalUnit fu : getAllFunctionalUnits()) {
            if (!fu.isFree()) return false;
        }
        return true;
    }

    // --- Program Loading ---

    public void loadProgramFromFile(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            instructionQueue.clear();
            instructionStatuses.clear();
            currentCycle = 0;
            simulationComplete = false;
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                Instruction instr = parseInstruction(line);
                if (instr != null) {
                    instructionQueue.enqueue(instr);
                }
            }
            
            if (mainPanel != null) {
                mainPanel.appendLog("Loaded " + instructionQueue.size() + " instructions");
            }
        } catch (IOException e) {
            if (mainPanel != null) {
                mainPanel.appendLog("Error loading file: " + e.getMessage());
            }
        }
    }

    public void addInstruction(String instructionText) {
        Instruction instr = parseInstruction(instructionText);
        if (instr != null) {
            instructionQueue.enqueue(instr);
        }
    }

    private Instruction parseInstruction(String text) {
        text = text.toUpperCase().replace(",", " ").replaceAll("\\s+", " ").trim();
        String[] parts = text.split(" ");
        
        if (parts.length < 2) return null;
        
        String opStr = parts[0].replace(".", "_");
        Instruction.Opcode opcode;
        try {
            opcode = Instruction.Opcode.valueOf(opStr);
        } catch (IllegalArgumentException e) {
            if (mainPanel != null) {
                mainPanel.appendLog("Unknown opcode: " + parts[0]);
            }
            return null;
        }

        int destReg = -1, src1Reg = -1, src2Reg = -1, baseReg = -1, offset = 0, immediate = 0;

        try {
            if (opcode.toString().startsWith("L_") || opcode == Instruction.Opcode.LD 
                || opcode == Instruction.Opcode.LW || opcode == Instruction.Opcode.LB
                || opcode == Instruction.Opcode.LBU || opcode == Instruction.Opcode.LH) {
                destReg = parseRegister(parts[1]);
                String memPart = parts[2];
                int parenIdx = memPart.indexOf('(');
                if (parenIdx > 0) {
                    offset = Integer.parseInt(memPart.substring(0, parenIdx));
                    baseReg = parseRegister(memPart.substring(parenIdx + 1, memPart.length() - 1));
                }
            } else if (opcode.toString().startsWith("S_") || opcode == Instruction.Opcode.SD 
                || opcode == Instruction.Opcode.SW || opcode == Instruction.Opcode.SB
                || opcode == Instruction.Opcode.SH) {
                src1Reg = parseRegister(parts[1]);
                String memPart = parts[2];
                int parenIdx = memPart.indexOf('(');
                if (parenIdx > 0) {
                    offset = Integer.parseInt(memPart.substring(0, parenIdx));
                    baseReg = parseRegister(memPart.substring(parenIdx + 1, memPart.length() - 1));
                }
            } else if (opStr.contains("I") && !opStr.contains("DIV")) {
                destReg = parseRegister(parts[1]);
                src1Reg = parseRegister(parts[2]);
                immediate = Integer.parseInt(parts[3]);
            } else {
                destReg = parseRegister(parts[1]);
                src1Reg = parseRegister(parts[2]);
                if (parts.length > 3) {
                    src2Reg = parseRegister(parts[3]);
                }
            }
        } catch (Exception e) {
            if (mainPanel != null) {
                mainPanel.appendLog("Error parsing instruction: " + text + " - " + e.getMessage());
            }
            return null;
        }

        return new Instruction(opcode, destReg, src1Reg, src2Reg, baseReg, offset, immediate);
    }

    private int parseRegister(String regStr) {
        regStr = regStr.trim().toUpperCase();
        if (regStr.startsWith("F")) {
            int num = Integer.parseInt(regStr.substring(1));
            return config.getNumIntRegisters() + num;
        } else if (regStr.startsWith("R")) {
            return Integer.parseInt(regStr.substring(1));
        } else if (regStr.startsWith("$")) {
            return Integer.parseInt(regStr.substring(1));
        }
        return -1;
    }

    // --- Data Access for GUI Tables ---

    public int getCurrentCycle() {
        return currentCycle;
    }

    public List<InstructionQueueRow> getInstructionQueueData() {
        List<InstructionQueueRow> rows = new ArrayList<>();
        int index = 1;
        
        for (InstructionStatus status : instructionStatuses) {
            InstructionQueueRow row = new InstructionQueueRow(index++, formatInstruction(status.getInstruction()));
            
            if (status.getIssueCycle() >= 0) {
                row.setIssueCycle(status.getIssueCycle());
                row.setIssueStatus("Issued");
            }
            if (status.getExecStartCycle() >= 0) {
                row.setExecStartCycle(status.getExecStartCycle());
                row.setIssueStatus("Executing");
            }
            if (status.getExecEndCycle() >= 0) {
                row.setExecEndCycle(status.getExecEndCycle());
                row.setIssueStatus("Exec Done");
            }
            if (status.getWriteBackCycle() >= 0) {
                row.setWriteBackCycle(status.getWriteBackCycle());
                row.setIssueStatus("Written Back");
            }
            
            rows.add(row);
        }
        
        return rows;
    }

    private String formatInstruction(Instruction instr) {
        StringBuilder sb = new StringBuilder();
        sb.append(instr.getOpcode().toString().replace("_", "."));
        
        if (instr.isLoad()) {
            sb.append(" ").append(formatRegister(instr.getDestReg()));
            sb.append(", ").append(instr.getOffset()).append("(").append(formatRegister(instr.getBaseReg())).append(")");
        } else if (instr.isStore()) {
            sb.append(" ").append(formatRegister(instr.getSrc1Reg()));
            sb.append(", ").append(instr.getOffset()).append("(").append(formatRegister(instr.getBaseReg())).append(")");
        } else if (instr.isImmediateType()) {
            sb.append(" ").append(formatRegister(instr.getDestReg()));
            sb.append(", ").append(formatRegister(instr.getSrc1Reg()));
            sb.append(", ").append(instr.getImmediate());
        } else {
            sb.append(" ").append(formatRegister(instr.getDestReg()));
            sb.append(", ").append(formatRegister(instr.getSrc1Reg()));
            if (instr.getSrc2Reg() >= 0) {
                sb.append(", ").append(formatRegister(instr.getSrc2Reg()));
            }
        }
        
        return sb.toString();
    }

    private String formatRegister(int regIndex) {
        if (regIndex < 0) return "-";
        if (regIndex >= config.getNumIntRegisters()) {
            return "F" + (regIndex - config.getNumIntRegisters());
        }
        return "R" + regIndex;
    }

    public List<ReservationStationRow> getFpAddSubRsData() {
        return getRsDataForList(fpAddSubStations);
    }

    public List<ReservationStationRow> getFpMulDivRsData() {
        return getRsDataForList(fpMulDivStations);
    }

    public List<ReservationStationRow> getIntAluRsData() {
        return getRsDataForList(intAluStations);
    }

    public List<ReservationStationRow> getIntMulDivRsData() {
        return getRsDataForList(intMulDivStations);
    }

    private List<ReservationStationRow> getRsDataForList(List<ReservationStation> stations) {
        List<ReservationStationRow> rows = new ArrayList<>();
        for (ReservationStation rs : stations) {
            rows.add(new ReservationStationRow(
                rs.getTag().name(),
                !rs.isFree(),
                rs.isFree() ? "" : rs.getOpcode().toString(),
                rs.isFree() ? "" : String.format("%.2f", rs.getVj()),
                rs.isFree() ? "" : String.format("%.2f", rs.getVk()),
                rs.isFree() ? "" : (rs.getQj() != null ? rs.getQj().name() : ""),
                rs.isFree() ? "" : (rs.getQk() != null ? rs.getQk().name() : ""),
                rs.getState().toString()
            ));
        }
        return rows;
    }

    public List<LoadBufferRow> getLoadBufferData() {
        List<LoadBufferRow> rows = new ArrayList<>();
        for (LoadBuffer lb : loadBuffers) {
            rows.add(new LoadBufferRow(
                lb.getTag().name(),
                lb.isBusy(),
                lb.isBusy() ? String.valueOf(lb.getEffectiveAddress()) : "",
                lb.getState().toString(),
                lb.isBusy() ? formatRegister(lb.getDestRegIndex()) : ""
            ));
        }
        return rows;
    }

    public List<StoreBufferRow> getStoreBufferData() {
        List<StoreBufferRow> rows = new ArrayList<>();
        for (StoreBuffer sb : storeBuffers) {
            rows.add(new StoreBufferRow(
                sb.getTag().name(),
                sb.isBusy(),
                sb.isBusy() ? String.valueOf(sb.getEffectiveAddress()) : "",
                sb.isBusy() ? String.format("%.2f", sb.getValueToStore()) : "",
                sb.getState().toString(),
                sb.isBusy() ? formatRegister(sb.getSrcRegIndex()) : ""
            ));
        }
        return rows;
    }

    public List<RegisterRow> getIntRegisterData() {
        List<RegisterRow> rows = new ArrayList<>();
        for (int i = 0; i < config.getNumIntRegisters(); i++) {
            Register r = registerFile.get(i);
            rows.add(new RegisterRow(
                "R" + i,
                String.valueOf(r.getIntValue()),
                r.getQi().name()
            ));
        }
        return rows;
    }

    public List<RegisterRow> getFpRegisterData() {
        List<RegisterRow> rows = new ArrayList<>();
        for (int i = 0; i < config.getNumFpRegisters(); i++) {
            Register r = registerFile.get(config.getNumIntRegisters() + i);
            rows.add(new RegisterRow(
                "F" + i,
                String.format("%.4f", r.getValue()),
                r.getQi().name()
            ));
        }
        return rows;
    }

    public List<CacheLineRow> getCacheData() {
        List<CacheLineRow> rows = new ArrayList<>();
        int numLines = config.getCacheSizeBytes() / config.getCacheBlockSize();
        for (int i = 0; i < numLines; i++) {
            DataCache.CacheLine line = dataCache.getLine(i);
            if (line != null) {
                rows.add(new CacheLineRow(
                    i, 
                    line.isValid(), 
                    line.isValid() ? String.valueOf(line.getTag()) : "-", 
                    line.isValid() ? "..." : "-", 
                    "-"
                ));
            } else {
                rows.add(new CacheLineRow(i, false, "-", "-", "-"));
            }
        }
        return rows;
    }

    // --- Register/Memory Editing ---

    public void setRegisterValue(int index, double value) {
        if (index >= 0 && index < registerFile.size()) {
            registerFile.get(index).setValue(value);
        }
    }

    public void setMemoryValue(long address, double value) {
        memory.storeDouble(address, value);
    }

    public double getMemoryValue(long address) {
        return memory.loadDouble(address);
    }

    public int getNumIntRegisters() {
        return config.getNumIntRegisters();
    }

    public int getNumFpRegisters() {
        return config.getNumFpRegisters();
    }

    public RegisterFile getRegisterFile() {
        return registerFile;
    }

    // Inner class to track instruction timing
    private static class InstructionStatus {
        private final Instruction instruction;
        private final Tag tag;
        private int issueCycle = -1;
        private int execStartCycle = -1;
        private int execEndCycle = -1;
        private int writeBackCycle = -1;

        public InstructionStatus(Instruction instruction, Tag tag) {
            this.instruction = instruction;
            this.tag = tag;
        }

        public Instruction getInstruction() { return instruction; }
        public Tag getTag() { return tag; }
        public int getIssueCycle() { return issueCycle; }
        public int getExecStartCycle() { return execStartCycle; }
        public int getExecEndCycle() { return execEndCycle; }
        public int getWriteBackCycle() { return writeBackCycle; }

        public void setIssueCycle(int cycle) { this.issueCycle = cycle; }
        public void setExecStartCycle(int cycle) { this.execStartCycle = cycle; }
        public void setExecEndCycle(int cycle) { this.execEndCycle = cycle; }
        public void setWriteBackCycle(int cycle) { this.writeBackCycle = cycle; }
    }
}
