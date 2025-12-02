package core;

import core.Instruction.Opcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Very first end-to-end harness for Tomasulo:
 * - 1 FP functional unit (ADD_D/SUB_D/MUL_D/DIV_D)
 * - 1 INT ALU unit (DADD*, logical, etc.)
 * - A small pool of Reservation Stations for INT + FP
 * - Simple Load/Store handling via LoadBuffer / StoreBuffer
 *
 * This is NOT final microarchitectural behavior, but good enough
 * to start testing your RS + FU + CDB integration on the given test programs.
 */
public class TomasuloSimulator {

    // --- config ---
    private static final int NUM_REGS = 64; // 0..31 -> "R"; 32..63 -> "F"
    private static final int FP_BASE = 32; // F0 == reg[32], F6 == reg[38], etc.

    private static final int NUM_INT_ALU_UNITS = 1;
    private static final int NUM_INT_MULDIV_UNITS = 1;
    private static final int NUM_FP_ADD_SUB_UNITS = 1;
    private static final int NUM_FP_MUL_DIV_UNITS = 1;

    private static final int NUM_FP_RS = 3;
    private static final int NUM_INT_RS = 3;
    private static final int NUM_LOAD_BUFFERS = 2;
    private static final int NUM_STORE_BUFFERS = 2;

    private static final int LOAD_LATENCY = 2; // cycles in memory
    private static final int STORE_LATENCY = 2; // cycles to commit store

    // --- core state ---
    private final InstructionQueue iq = new InstructionQueue();
    private final List<Instruction> program;

    private final RegisterFile registerFile = new RegisterFile(NUM_REGS);
    private final IMemory memory = new MainMemory();

    private final List<ReservationStation> fpStations = new ArrayList<>();
    private final List<ReservationStation> intStations = new ArrayList<>();

    private final List<FunctionalUnit> fpUnits = new ArrayList<>();
    private final List<FunctionalUnit> intUnits = new ArrayList<>();

    private final List<LoadBuffer> loadBuffers = new ArrayList<>();
    private final List<StoreBuffer> storeBuffers = new ArrayList<>();

    private final CommonDataBus cdb = new CommonDataBus();

    private int cycle = 0;
    private long nextTagId = 1;
    private long nextSeqNum = 0; // you can use this later for load/store ordering

    public TomasuloSimulator(List<Instruction> program) {
        this.program = program;
        // load program into IQ
        for (Instruction instr : program) {
            iq.enqueue(instr);
        }
        initStructures();
        initTestState(); // initial register/memory values to make tests meaningful
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
        // FP RS
        for (int i = 0; i < NUM_FP_RS; i++) {
            fpStations.add(new ReservationStation(new Tag("FP" + (i + 1))));
        }
        // INT RS
        for (int i = 0; i < NUM_INT_RS; i++) {
            intStations.add(new ReservationStation(new Tag("I" + (i + 1))));
        }

        // FUs
        // FP FUs: separate ADD/SUB and MUL/DIV
        for (int i = 0; i < NUM_FP_ADD_SUB_UNITS; i++) {
            fpUnits.add(new FunctionalUnit(FunctionalUnit.Type.FP_ADD_SUB));
        }
        for (int i = 0; i < NUM_FP_MUL_DIV_UNITS; i++) {
            fpUnits.add(new FunctionalUnit(FunctionalUnit.Type.FP_MUL_DIV));
        }

        // INT FUs: ALU vs MUL/DIV
        for (int i = 0; i < NUM_INT_ALU_UNITS; i++) {
            intUnits.add(new FunctionalUnit(FunctionalUnit.Type.INT_ALU));
        }

        // Load/Store buffers
        for (int i = 0; i < NUM_LOAD_BUFFERS; i++) {
            loadBuffers.add(new LoadBuffer(new Tag("L" + (i + 1)), LOAD_LATENCY));
        }
        for (int i = 0; i < NUM_STORE_BUFFERS; i++) {
            storeBuffers.add(new StoreBuffer(new Tag("S" + (i + 1)), STORE_LATENCY));
        }
    }

    /**
     * Initialize some register and memory values so the test programs
     * actually compute interesting results instead of all zeros.
     */
    private void initTestState() {
        // R2 base address
        registerFile.get(r(2)).setValue(100); // R2 = 100

        // Test 1 / 2 memory values at 0(R2) and 8(R2), 20(R2) etc.
        memory.storeDouble(100, 1.0); // 0(R2)
        memory.storeDouble(108, 2.0); // 8(R2)
        memory.storeDouble(120, 3.0); // 20(R2)

        // FP registers used in tests
        registerFile.get(f(1)).setValue(10.0);
        registerFile.get(f(2)).setValue(2.0);
        registerFile.get(f(3)).setValue(3.0);
        registerFile.get(f(4)).setValue(4.0);
    }

