package com.tomasulo.gui.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.tomasulo.core.Instruction;
import com.tomasulo.core.TomasuloSimulator;
import com.tomasulo.gui.SimulationView;
import com.tomasulo.parser.InstructionParser;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SimulationController {

    private final Stage stage;
    private final TomasuloSimulator.Config config;
    private TomasuloSimulator simulator;
    private SimulationView view;

    public SimulationController(Stage stage, TomasuloSimulator.Config config) {
        this.stage = stage;
        this.config = config;
    }

    public void setView(SimulationView view) {
        this.view = view;
    }

    public TomasuloSimulator getSimulator() {
        return simulator;
    }

    public void loadProgramFile() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            List<Instruction> instructions = InstructionParser.parseFile(file.getAbsolutePath());
            if (instructions.isEmpty()) {
                view.log("Error loading file or file is empty.");
            } else {
                initSimulator(instructions);
            }
        }
    }

    public void loadProgramText(String text) {
        if (text.isEmpty()) return;

        try {
            File temp = File.createTempFile("tomasulo_prog", ".txt");
            java.nio.file.Files.write(temp.toPath(), text.getBytes());
            List<Instruction> instructions = InstructionParser.parseFile(temp.getAbsolutePath());
            if (instructions.isEmpty()) {
                view.log("Error parsing text.");
            } else {
                initSimulator(instructions);
            }
            temp.deleteOnExit();
        } catch (IOException e) {
            view.log("Error creating temp file: " + e.getMessage());
        }
    }

    public void initSimulator(List<Instruction> instructions) {
        simulator = new TomasuloSimulator(instructions, config);
        view.updateView();
        view.log("Program loaded. " + instructions.size() + " instructions.");
    }

    public void step() {
        if (simulator == null) return;
        if (!simulator.isFinished()) {
            simulator.step();
            view.updateView();
            
            // Append all logs from this cycle
            for (String logMsg : simulator.getCycleLog()) {
                view.log(logMsg);
            }
        } else {
            view.log("Simulation Finished.");
        }
    }

    public void runAll() {
        if (simulator == null) return;
        // TODO: Run in a background thread to avoid freezing UI for long simulations
        while (!simulator.isFinished()) {
            simulator.step();
            // Append all logs from this cycle
            for (String logMsg : simulator.getCycleLog()) {
                view.log(logMsg);
            }
        }
        view.updateView();
        view.log("Run complete. Total Cycles: " + simulator.getCycle());
    }

    public void setRegister(String regName, String valueStr) {
        if (simulator == null) {
            view.log("Simulator not initialized. Load a program first.");
            return;
        }
        
        regName = regName.toUpperCase();
        
        try {
            int regIdx = -1;
            if (regName.startsWith("R")) {
                regIdx = Integer.parseInt(regName.substring(1));
                if (regIdx < 0 || regIdx >= 32) {
                    view.log("Invalid integer register index: " + regName);
                    return;
                }
                if (regIdx == 0) {
                    view.log("Error: R0 cannot be modified.");
                    return;
                }
                
                // Validate integer value
                try {
                    int val = Integer.parseInt(valueStr);
                    simulator.getRegisterFile().get(regIdx).setValue(val);
                    view.updateView();
                    view.log("Set " + regName + " to " + val);
                } catch (NumberFormatException e) {
                    view.log("Error: Integer register " + regName + " requires an integer value.");
                }
                
            } else if (regName.startsWith("F")) {
                regIdx = Integer.parseInt(regName.substring(1));
                if (regIdx < 0 || regIdx >= 32) {
                    view.log("Invalid FP register index: " + regName);
                    return;
                }
                
                try {
                    double val = Double.parseDouble(valueStr);
                    simulator.getRegisterFile().get(regIdx + 32).setValue(val);
                    view.updateView();
                    view.log("Set " + regName + " to " + val);
                } catch (NumberFormatException e) {
                    view.log("Error: Invalid number format for " + regName);
                }
            } else {
                view.log("Unknown register type: " + regName);
            }
        } catch (Exception ex) {
            view.log("Invalid input for register set: " + ex.getMessage());
        }
    }

    public void loadRegisterFile() {
        if (simulator == null) {
            view.log("Please load a program first to initialize the simulator.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Register File");
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                for (String line : lines) {
                    parseRegisterLine(line);
                }
                view.updateView();
                view.log("Register file loaded.");
            } catch (IOException e) {
                view.log("Error reading register file: " + e.getMessage());
            }
        }
    }

    private void parseRegisterLine(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) return;

        String[] parts = line.split("\\s+");
        if (parts.length != 2) {
            view.log("Invalid register line format: " + line);
            return;
        }

        String regName = parts[0].toUpperCase();
        String valueStr = parts[1];

        try {
            if (regName.startsWith("R")) {
                int regIdx = Integer.parseInt(regName.substring(1));
                if (regIdx < 0 || regIdx >= 32) {
                     view.log("Invalid integer register index: " + regName);
                     return;
                }
                // Validation: must be integer
                try {
                    int val = Integer.parseInt(valueStr);
                    simulator.getRegisterFile().get(regIdx).setValue(val);
                } catch (NumberFormatException e) {
                    view.log("Error: Integer register " + regName + " requires an integer value. Got: " + valueStr);
                }
            } else if (regName.startsWith("F")) {
                int regIdx = Integer.parseInt(regName.substring(1));
                 if (regIdx < 0 || regIdx >= 32) {
                     view.log("Invalid FP register index: " + regName);
                     return;
                }
                // Validation: can be double
                try {
                    double val = Double.parseDouble(valueStr);
                    // F0 is index 32 in the simulator's flat register file
                    simulator.getRegisterFile().get(regIdx + 32).setValue(val);
                } catch (NumberFormatException e) {
                     view.log("Error: Invalid number format for " + regName + ": " + valueStr);
                }
            } else {
                view.log("Unknown register type: " + regName);
            }
        } catch (Exception e) {
            view.log("Error parsing line: " + line);
        }
    }
}
