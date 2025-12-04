package com.tomasulo.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.tomasulo.core.CacheBlock;
import com.tomasulo.core.Instruction;
import com.tomasulo.core.LoadBuffer;
import com.tomasulo.core.Register;
import com.tomasulo.core.RegisterFile;
import com.tomasulo.core.ReservationStation;
import com.tomasulo.core.StoreBuffer;
import com.tomasulo.core.TomasuloSimulator;
import com.tomasulo.parser.InstructionParser;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SimulationView extends BorderPane {

    private final Stage stage;
    private final TomasuloSimulator.Config config;
    private TomasuloSimulator simulator;

    private Label cycleLabel;
    private TextArea logArea;
    private TextArea programInput;
    
    // Tables
    private TableView<ReservationStation> fpAddTable;
    private TableView<ReservationStation> fpMulTable;
    private TableView<ReservationStation> intTable;
    private TableView<LoadBuffer> loadTable;
    private TableView<StoreBuffer> storeTable;
    private TableView<RegisterWrapper> registerTable;
    private TableView<CacheBlock> cacheTable;
    private TableView<Instruction> instructionQueueTable;


    public SimulationView(Stage stage, TomasuloSimulator.Config config) {
        this.stage = stage;
        this.config = config;

        setupUI();
    }

    private void setupUI() {
        // Top Control Bar
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        
        Button loadBtn = new Button("Load Program File");
        loadBtn.setOnAction(e -> loadProgramFile());

        Button loadTextBtn = new Button("Load from Text Area");
        loadTextBtn.setOnAction(e -> loadProgramText());

        Button stepBtn = new Button("Step");
        stepBtn.setOnAction(e -> step());

        Button runBtn = new Button("Run All");
        runBtn.setOnAction(e -> runAll());

        cycleLabel = new Label("Cycle: 0");
        cycleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        topBar.getChildren().addAll(loadBtn, loadTextBtn, stepBtn, runBtn, cycleLabel);
        setTop(topBar);

        // Left Side: Program Input & Instruction Queue
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(10));
        leftPane.setPrefWidth(300);

        programInput = new TextArea();
        programInput.setPromptText("Enter Assembly Code Here...");
        programInput.setPrefHeight(200);

        instructionQueueTable = new TableView<>();
        TableColumn<Instruction, String> iqOpCol = new TableColumn<>("Opcode");
        iqOpCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOpcode().toString()));
        TableColumn<Instruction, String> iqDestCol = new TableColumn<>("Dest");
        iqDestCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDestReg())));
        instructionQueueTable.getColumns().addAll(iqOpCol, iqDestCol);
        instructionQueueTable.setPlaceholder(new Label("Instruction Queue Empty"));

        leftPane.getChildren().addAll(new Label("Program Input:"), programInput, new Label("Instruction Queue:"), instructionQueueTable);
        setLeft(leftPane);

        // Center: Reservation Stations & Buffers
        VBox centerPane = new VBox(10);
        centerPane.setPadding(new Insets(10));
        
        fpAddTable = createRsTable("FP Add/Sub Stations");
        fpMulTable = createRsTable("FP Mul/Div Stations");
        intTable = createRsTable("Integer Stations");
        loadTable = createLoadTable();
        storeTable = createStoreTable();

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("FP Add/Sub", fpAddTable));
        tabPane.getTabs().add(new Tab("FP Mul/Div", fpMulTable));
        tabPane.getTabs().add(new Tab("Integer", intTable));
        tabPane.getTabs().add(new Tab("Load Buffers", loadTable));
        tabPane.getTabs().add(new Tab("Store Buffers", storeTable));
        
        for(Tab t : tabPane.getTabs()) t.setClosable(false);

        centerPane.getChildren().add(tabPane);
        setCenter(centerPane);

        // Right: Registers & Memory
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(10));
        rightPane.setPrefWidth(300);

        registerTable = new TableView<>();
        TableColumn<RegisterWrapper, String> regNameCol = new TableColumn<>("Reg");
        regNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
        TableColumn<RegisterWrapper, String> regValCol = new TableColumn<>("Value");
        regValCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().register.getValue())));
        TableColumn<RegisterWrapper, String> regQiCol = new TableColumn<>("Qi");
        regQiCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().register.getQi().toString()));
        
        registerTable.getColumns().addAll(regNameCol, regQiCol, regValCol);
        
        // Cache Table
        cacheTable = new TableView<>();
        TableColumn<CacheBlock, String> cacheTagCol = new TableColumn<>("Tag");
        cacheTagCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTag())));
        TableColumn<CacheBlock, String> cacheDataCol = new TableColumn<>("Data");
        cacheDataCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataHex()));
        TableColumn<CacheBlock, String> cacheDirtyCol = new TableColumn<>("Dirty");
        cacheDirtyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().isDirty())));
        
        cacheTable.getColumns().addAll(cacheTagCol, cacheDataCol, cacheDirtyCol);
        cacheTable.setPlaceholder(new Label("Cache Empty / Invalid"));

        rightPane.getChildren().addAll(new Label("Register File"), registerTable, createSetRegisterButton(), new Label("Cache Content"), cacheTable);
        setRight(rightPane);


        // Bottom: Log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        setBottom(logArea);
    }

    private Button createSetRegisterButton() {
        Button btn = new Button("Set Register Value");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("R0 100");
            dialog.setTitle("Set Register");
            dialog.setHeaderText("Enter Register and Value (e.g., R1 50 or F2 3.14)");
            dialog.showAndWait().ifPresent(result -> {
                String[] parts = result.split(" ");
                if (parts.length == 2) {
                    try {
                        String regName = parts[0];
                        double val = Double.parseDouble(parts[1]);
                        int regIdx = -1;
                        if (regName.startsWith("R")) {
                            regIdx = Integer.parseInt(regName.substring(1));
                        } else if (regName.startsWith("F")) {
                            regIdx = Integer.parseInt(regName.substring(1)) + 32;
                        }
                        
                        if (regIdx >= 0 && regIdx < 64 && simulator != null) {
                            simulator.getRegisterFile().get(regIdx).setValue(val);
                            updateView();
                            log("Set " + regName + " to " + val);
                        }
                    } catch (Exception ex) {
                        log("Invalid input for register set.");
                    }
                }
            });
        });
        return btn;
    }


    private TableView<ReservationStation> createRsTable(String title) {
        TableView<ReservationStation> table = new TableView<>();
        
        TableColumn<ReservationStation, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTag().toString()));

        TableColumn<ReservationStation, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().isBusy())));

        TableColumn<ReservationStation, String> opCol = new TableColumn<>("Op");
        opCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOpcode() != null ? c.getValue().getOpcode().toString() : ""));

        TableColumn<ReservationStation, String> vjCol = new TableColumn<>("Vj");
        vjCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getVj())));

        TableColumn<ReservationStation, String> vkCol = new TableColumn<>("Vk");
        vkCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getVk())));

        TableColumn<ReservationStation, String> qjCol = new TableColumn<>("Qj");
        qjCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getQj().toString()));

        TableColumn<ReservationStation, String> qkCol = new TableColumn<>("Qk");
        qkCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getQk().toString()));

        table.getColumns().addAll(tagCol, busyCol, opCol, vjCol, vkCol, qjCol, qkCol);
        return table;
    }

    private TableView<LoadBuffer> createLoadTable() {
        TableView<LoadBuffer> table = new TableView<>();
        TableColumn<LoadBuffer, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTag().toString()));
        
        TableColumn<LoadBuffer, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().isBusy())));
        
        TableColumn<LoadBuffer, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEffectiveAddress())));

        table.getColumns().addAll(tagCol, busyCol, addrCol);
        return table;
    }

    private TableView<StoreBuffer> createStoreTable() {
        TableView<StoreBuffer> table = new TableView<>();
        TableColumn<StoreBuffer, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTag().toString()));
        
        TableColumn<StoreBuffer, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().isBusy())));
        
        TableColumn<StoreBuffer, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEffectiveAddress())));
        
        TableColumn<StoreBuffer, String> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getValueToStore())));
        
        TableColumn<StoreBuffer, String> qCol = new TableColumn<>("Q");
        qCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSourceTag() != null ? c.getValue().getSourceTag().toString() : ""));

        table.getColumns().addAll(tagCol, busyCol, addrCol, valCol, qCol);
        return table;
    }

    private void loadProgramFile() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
                InstructionParser parser = new InstructionParser();
                List<Instruction> instructions = parser.parseFile(file.getAbsolutePath());
                initSimulator(instructions);
        }
    }

    private void loadProgramText() {
        String text = programInput.getText();
        if (text.isEmpty()) return;
        
        // Need to save to temp file or modify parser to read string.
        // For now, let's assume we can parse lines.
        // Since InstructionParser takes a filename, I'll write to a temp file.
        try {
            File temp = File.createTempFile("tomasulo_prog", ".txt");
            java.nio.file.Files.write(temp.toPath(), text.getBytes());
            InstructionParser parser = new InstructionParser();
            List<Instruction> instructions = parser.parseFile(temp.getAbsolutePath());
            initSimulator(instructions);
            temp.deleteOnExit();
        } catch (IOException e) {
            log("Error parsing text: " + e.getMessage());
        }
    }

    private void initSimulator(List<Instruction> instructions) {
        simulator = new TomasuloSimulator(instructions, config);
        updateView();
        log("Program loaded. " + instructions.size() + " instructions.");
    }

    private void step() {
        if (simulator == null) return;
        if (!simulator.isFinished()) {
            simulator.step();
            updateView();
            log("Cycle " + simulator.getCycle() + " executed.");
            String cdb = simulator.getCdbStatus();
            if (!cdb.isEmpty()) log("[CDB] " + cdb);
        } else {
            log("Simulation Finished.");
        }
    }

    private void runAll() {
        if (simulator == null) return;
        while (!simulator.isFinished()) {
            simulator.step();
        }
        updateView();
        log("Run complete. Total Cycles: " + simulator.getCycle());
    }

    private void updateView() {
        if (simulator == null) return;

        cycleLabel.setText("Cycle: " + simulator.getCycle());

        fpAddTable.setItems(FXCollections.observableArrayList(simulator.getFpAddSubStations()));
        fpMulTable.setItems(FXCollections.observableArrayList(simulator.getFpMulDivStations()));
        intTable.setItems(FXCollections.observableArrayList(simulator.getIntStations()));
        loadTable.setItems(FXCollections.observableArrayList(simulator.getLoadBuffers()));
        storeTable.setItems(FXCollections.observableArrayList(simulator.getStoreBuffers()));
        
        // Instruction Queue - need to access internal queue or just show what's left?
        // Simulator has getInstructionQueue() which returns InstructionQueue object.
        // InstructionQueue has peek(), but not a list.
        // I should add a toList() to InstructionQueue or similar.
        // For now, I'll skip or try to reflect.
        // Actually, let's just show the original list for now or leave empty if hard.
        // Better: Add getInstructions() to InstructionQueue.
        
        // Registers
        List<RegisterWrapper> regWrappers = new ArrayList<>();
        RegisterFile rf = simulator.getRegisterFile();
        for (int i = 0; i < 32; i++) {
            regWrappers.add(new RegisterWrapper("R" + i, rf.get(i)));
        }
        for (int i = 0; i < 32; i++) {
            regWrappers.add(new RegisterWrapper("F" + i, rf.get(i + 32)));
        }
        registerTable.setItems(FXCollections.observableArrayList(regWrappers));

        // Cache
        List<CacheBlock> validBlocks = new ArrayList<>();
        for (CacheBlock b : simulator.getCache().getBlocks()) {
            if (b.isValid()) {
                validBlocks.add(b);
            }
        }
        cacheTable.setItems(FXCollections.observableArrayList(validBlocks));
        
        // Instruction Queue
        instructionQueueTable.setItems(FXCollections.observableArrayList(simulator.getInstructionQueue().toList()));
    }


    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }

    // Helper class for Register Table
    public static class RegisterWrapper {
        public String name;
        public Register register;
        public RegisterWrapper(String name, Register register) {
            this.name = name;
            this.register = register;
        }
    }
}
