package com.tomasulo.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Configuration dialog shown at startup to configure all simulator parameters.
 * Allows the user to set:
 * - Cache parameters (hit latency, miss penalty, block size, cache size)
 * - Instruction latencies for all instruction types
 * - Sizes of reservation stations and buffers
 * - Number of functional units
 */
public class ConfigurationDialog extends Dialog<SimulatorConfig> {

    private final SimulatorConfig config = new SimulatorConfig();

    // Cache configuration spinners
    private Spinner<Integer> cacheHitLatencySpinner;
    private Spinner<Integer> cacheMissPenaltySpinner;
    private Spinner<Integer> cacheBlockSizeSpinner;
    private Spinner<Integer> cacheSizeSpinner;

    // Instruction latency spinners
    private Spinner<Integer> fpAddLatencySpinner;
    private Spinner<Integer> fpSubLatencySpinner;
    private Spinner<Integer> fpMulLatencySpinner;
    private Spinner<Integer> fpDivLatencySpinner;
    private Spinner<Integer> intAddLatencySpinner;
    private Spinner<Integer> intSubLatencySpinner;
    private Spinner<Integer> intMulLatencySpinner;
    private Spinner<Integer> intDivLatencySpinner;
    private Spinner<Integer> loadLatencySpinner;
    private Spinner<Integer> storeLatencySpinner;
    private Spinner<Integer> branchLatencySpinner;

    // Station/buffer size spinners
    private Spinner<Integer> fpAddSubStationsSpinner;
    private Spinner<Integer> fpMulDivStationsSpinner;
    private Spinner<Integer> intAluStationsSpinner;
    private Spinner<Integer> intMulDivStationsSpinner;
    private Spinner<Integer> loadBuffersSpinner;
    private Spinner<Integer> storeBuffersSpinner;

    // Functional unit spinners
    private Spinner<Integer> fpAddSubUnitsSpinner;
    private Spinner<Integer> fpMulDivUnitsSpinner;
    private Spinner<Integer> intAluUnitsSpinner;
    private Spinner<Integer> intMulDivUnitsSpinner;

    public ConfigurationDialog() {
        setTitle("Tomasulo Simulator Configuration");
        setHeaderText("Configure simulator parameters before starting");

        // Create the main content
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
            createCacheTab(),
            createLatencyTab(),
            createStructuresTab()
        );

        getDialogPane().setContent(tabPane);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefWidth(600);
        getDialogPane().setPrefHeight(500);