    private Tag nextTag() {
        return new Tag("T" + (nextTagId++));
    }

    private long nextSeqNum() {
        return nextSeqNum++;
    }

    // --- main simulation loop ---

    public void run(int maxCycles) {
        while (cycle < maxCycles && (!iq.isEmpty() || anyBusy())) {
            cycle++;
            System.out.println("\n========== CYCLE " + cycle + " ==========");

            // 1) Execute FUs + memory and collect finished results
            List<CdbMessage> readyMessages = new ArrayList<>();
            tickFunctionalUnits(readyMessages);
            tickLoadsStores(readyMessages);

            // 2) Broadcast at most one result on CDB (simple policy)
            if (!readyMessages.isEmpty()) {
                CdbMessage chosen = chooseMessageForCdb(readyMessages);
                System.out.println("[CDB] Broadcasting " + chosen);
                broadcastOnCdb(chosen);
                // free the producer structures
                handleProducerFree(chosen.tag());
            }

            // 3) Wake up RS (already done via CDB) and dispatch RS -> FU
            dispatchReadyRsToFus();

            // 4) Issue from IQ
            issueFromQueue();

            // 5) Debug prints
            printState();
        }

        System.out.println("\nSimulation finished at cycle " + cycle);
        printFinalRegisters();
    }

