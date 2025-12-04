# Tomasulo's Revenge - Dynamic Instruction Scheduler Simulator

## Project Overview
This project is a Java-based simulator for **Tomasulo's Algorithm**, a dynamic scheduling algorithm that allows out-of-order execution of instructions. The simulator includes a graphical user interface (GUI) to visualize the internal state of the processor cycle-by-cycle, including Reservation Stations, Load/Store Buffers, Register File, and Cache.

## Features
*   **Configurable Architecture:**
    *   Set number of Reservation Stations (Integer, FP Add/Sub, FP Mul/Div).
    *   Set number of Load/Store Buffers.
    *   Define latencies for different functional units.
    *   Configure Cache size, block size, hit latency, and miss penalty.
*   **Instruction Support:**
    *   Supports a subset of MIPS assembly instructions.
    *   Handles Integer and Floating-Point operations.
    *   Loads and Stores with offset addressing.
*   **Memory System:**
    *   Simulates a **Direct-Mapped Cache** with Write-Back and Write-Allocate policies.
    *   Visualizes cache hits, misses, and dirty blocks.
*   **Visualization (GUI):**
    *   **Configuration Screen:** Easy setup of simulation parameters.
    *   **Simulation View:**
        *   **Instruction Queue:** View pending instructions.
        *   **Reservation Stations:** Monitor `Busy`, `Op`, `Vj`, `Vk`, `Qj`, `Qk` status.
        *   **Registers:** Real-time view of Register File (R0-R31, F0-F31) and their dependency tags (`Qi`).
        *   **CDB Arbitration:** Logs which instruction broadcasts on the Common Data Bus when conflicts occur.
    *   **Controls:** Step-by-step execution, Run All, and dynamic Register editing.

## Project Status

### ✅ Completed Modules
1.  **Parser (`com.tomasulo.parser`)**
    *   `InstructionParser`: Fully functional. Parses text files and strings into `Instruction` objects.
2.  **Core (`com.tomasulo.core`)**
    *   **Main Loop:** `TomasuloSimulator` correctly handles Issue, Execute, and Write Result stages.
    *   **Reservation Stations:** Logic for operand tracking and renaming is implemented.
    *   **Common Data Bus (CDB):** Broadcasting and tag matching works.
    *   **Functional Units:** Latency simulation for ALU, FP Add, FP Mul, FP Div.
    *   **Register File:** 64 registers (32 Int, 32 FP) with Qi tag tracking.
3.  **Memory (`com.tomasulo.core`)**
    *   **Cache:** Fully implemented Direct-Mapped cache with hit/miss logic.
    *   **Integration:** Load/Store buffers interact correctly with the cache.
4.  **GUI (`com.tomasulo.gui`)**
    *   **ConfigView:** User input for all architectural parameters.
    *   **SimulationView:** Complete visualization tables and controls.

### 🚧 Work in Progress / To Do
1.  **Branch Handling:**
    *   Currently, branch instructions are parsed but skipped during execution.
    *   *Next Step:* Implement `BranchHandler` with prediction logic (e.g., predict not taken) and flushing mechanism on misprediction.
2.  **Configuration Loading:**
    *   `ConfigLoader` is currently a placeholder.
    *   *Next Step:* Implement JSON/Text file loading for configurations to avoid manual entry every time.
3.  **Address Calculation Timing:**
    *   Effective Address (EA) is currently calculated at the **Issue** stage.
    *   *Refinement:* Move EA calculation to the execution stage of Load/Store buffers to strictly adhere to the algorithm (waiting for base register).

## How to Run
**Prerequisites:** Java 17+, Maven.

1.  **Build the project:**
    ```bash
    mvn clean package
    ```
2.  **Run the GUI:**
    ```bash
    mvn javafx:run
    ```

## Project Structure
```
src/main/java/com/tomasulo/
├── core/           # Core logic (Simulator, RS, Registers, Memory)
├── gui/            # JavaFX User Interface
└── parser/         # Instruction parsing logic
```
