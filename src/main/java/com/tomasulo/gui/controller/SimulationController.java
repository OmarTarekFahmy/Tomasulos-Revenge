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

    private void initSimulator(List<Instruction> instructions) {
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

    public void setRegister(String regName, double val) {
        if (simulator == null) return;
        try {
            int regIdx = -1;
            if (regName.startsWith("R")) {
                regIdx = Integer.parseInt(regName.substring(1));
            } else if (regName.startsWith("F")) {
                regIdx = Integer.parseInt(regName.substring(1)) + 32;
            }

            if (regIdx >= 0 && regIdx < 64) {
                simulator.getRegisterFile().get(regIdx).setValue(val);
                view.updateView();
                view.log("Set " + regName + " to " + val);
            }
        } catch (Exception ex) {
            view.log("Invalid input for register set.");
        }
    }
}
