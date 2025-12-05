package com.tomasulo.gui;

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
import com.tomasulo.gui.controller.SimulationController;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SimulationView extends BorderPane {

    private final SimulationController controller;

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
    private VBox cacheBox;
    private TableView<Instruction> instructionQueueTable;

    private TabPane tabPane;
    private VBox centerPane;


    public SimulationView(SimulationController controller) {
        this.controller = controller;
        setupUI();
    }

    private void setupUI() {
        // Top Control Bar
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        
        Button loadBtn = new Button("Load Program File");
        loadBtn.setOnAction(e -> controller.loadProgramFile());

        Button loadTextBtn = new Button("Load from Text Area");
        loadTextBtn.setMaxWidth(Double.MAX_VALUE);
        loadTextBtn.setOnAction(e -> controller.loadProgramText(programInput.getText()));

        Button stepBtn = new Button("Step");
        stepBtn.setOnAction(e -> controller.step());

        Button runBtn = new Button("Run All");
        runBtn.setOnAction(e -> controller.runAll());

        javafx.scene.control.ToggleButton toggleViewBtn = new javafx.scene.control.ToggleButton("Show All Stations");
        toggleViewBtn.setSelected(true);
        toggleViewBtn.setOnAction(e -> switchCenterView(toggleViewBtn.isSelected()));

        cycleLabel = new Label("Cycle: 0");
        cycleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        topBar.getChildren().addAll(loadBtn, stepBtn, runBtn, toggleViewBtn, cycleLabel);
        setTop(topBar);

        // Left Side: Program Input & Instruction Queue
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(10));
        leftPane.setPrefWidth(300);

        programInput = new TextArea();
        programInput.setPromptText("Enter Assembly Code Here...");
        programInput.setPrefHeight(200);

        instructionQueueTable = new TableView<>();
        instructionQueueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Instruction, String> iqOpCol = new TableColumn<>("Opcode");
        iqOpCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOpcode().toString()));
        TableColumn<Instruction, String> iqDestCol = new TableColumn<>("Dest");
        iqDestCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getDestReg())));
        instructionQueueTable.getColumns().addAll(iqOpCol, iqDestCol);
        instructionQueueTable.setPlaceholder(new Label("Instruction Queue Empty"));

        leftPane.getChildren().addAll(new Label("Program Input:"), programInput, loadTextBtn, new Label("Instruction Queue:"), instructionQueueTable);
        setLeft(leftPane);

        // Center: Reservation Stations & Buffers
        centerPane = new VBox(10);
        centerPane.setPadding(new Insets(10));
        
        fpAddTable = createRsTable("FP Add/Sub Stations");
        fpMulTable = createRsTable("FP Mul/Div Stations");
        intTable = createRsTable("Integer Stations");
        loadTable = createLoadTable();
        storeTable = createStoreTable();

        tabPane = new TabPane();
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
        registerTable.setEditable(true);
        registerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<RegisterWrapper, String> regNameCol = new TableColumn<>("Reg");
        regNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
        TableColumn<RegisterWrapper, String> regValCol = new TableColumn<>("Value");
        regValCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().register.getValue())));
        regValCol.setCellFactory(TextFieldTableCell.forTableColumn());
        regValCol.setOnEditCommit(t -> {
            RegisterWrapper wrapper = t.getRowValue();
            controller.setRegister(wrapper.name, t.getNewValue());
        });
        TableColumn<RegisterWrapper, String> regQiCol = new TableColumn<>("Qi");
        regQiCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().register.getQi().toString()));
        
        registerTable.getColumns().addAll(regNameCol, regQiCol, regValCol);
        
        // Cache Table
        cacheTable = new TableView<>();
        cacheTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<CacheBlock, String> cacheTagCol = new TableColumn<>("Tag");
        cacheTagCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTag())));
        TableColumn<CacheBlock, String> cacheDataCol = new TableColumn<>("Data");
        cacheDataCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDataHex()));
        TableColumn<CacheBlock, String> cacheDirtyCol = new TableColumn<>("Dirty");
        cacheDirtyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().isDirty())));
        
        cacheTable.getColumns().addAll(cacheTagCol, cacheDirtyCol, cacheDataCol);
        cacheTable.setPlaceholder(new Label("Cache Empty / Invalid"));
        cacheTable.setPrefHeight(200);

        Button loadRegBtn = new Button("Load Register File");
        loadRegBtn.setMaxWidth(Double.MAX_VALUE);
        loadRegBtn.setOnAction(e -> controller.loadRegisterFile());

        Button setCacheBtn = new Button("Set Cache Value");
        setCacheBtn.setMaxWidth(Double.MAX_VALUE);
        setCacheBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("0 10.5");
            dialog.setTitle("Set Cache Value");
            dialog.setHeaderText("Enter Address and Value (e.g., 100 50.5)");
            dialog.showAndWait().ifPresent(result -> {
                String[] parts = result.trim().split("\\s+");
                if (parts.length == 2) {
                    controller.setCacheValue(parts[0], parts[1]);
                } else {
                    log("Invalid format. Use: ADDRESS VALUE");
                }
            });
        });

        cacheBox = new VBox(5);
        cacheBox.getChildren().addAll(new Label("Cache Content"), cacheTable, setCacheBtn);

        rightPane.getChildren().addAll(new Label("Register File"), registerTable, loadRegBtn);
        VBox.setVgrow(registerTable, Priority.ALWAYS);
        setRight(rightPane);


        // Bottom: Log
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(150);
        setBottom(logArea);
        
        switchCenterView(true);
    }

    private void switchCenterView(boolean showAll) {
        centerPane.getChildren().clear();
        if (showAll) {
            // Detach from tabs
            for (Tab t : tabPane.getTabs()) {
                t.setContent(null);
            }
            
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));
            
            // Row 0: FP Add/Sub and FP Mul/Div (Span 3 columns each in a 6-column grid)
            VBox fpAddBox = createTableBox("FP Add/Sub Stations", fpAddTable);
            grid.add(fpAddBox, 0, 0);
            GridPane.setColumnSpan(fpAddBox, 3);
            
            VBox fpMulBox = createTableBox("FP Mul/Div Stations", fpMulTable);
            grid.add(fpMulBox, 3, 0);
            GridPane.setColumnSpan(fpMulBox, 3);
            
            // Row 1: Integer, Store, Load (Span 2 columns each in a 6-column grid)
            VBox intBox = createTableBox("Integer Stations", intTable);
            grid.add(intBox, 0, 1);
            GridPane.setColumnSpan(intBox, 2);
            
            VBox storeBox = createTableBox("Store Buffers", storeTable);
            grid.add(storeBox, 2, 1);
            GridPane.setColumnSpan(storeBox, 2);

            VBox loadBox = createTableBox("Load Buffers", loadTable);
            grid.add(loadBox, 4, 1);
            GridPane.setColumnSpan(loadBox, 2);
            
            // Row 2: Cache (Span all 6 columns)
            grid.add(cacheBox, 0, 2);
            GridPane.setColumnSpan(cacheBox, 6);
            
            // Make columns grow equally (16.66% each)
            for (int i = 0; i < 6; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPercentWidth(100.0 / 6.0);
                grid.getColumnConstraints().add(col);
            }
            
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(grid);
            scroll.setFitToWidth(true);
            centerPane.getChildren().add(scroll);
        } else {
            // Re-attach to tabs
            tabPane.getTabs().get(0).setContent(fpAddTable);
            tabPane.getTabs().get(1).setContent(fpMulTable);
            tabPane.getTabs().get(2).setContent(intTable);
            tabPane.getTabs().get(3).setContent(loadTable);
            tabPane.getTabs().get(4).setContent(storeTable);
            
            VBox layout = new VBox(10);
            layout.getChildren().addAll(tabPane, cacheBox);
            centerPane.getChildren().add(layout);
        }
    }

    private VBox createTableBox(String title, TableView<?> table) {
        VBox box = new VBox(5);
        box.getChildren().addAll(new Label(title), table);
        return box;
    }

    private TableView<ReservationStation> createRsTable(String title) {
        TableView<ReservationStation> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(25);
        table.prefHeightProperty().bind(Bindings.size(table.getItems()).multiply(table.getFixedCellSize()).add(30));
        table.minHeightProperty().bind(table.prefHeightProperty());
        table.maxHeightProperty().bind(table.prefHeightProperty());
        
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
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(25);
        table.prefHeightProperty().bind(Bindings.size(table.getItems()).multiply(table.getFixedCellSize()).add(30));
        table.minHeightProperty().bind(table.prefHeightProperty());
        table.maxHeightProperty().bind(table.prefHeightProperty());

        TableColumn<LoadBuffer, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().toString()));
        
        TableColumn<LoadBuffer, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().isBusy())));
        
        TableColumn<LoadBuffer, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getEffectiveAddress())));

        table.getColumns().addAll(tagCol, busyCol, addrCol);
        return table;
    }

    private TableView<StoreBuffer> createStoreTable() {
        TableView<StoreBuffer> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(25);
        table.prefHeightProperty().bind(Bindings.size(table.getItems()).multiply(table.getFixedCellSize()).add(30));
        table.minHeightProperty().bind(table.prefHeightProperty());
        table.maxHeightProperty().bind(table.prefHeightProperty());

        TableColumn<StoreBuffer, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().toString()));
        
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

    public void updateView() {
        TomasuloSimulator simulator = controller.getSimulator();
        if (simulator == null) return;

        cycleLabel.setText("Cycle: " + simulator.getCycle());

        fpAddTable.getItems().setAll(simulator.getFpAddSubStations());
        fpAddTable.refresh();

        fpMulTable.getItems().setAll(simulator.getFpMulDivStations());
        fpMulTable.refresh();

        intTable.getItems().setAll(simulator.getIntStations());
        intTable.refresh();

        loadTable.getItems().setAll(simulator.getLoadBuffers());
        loadTable.refresh();

        storeTable.getItems().setAll(simulator.getStoreBuffers());
        storeTable.refresh();
        
        // Registers
        List<RegisterWrapper> regWrappers = new ArrayList<>();
        RegisterFile rf = simulator.getRegisterFile();
        for (int i = 0; i < 32; i++) {
            regWrappers.add(new RegisterWrapper("R" + i, rf.get(i)));
        }
        for (int i = 0; i < 32; i++) {
            regWrappers.add(new RegisterWrapper("F" + i, rf.get(i + 32)));
        }
        registerTable.getItems().setAll(regWrappers);
        registerTable.refresh();

        // Cache
        List<CacheBlock> validBlocks = new ArrayList<>();
        for (CacheBlock b : simulator.getCache().getBlocks()) {
            if (b.isValid()) {
                validBlocks.add(b);
            }
        }
        cacheTable.getItems().setAll(validBlocks);
        cacheTable.refresh();
        
        // Instruction Queue
        instructionQueueTable.getItems().setAll(simulator.getInstructionQueue().toList());
        instructionQueueTable.refresh();
    }

    public void log(String msg) {
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
