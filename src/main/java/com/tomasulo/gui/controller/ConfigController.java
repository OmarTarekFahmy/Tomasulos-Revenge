package com.tomasulo.gui.controller;

import java.util.ArrayList;

import com.tomasulo.core.TomasuloSimulator;
import com.tomasulo.gui.SimulationView;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ConfigController {

    private final Stage stage;

    public ConfigController(Stage stage) {
        this.stage = stage;
    }

    public void startSimulation(
            TextField intAluLatency, TextField fpAddSubLatency, TextField fpMulLatency, TextField fpDivLatency,
            TextField numIntRs, TextField numFpAddRs, TextField numFpMulRs, TextField numLoadBuffers,
            TextField numStoreBuffers,
            TextField cacheSize, TextField blockSize, TextField cacheHitLatency, TextField cacheMissPenalty) {
        try {
            TomasuloSimulator.Config config = new TomasuloSimulator.Config();

            config.intAluLatency = Integer.parseInt(intAluLatency.getText());
            config.fpAddSubLatency = Integer.parseInt(fpAddSubLatency.getText());
            config.fpMulLatency = Integer.parseInt(fpMulLatency.getText());
            config.fpDivLatency = Integer.parseInt(fpDivLatency.getText());

            config.numIntRs = Integer.parseInt(numIntRs.getText());
            config.numFpAddSubRs = Integer.parseInt(numFpAddRs.getText());
            config.numFpMulDivRs = Integer.parseInt(numFpMulRs.getText());
            config.numLoadBuffers = Integer.parseInt(numLoadBuffers.getText());
            config.numStoreBuffers = Integer.parseInt(numStoreBuffers.getText());

            config.cacheSize = Integer.parseInt(cacheSize.getText());
            config.blockSize = Integer.parseInt(blockSize.getText());
            config.cacheHitLatency = Integer.parseInt(cacheHitLatency.getText());
            config.cacheMissPenalty = Integer.parseInt(cacheMissPenalty.getText());

            config.memorySize = 1024; // 1KB memory

            if (config.numIntRs < 1 || config.numFpAddSubRs < 1 || config.numFpMulDivRs < 1 ||
                    config.numLoadBuffers < 1 || config.numStoreBuffers < 1) {
                throw new NumberFormatException("Counts must be positive");
            }

            // TODO: Validate configuration values (e.g., cache size power of 2)

            SimulationController simController = new SimulationController(stage, config);
            SimulationView simView = new SimulationView(simController);
            simController.setView(simView); // Link view back to controller if needed, or just pass controller to view

            // Initialize with empty program to show structure
            simController.initSimulator(new ArrayList<>());

            Scene scene = new Scene(simView, 1200, 800);
            stage.setTitle("Tomasulo Simulator - Running");
            stage.setScene(scene);
            stage.setMaximized(true);

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter valid integers for all fields.");
            alert.showAndWait();
        }
    }
}