        // Convert result to SimulatorConfig
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                applyConfiguration();
                return config;
            }
            return null;
        });
    }

    private Tab createCacheTab() {
        Tab tab = new Tab("Cache");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.TOP_LEFT);

        int row = 0;

        // Cache Hit Latency
        Label hitLabel = new Label("Cache Hit Latency (cycles):");
        cacheHitLatencySpinner = createIntSpinner(1, 100, config.getCacheHitLatency());
        grid.add(hitLabel, 0, row);
        grid.add(cacheHitLatencySpinner, 1, row++);

        // Cache Miss Penalty
        Label missLabel = new Label("Cache Miss Penalty (cycles):");
        cacheMissPenaltySpinner = createIntSpinner(1, 1000, config.getCacheMissPenalty());
        grid.add(missLabel, 0, row);
        grid.add(cacheMissPenaltySpinner, 1, row++);

        // Cache Block Size
        Label blockLabel = new Label("Cache Block Size (bytes):");
        cacheBlockSizeSpinner = createIntSpinner(8, 256, config.getCacheBlockSize());
        grid.add(blockLabel, 0, row);
        grid.add(cacheBlockSizeSpinner, 1, row++);

        // Cache Size
        Label sizeLabel = new Label("Cache Size (bytes):");
        cacheSizeSpinner = createIntSpinner(64, 65536, config.getCacheSizeBytes());
        grid.add(sizeLabel, 0, row);
        grid.add(cacheSizeSpinner, 1, row++);

        // Info text
        Label infoLabel = new Label(
            "Note: Cache uses direct mapping. Block size must be power of 2.\n" +
            "Address format: [Tag][Index][Block Offset]\n" +
            "Cache misses are only considered for data, not instructions."
        );
        infoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        infoLabel.setWrapText(true);
        GridPane.setColumnSpan(infoLabel, 2);
        grid.add(infoLabel, 0, row++);

        tab.setContent(grid);
        return tab;
    }

    private Tab createLatencyTab() {
        Tab tab = new Tab("Instruction Latencies");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.TOP_LEFT);

        int row = 0;

        // FP Instructions Section
        Label fpHeader = new Label("Floating-Point Instructions");
        fpHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane.setColumnSpan(fpHeader, 2);
        grid.add(fpHeader, 0, row++);

        grid.add(new Label("ADD.D / ADD.S Latency:"), 0, row);
        fpAddLatencySpinner = createIntSpinner(1, 100, config.getFpAddLatency());
        grid.add(fpAddLatencySpinner, 1, row++);

        grid.add(new Label("SUB.D / SUB.S Latency:"), 0, row);
        fpSubLatencySpinner = createIntSpinner(1, 100, config.getFpSubLatency());
        grid.add(fpSubLatencySpinner, 1, row++);

        grid.add(new Label("MUL.D / MUL.S Latency:"), 0, row);
        fpMulLatencySpinner = createIntSpinner(1, 100, config.getFpMulLatency());
        grid.add(fpMulLatencySpinner, 1, row++);

        grid.add(new Label("DIV.D / DIV.S Latency:"), 0, row);
        fpDivLatencySpinner = createIntSpinner(1, 200, config.getFpDivLatency());
        grid.add(fpDivLatencySpinner, 1, row++);

        // Integer Instructions Section
        Label intHeader = new Label("Integer Instructions");
        intHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane.setColumnSpan(intHeader, 2);
        grid.add(intHeader, 0, row++);

        grid.add(new Label("DADD / DADDI Latency:"), 0, row);
        intAddLatencySpinner = createIntSpinner(1, 100, config.getIntAddLatency());
        grid.add(intAddLatencySpinner, 1, row++);

        grid.add(new Label("DSUB / DSUBI Latency:"), 0, row);
        intSubLatencySpinner = createIntSpinner(1, 100, config.getIntSubLatency());
        grid.add(intSubLatencySpinner, 1, row++);

        grid.add(new Label("DMUL / DMULI Latency:"), 0, row);
        intMulLatencySpinner = createIntSpinner(1, 100, config.getIntMulLatency());
        grid.add(intMulLatencySpinner, 1, row++);

        grid.add(new Label("DDIV Latency:"), 0, row);
        intDivLatencySpinner = createIntSpinner(1, 200, config.getIntDivLatency());
        grid.add(intDivLatencySpinner, 1, row++);

        // Memory Instructions Section
        Label memHeader = new Label("Memory Instructions");
        memHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane.setColumnSpan(memHeader, 2);
        grid.add(memHeader, 0, row++);

        grid.add(new Label("Load Latency (LW, LD, L.D, L.S):"), 0, row);
        loadLatencySpinner = createIntSpinner(1, 100, config.getLoadLatency());
        grid.add(loadLatencySpinner, 1, row++);

        grid.add(new Label("Store Latency (SW, SD, S.D, S.S):"), 0, row);
        storeLatencySpinner = createIntSpinner(1, 100, config.getStoreLatency());
        grid.add(storeLatencySpinner, 1, row++);

        // Branch Section
        Label branchHeader = new Label("Branch Instructions");
        branchHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane.setColumnSpan(branchHeader, 2);
        grid.add(branchHeader, 0, row++);

        grid.add(new Label("Branch Latency (BEQ, BNE, BEQZ, BNEZ):"), 0, row);
        branchLatencySpinner = createIntSpinner(1, 10, config.getBranchLatency());
        grid.add(branchLatencySpinner, 1, row++);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        return tab;
    }

    private Tab createStructuresTab() {
        Tab tab = new Tab("Hardware Structures");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.TOP_LEFT);

        int row = 0;

        // Reservation Stations Section
        Label rsHeader = new Label("Reservation Stations");
        rsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane.setColumnSpan(rsHeader, 2);
        grid.add(rsHeader, 0, row++);

        grid.add(new Label("FP Add/Sub Stations:"), 0, row);
        fpAddSubStationsSpinner = createIntSpinner(1, 10, config.getFpAddSubStations());
        grid.add(fpAddSubStationsSpinner, 1, row++);

        grid.add(new Label("FP Mul/Div Stations:"), 0, row);
        fpMulDivStationsSpinner = createIntSpinner(1, 10, config.getFpMulDivStations());
        grid.add(fpMulDivStationsSpinner, 1, row++);

        grid.add(new Label("Integer ALU Stations:"), 0, row);
        intAluStationsSpinner = createIntSpinner(1, 10, config.getIntAluStations());
        grid.add(intAluStationsSpinner, 1, row++);

        grid.add(new Label("Integer Mul/Div Stations:"), 0, row);
        intMulDivStationsSpinner = createIntSpinner(1, 10, config.getIntMulDivStations());
        grid.add(intMulDivStationsSpinner, 1, row++);

        // Buffers Section
        Label bufferHeader = new Label("Load/Store Buffers");
        bufferHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane.setColumnSpan(bufferHeader, 2);
        grid.add(bufferHeader, 0, row++);

        grid.add(new Label("Load Buffers:"), 0, row);
        loadBuffersSpinner = createIntSpinner(1, 10, config.getLoadBuffers());
        grid.add(loadBuffersSpinner, 1, row++);

        grid.add(new Label("Store Buffers:"), 0, row);
        storeBuffersSpinner = createIntSpinner(1, 10, config.getStoreBuffers());
        grid.add(storeBuffersSpinner, 1, row++);

        // Functional Units Section
        Label fuHeader = new Label("Functional Units");
        fuHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        GridPane.setColumnSpan(fuHeader, 2);
        grid.add(fuHeader, 0, row++);

        grid.add(new Label("FP Add/Sub Units:"), 0, row);
        fpAddSubUnitsSpinner = createIntSpinner(1, 5, config.getFpAddSubUnits());
        grid.add(fpAddSubUnitsSpinner, 1, row++);

        grid.add(new Label("FP Mul/Div Units:"), 0, row);
        fpMulDivUnitsSpinner = createIntSpinner(1, 5, config.getFpMulDivUnits());
        grid.add(fpMulDivUnitsSpinner, 1, row++);

        grid.add(new Label("Integer ALU Units:"), 0, row);
        intAluUnitsSpinner = createIntSpinner(1, 5, config.getIntAluUnits());
        grid.add(intAluUnitsSpinner, 1, row++);

        grid.add(new Label("Integer Mul/Div Units:"), 0, row);
        intMulDivUnitsSpinner = createIntSpinner(1, 5, config.getIntMulDivUnits());
        grid.add(intMulDivUnitsSpinner, 1, row++);

        tab.setContent(grid);
        return tab;
    }

    private Spinner<Integer> createIntSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);
        spinner.setPrefWidth(100);
        
        // Make the spinner commit on focus lost
        spinner.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    String text = spinner.getEditor().getText();
                    int value = Integer.parseInt(text);
                    spinner.getValueFactory().setValue(value);
                } catch (NumberFormatException e) {
                    spinner.getEditor().setText(String.valueOf(spinner.getValue()));
                }
            }
        });
        
        return spinner;
    }

    private void applyConfiguration() {
        // Cache configuration
        config.setCacheHitLatency(cacheHitLatencySpinner.getValue());
        config.setCacheMissPenalty(cacheMissPenaltySpinner.getValue());
        config.setCacheBlockSize(cacheBlockSizeSpinner.getValue());
        config.setCacheSizeBytes(cacheSizeSpinner.getValue());

        // Instruction latencies
        config.setFpAddLatency(fpAddLatencySpinner.getValue());
        config.setFpSubLatency(fpSubLatencySpinner.getValue());
        config.setFpMulLatency(fpMulLatencySpinner.getValue());
        config.setFpDivLatency(fpDivLatencySpinner.getValue());
        config.setIntAddLatency(intAddLatencySpinner.getValue());
        config.setIntSubLatency(intSubLatencySpinner.getValue());
        config.setIntMulLatency(intMulLatencySpinner.getValue());
        config.setIntDivLatency(intDivLatencySpinner.getValue());
        config.setLoadLatency(loadLatencySpinner.getValue());
        config.setStoreLatency(storeLatencySpinner.getValue());
        config.setBranchLatency(branchLatencySpinner.getValue());

        // Station/buffer sizes
        config.setFpAddSubStations(fpAddSubStationsSpinner.getValue());
        config.setFpMulDivStations(fpMulDivStationsSpinner.getValue());
        config.setIntAluStations(intAluStationsSpinner.getValue());
        config.setIntMulDivStations(intMulDivStationsSpinner.getValue());
        config.setLoadBuffers(loadBuffersSpinner.getValue());
        config.setStoreBuffers(storeBuffersSpinner.getValue());

        // Functional units
        config.setFpAddSubUnits(fpAddSubUnitsSpinner.getValue());
        config.setFpMulDivUnits(fpMulDivUnitsSpinner.getValue());
        config.setIntAluUnits(intAluUnitsSpinner.getValue());
        config.setIntMulDivUnits(intMulDivUnitsSpinner.getValue());
    }
}
