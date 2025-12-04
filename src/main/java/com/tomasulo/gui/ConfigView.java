package com.tomasulo.gui;

import com.tomasulo.core.TomasuloSimulator;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ConfigView extends VBox {

    private final Stage stage;

    // Latencies final
    private final TextField intAluLatency = new TextField("1");
    private final TextField fpAddSubLatency = new TextField("3");
    private final TextField fpMulLatency = new TextField("5");
    private final TextField fpDivLatency = new TextField("12");
    // Counts
    private final TextField numIntRs = new TextField("3");
    private final TextField numFpAddRs = new TextField("3");
    private final TextField numFpMulRs = new TextField("3");
    private final TextField numLoadBuffers = new TextField("3");
    private final TextField numStoreBuffers = new TextField("3");

    // Cache
    private final TextField cacheSize = new TextField("1024");
    private final TextField blockSize = new TextField("64");
    private final TextField cacheHitLatency = new TextField("1");
    private final TextField cacheMissPenalty = new TextField("10");

    public ConfigView(Stage stage) {
        this.stage = stage;
        setPadding(new Insets(20));
        setSpacing(20);

        Label title = new Label("Configuration");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;
        
        // Latencies
        addSection(grid, "Latencies", row++);
        addInput(grid, "Integer ALU Latency:", intAluLatency, row++);
        addInput(grid, "FP Add/Sub Latency:", fpAddSubLatency, row++);
        addInput(grid, "FP Mul Latency:", fpMulLatency, row++);
        addInput(grid, "FP Div Latency:", fpDivLatency, row++);

        // Buffer Sizes
        addSection(grid, "Buffer Sizes", row++);
        addInput(grid, "Integer RS Count:", numIntRs, row++);
        addInput(grid, "FP Add/Sub RS Count:", numFpAddRs, row++);
        addInput(grid, "FP Mul/Div RS Count:", numFpMulRs, row++);
        addInput(grid, "Load Buffers:", numLoadBuffers, row++);
        addInput(grid, "Store Buffers:", numStoreBuffers, row++);

        // Cache
        addSection(grid, "Cache Configuration", row++);
        addInput(grid, "Cache Size (bytes):", cacheSize, row++);
        addInput(grid, "Block Size (bytes):", blockSize, row++);
        addInput(grid, "Hit Latency:", cacheHitLatency, row++);
        addInput(grid, "Miss Penalty:", cacheMissPenalty, row++);

        Button startButton = new Button("Start Simulation");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(e -> startSimulation());

        getChildren().addAll(title, grid, startButton);
    }

    private void addSection(GridPane grid, String title, int row) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold; -fx-underline: true;");
        grid.add(label, 0, row, 2, 1);
    }

    private void addInput(GridPane grid, String label, TextField field, int row) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }

    private void startSimulation() {
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

            SimulationView simView = new SimulationView(stage, config);
            Scene scene = new Scene(simView, 1200, 800);
            stage.setTitle("Tomasulo Simulator - Running");
            stage.setScene(scene);

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter valid integers for all fields.");
            alert.showAndWait();
        }
    }
}
