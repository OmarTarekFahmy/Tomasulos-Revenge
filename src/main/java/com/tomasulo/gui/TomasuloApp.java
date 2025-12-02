package com.tomasulo.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX Application for Tomasulo Algorithm Simulator.
 * 
 * This GUI allows users to:
 * - Load MIPS assembly instructions from file or construct them graphically
 * - Configure cache parameters (hit latency, miss penalty, block size, cache size)
 * - Configure instruction latencies
 * - Configure reservation station and buffer sizes
 * - Pre-load register values
 * - Step through cycle-by-cycle simulation
 * - View the state of all components (RS, buffers, registers, cache, queue)
 */
public class TomasuloApp extends Application {

    private SimulatorController controller;
    private MainSimulatorPanel mainPanel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tomasulo Algorithm Simulator");

        // Show configuration dialog first
        ConfigurationDialog configDialog = new ConfigurationDialog();
        configDialog.showAndWait().ifPresent(config -> {
            controller = new SimulatorController(config);
            mainPanel = new MainSimulatorPanel(controller);

            Scene scene = new Scene(mainPanel, 1400, 900);
            
            // Load CSS if available
            try {
                String css = getClass().getResource("/styles.css") != null 
                    ? getClass().getResource("/styles.css").toExternalForm() 
                    : "";
                if (!css.isEmpty()) {
                    scene.getStylesheets().add(css);
                }
            } catch (Exception e) {
                // CSS not found, continue without styles
            }

            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