    private boolean anyBusy() {
        return fpStations.stream().anyMatch(rs -> !rs.isFree())
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
            lb.tick(memory);
            if (lb.isCdbReady()) {
                CdbMessage msg = lb.produceCdbMessage(memory);
                if (msg != null) {
                    readyMessages.add(msg);
                }
            }
        }
        // Stores: just advance memory commit
        for (StoreBuffer sb : storeBuffers) {
            sb.tick(memory);
        }
    }

    // --- 2) choose one message to broadcast ---

    private CdbMessage chooseMessageForCdb(List<CdbMessage> readyMessages) {
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
        List<ReservationStation> all = new ArrayList<>(fpStations);
        all.addAll(intStations);
        return all;
    }

    private void handleProducerFree(Tag tag) {
        // If it was produced by an RS: free that RS
        for (ReservationStation rs : allRs()) {
            if (!rs.isFree() && rs.getTag().equals(tag) && rs.isResultReady()) {
                rs.free();
                return;
            }
        }
        // If it was produced by a load buffer: mark it done
        for (LoadBuffer lb : loadBuffers) {
            if (lb.getTag().equals(tag) && lb.isCdbReady()) {
                lb.onCdbWrittenBack(registerFile);
                return;
            }
        }
    }

    // --- 3) Dispatch RS to FUs ---

    private void dispatchReadyRsToFus() {
        // FP RS -> FP FUs
        for (ReservationStation rs : fpStations) {
            if (rs.isWaitingForFu()) {
                FunctionalUnit fu = findFreeFuForOpcode(fpUnits, rs.getOpcode());
                if (fu != null) {
                    System.out.println("[DISPATCH] RS " + rs.getTag() +
                            " ( " + rs.getOpcode() + " ) -> FP FU");
                    fu.start(rs);
                }
            }
        }
        // INT RS -> INT FUs
        for (ReservationStation rs : intStations) {
            if (rs.isWaitingForFu()) {
                FunctionalUnit fu = findFreeFuForOpcode(intUnits, rs.getOpcode());
                if (fu != null) {
                    System.out.println("[DISPATCH] RS " + rs.getTag() +
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
                System.out.println("[ISSUE] Stall: no free LoadBuffer for " + op);
                return;
            }
            Tag tag = nextTag();
            long seq = nextSeqNum();
            System.out.println("[ISSUE] " + op + " -> LoadBuffer " + lb.getTag() +
                    " (tag=" + tag + ", seq=" + seq + ")");
            lb.issue(instr, registerFile, tag, seq);
            // compute EA directly for now (base + offset)
            int base = instr.getBaseReg();
            int offset = instr.getOffset();
            long ea = (long) registerFile.get(base).getIntValue() + offset;
            lb.setEffectiveAddress(ea);
            iq.dequeue();
            return;
        }

        if (instr.isStore()) {
            StoreBuffer sb = findFreeStoreBuffer();
            if (sb == null) {
                System.out.println("[ISSUE] Stall: no free StoreBuffer for " + op);
                return;
            }
            Tag tag = nextTag();
            long seq = nextSeqNum();
            System.out.println("[ISSUE] " + op + " -> StoreBuffer " + sb.getTag() +
                    " (tag=" + tag + ", seq=" + seq + ")");
            sb.issue(instr, registerFile, tag, seq);
            int base = instr.getBaseReg();
            int offset = instr.getOffset();
            long ea = (long) registerFile.get(base).getIntValue() + offset;
            sb.setEffectiveAddress(ea);
            iq.dequeue();
            return;
        }

        if (instr.isFpAddSub() || instr.isFpMulDiv()) {
            ReservationStation rs = findFreeRs(fpStations);
            if (rs == null) {
                System.out.println("[ISSUE] Stall: no free FP RS for " + op);
                return;
            }
            System.out.println("[ISSUE] " + op + " -> RS " + rs.getTag());
            rs.issue(instr, registerFile);
            iq.dequeue();
            return;
        }

        if (instr.isIntArithmetic()) {
            ReservationStation rs = findFreeRs(intStations);
            if (rs == null) {
                System.out.println("[ISSUE] Stall: no free INT RS for " + op);
                return;
            }
            System.out.println("[ISSUE] " + op + " -> RS " + rs.getTag());
            rs.issue(instr, registerFile);
            iq.dequeue();
            return;
        }

        // Branches / jumps: not implemented yet
        if (instr.isBranch()) {
            System.out.println("[ISSUE] Branch not implemented yet, skipping: " + op);
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

        System.out.println("[STATE] FP Reservation Stations:");
        for (ReservationStation rs : fpStations) {
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
    }

    // --- Test program builders for the given examples ---

    public static List<Instruction> buildTestProgram1() {
        List<Instruction> p = new ArrayList<>();
        // L.D F6, 0(R2)
        p.add(new Instruction(Opcode.L_D, f(6), -1, -1, r(2), 0, 0));
        // L.D F2, 8(R2)
        p.add(new Instruction(Opcode.L_D, f(2), -1, -1, r(2), 8, 0));
        // MUL.D F0, F2, F4
        p.add(new Instruction(Opcode.MUL_D, f(0), f(2), f(4), -1, 0, 0));
        // SUB.D F8, F2, F6
        p.add(new Instruction(Opcode.SUB_D, f(8), f(2), f(6), -1, 0, 0));
        // DIV.D F10, F0, F6
        p.add(new Instruction(Opcode.DIV_D, f(10), f(0), f(6), -1, 0, 0));
        // ADD.D F6, F8, F2
        p.add(new Instruction(Opcode.ADD_D, f(6), f(8), f(2), -1, 0, 0));
        // S.D F6, 8(R2)
        p.add(new Instruction(Opcode.SD, -1, f(6), -1, r(2), 8, 0));
        return p;
    }

    public static List<Instruction> buildTestProgram2() {
        List<Instruction> p = new ArrayList<>();
        // L.D F6, 0(R2)
        p.add(new Instruction(Opcode.L_D, f(6), -1, -1, r(2), 0, 0));
        // ADD.D F7, F1, F3
        p.add(new Instruction(Opcode.ADD_D, f(7), f(1), f(3), -1, 0, 0));
        // L.D F2, 20(R2)
        p.add(new Instruction(Opcode.L_D, f(2), -1, -1, r(2), 20, 0));
        // MUL.D F0, F2, F4
        p.add(new Instruction(Opcode.MUL_D, f(0), f(2), f(4), -1, 0, 0));
        // SUB.D F8, F2, F6
        p.add(new Instruction(Opcode.SUB_D, f(8), f(2), f(6), -1, 0, 0));
        // DIV.D F10, F0, F6
        p.add(new Instruction(Opcode.DIV_D, f(10), f(0), f(6), -1, 0, 0));
        // S.D F10, 0(R2)
        p.add(new Instruction(Opcode.SD, -1, f(10), -1, r(2), 0, 0));
        return p;
    }

    // Loop test (Test 3) is not wired yet because branches are not implemented.

    // --- quick main() to run Test 1 and Test 2 ---

    public static void main(String[] args) {
        System.out.println("===== Running Test Program 1 =====");
        TomasuloSimulator sim1 = new TomasuloSimulator(parser.InstructionParser.parseFile("src/parser/test.txt"));
        sim1.run(25);

        // System.out.println("\n\n===== Running Test Program 2 =====");
        // TomasuloSimulator sim2 = new TomasuloSimulator(buildTestProgram2());
        // sim2.run(50);
    }
}
