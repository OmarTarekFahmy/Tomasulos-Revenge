# Tomasulo Algorithm Simulator

A JavaFX-based GUI simulator for the Tomasulo algorithm, designed to help understand dynamic instruction scheduling in modern processors.

## Features

- **Interactive GUI**: Step through cycle-by-cycle simulation with visual feedback
- **Configurable Hardware**: Set sizes for reservation stations, load/store buffers, and functional units
- **Instruction Support**:
  - FP operations: ADD.D, SUB.D, MUL.D, DIV.D, ADD.S, SUB.S, MUL.S, DIV.S
  - Integer operations: DADD, DADDI, DSUB, DSUBI, DMUL, DDIV, OR, XOR, SLT, shifts
  - Memory operations: L.D, L.S, LD, LW, S.D, S.S, SD, SW
  - Branches: BEQ, BNE, BEQZ, BNEZ (no prediction)
- **Cache Simulation**: Configurable direct-mapped data cache with hit/miss latency
- **Hazard Handling**: Automatically handles RAW, WAR, and WAW hazards
- **Register Pre-loading**: Initialize register values before simulation
- **Memory Editing**: View and modify memory contents
- **Instruction Input**: Load from file or construct graphically

## Requirements

- Java 17 or later
- Maven 3.6+ (recommended)

## Project Structure

```
Tomasulos-Revenge/
├── pom.xml                          # Maven configuration
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java     # Java module descriptor
│       │   └── com/tomasulo/
│       │       ├── core/            # Simulator core classes
│       │       │   ├── Instruction.java
│       │       │   ├── ReservationStation.java
│       │       │   ├── LoadBuffer.java
│       │       │   ├── StoreBuffer.java
│       │       │   ├── FunctionalUnit.java
│       │       │   ├── RegisterFile.java
│       │       │   ├── CommonDataBus.java
│       │       │   └── ...
│       │       ├── gui/             # JavaFX GUI classes
│       │       │   ├── TomasuloApp.java
│       │       │   ├── MainSimulatorPanel.java
│       │       │   ├── SimulatorController.java
│       │       │   ├── ConfigurationDialog.java
│       │       │   └── ...
│       │       └── parser/          # Instruction parsing
│       │           ├── InstructionParser.java
│       │           └── ProgramLoader.java
│       └── resources/
│           └── styles.css           # CSS styling
└── README.md
```

## Building and Running

### Using Maven (Recommended)

1. **Compile the project:**
   ```bash
   mvn compile
   ```

2. **Run the application:**
   ```bash
   mvn javafx:run
   ```

3. **Build a packaged JAR:**
   ```bash
   mvn package
   ```

### Using VS Code Tasks

The project includes VS Code tasks for convenience:
- `mvn-compile`: Compile the project
- `mvn-run`: Run the JavaFX application
- `mvn-package`: Create a JAR package
- `mvn-clean`: Clean build artifacts

Using VS Code tasks:
- Press `Ctrl+Shift+B` to run the default build task

Or manually:
```bash
javac -d out --module-path "%JAVAFX_HOME%/lib" --add-modules javafx.controls,javafx.fxml src/core/*.java src/parser/*.java src/gui/*.java
```

### 4. Run

Using VS Code tasks:
- Run the "run-simulator" task

Or manually:
```bash
java --module-path "%JAVAFX_HOME%/lib" --add-modules javafx.controls,javafx.fxml -cp out gui.TomasuloApp
```

## Usage

### Configuration Dialog

When the application starts, you'll see a configuration dialog with three tabs:

1. **Cache**: Set cache hit latency, miss penalty, block size, and total size
2. **Instruction Latencies**: Configure execution latency for each instruction type
3. **Hardware Structures**: Set sizes for reservation stations, buffers, and functional units

### Main Interface

The main window displays:

- **Instruction Queue & Timing**: Shows all instructions with Issue/Execute/WriteBack cycle numbers
- **Reservation Stations**: FP Add/Sub, FP Mul/Div, Integer ALU, and Integer Mul/Div stations
- **Load/Store Buffers**: Current state of memory operation buffers
- **Register Files**: Integer (R0-R31) and FP (F0-F31) registers with values and Qi tags
- **Data Cache**: Cache line status with valid bits, tags, and data

### Controls

- **Step (1 Cycle)**: Advance simulation by one clock cycle
- **Run to Completion**: Run until all instructions complete (max 1000 cycles)
- **Reset**: Reset simulation to initial state
- **Load Program**: Load MIPS assembly from a text file
- **Add Instruction**: Graphically construct and add a single instruction
- **Edit Registers**: Pre-load register values
- **Edit Memory**: View and modify memory contents

### Sample Program File

Create a `.asm` file with MIPS instructions (one per line):

```asm
# Sample Tomasulo test program
L.D F6, 0(R2)
L.D F2, 8(R2)
MUL.D F0, F2, F4
SUB.D F8, F2, F6
DIV.D F10, F0, F6
ADD.D F6, F8, F2
S.D F6, 8(R2)
```

## Architecture

### CDB Conflict Resolution

When multiple instructions complete in the same cycle, the Common Data Bus (CDB) uses priority-based arbitration:
1. Load operations (highest priority - to feed dependent instructions faster)
2. FP operations
3. Integer operations (lowest priority)

Within the same priority level, FIFO ordering is used.

### Cache Implementation

The data cache uses **direct mapping** with a write-through policy:
- Address format: `[Tag][Index][Block Offset]`
- Block offset bits = log2(blockSize)
- Index bits = log2(numCacheLines)
- Cache misses are only considered for data accesses, not instructions

### Address Clash Handling

Load/Store operations check for address conflicts:
- Stores wait for all prior stores to the same address
- Loads wait for prior stores to the same address to complete

## Project Structure

```
src/
├── core/           # Core Tomasulo algorithm implementation
│   ├── Instruction.java
│   ├── ReservationStation.java
│   ├── LoadBuffer.java
│   ├── StoreBuffer.java
│   ├── FunctionalUnit.java
│   ├── RegisterFile.java
│   ├── CommonDataBus.java
│   └── ...
├── gui/            # JavaFX GUI components
│   ├── TomasuloApp.java          # Main application
│   ├── ConfigurationDialog.java   # Startup configuration
│   ├── MainSimulatorPanel.java    # Main simulation view
│   ├── SimulatorController.java   # Simulation logic controller
│   ├── AddInstructionDialog.java  # Instruction builder
│   └── ...
└── parser/         # Instruction parsing utilities
```

## License

MIT License
