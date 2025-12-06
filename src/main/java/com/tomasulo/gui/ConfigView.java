package com.tomasulo.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.tomasulo.gui.controller.ConfigController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class ConfigView extends BorderPane {

    private final ConfigController controller;

    // Latencies final
    private final TextField intAluLatency = new TextField("1");
    private final TextField fpAddSubLatency = new TextField("3");
    private final TextField fpMulLatency = new TextField("5");
    private final TextField fpDivLatency = new TextField("12");
    // Counts
    private final TextField numIntRs = new TextField("3");
    private final TextField numFpAddRs = new TextField("3");
    private final TextField numFpMulRs = new TextField("3");
    private final TextField numLoadBuffers = new TextField("2");
    private final TextField numStoreBuffers = new TextField("2");

    // Cache
    private final TextField cacheSize = new TextField("1024");
    private final TextField blockSize = new TextField("8");
    private final TextField cacheHitLatency = new TextField("1");
    private final TextField cacheMissPenalty = new TextField("10");

    // Memory
    private final TextField memorySize = new TextField("1024");

    public ConfigView(ConfigController controller) {
        this.controller = controller;
        setPadding(new Insets(20));

        Label title = new Label("Configuration");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        VBox topBox = new VBox(title);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(0, 0, 20, 0));
        setTop(topBox);

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

        // Memory
        addSection(grid, "Memory Configuration", row++);
        addInput(grid, "Memory Size (bytes):", memorySize, row++);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color:transparent;");
        setCenter(scrollPane);

        Button loadConfigButton = new Button("Load Configuration");
        loadConfigButton.setMaxWidth(Double.MAX_VALUE);
        loadConfigButton.setOnAction(e -> loadConfiguration());

        Button startButton = new Button("Start Simulation");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(e -> controller.startSimulation(
            intAluLatency, fpAddSubLatency, fpMulLatency, fpDivLatency,
            numIntRs, numFpAddRs, numFpMulRs, numLoadBuffers, numStoreBuffers,
            cacheSize, blockSize, cacheHitLatency, cacheMissPenalty, memorySize
        ));

        VBox bottomBox = new VBox(10, loadConfigButton, startButton);
        bottomBox.setPadding(new Insets(20, 0, 0, 0));
        setBottom(bottomBox);
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

    private void loadConfiguration() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Configuration File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File file = fileChooser.showOpenDialog(getScene().getWindow());

        if (file != null) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        updateField(key, value);
                    }
                }
            } catch (IOException e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Could not load configuration");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void updateField(String key, String value) {
        switch (key) {
            case "intAluLatency": intAluLatency.setText(value); break;
            case "fpAddSubLatency": fpAddSubLatency.setText(value); break;
            case "fpMulLatency": fpMulLatency.setText(value); break;
            case "fpDivLatency": fpDivLatency.setText(value); break;
            case "numIntRs": numIntRs.setText(value); break;
            case "numFpAddRs": numFpAddRs.setText(value); break;
            case "numFpMulRs": numFpMulRs.setText(value); break;
            case "numLoadBuffers": numLoadBuffers.setText(value); break;
            case "numStoreBuffers": numStoreBuffers.setText(value); break;
            case "cacheSize": cacheSize.setText(value); break;
            case "blockSize": blockSize.setText(value); break;
            case "cacheHitLatency": cacheHitLatency.setText(value); break;
            case "cacheMissPenalty": cacheMissPenalty.setText(value); break;
            case "memorySize": memorySize.setText(value); break;
        }
    }
}

