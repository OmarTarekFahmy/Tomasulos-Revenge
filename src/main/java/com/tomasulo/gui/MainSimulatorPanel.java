package com.tomasulo.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Main panel containing all the simulation views:
 * - Instruction Queue table
 * - Reservation Stations tables (FP and INT)
 * - Load/Store Buffer tables
 * - Register File tables (INT and FP)
 * - Cache table
 * - Control buttons (Step, Run, Reset, Load)
 * - Cycle counter
 */
public class MainSimulatorPanel extends BorderPane {

    private final SimulatorController controller;

    // Control components
    private Label cycleLabel;
    private Button stepButton;
    private Button runButton;
    private Button resetButton;
    private Button loadFileButton;
    private Button addInstructionButton;
    private TextArea logArea;

    // Tables
    private TableView<InstructionQueueRow> instructionTable;
    private TableView<ReservationStationRow> fpAddSubRsTable;
    private TableView<ReservationStationRow> fpMulDivRsTable;
    private TableView<ReservationStationRow> intAluRsTable;
    private TableView<ReservationStationRow> intMulDivRsTable;
    private TableView<LoadBufferRow> loadBufferTable;
    private TableView<StoreBufferRow> storeBufferTable;
    private TableView<RegisterRow> intRegisterTable;
    private TableView<RegisterRow> fpRegisterTable;
    private TableView<CacheLineRow> cacheTable;

    // Observable data lists
    private ObservableList<InstructionQueueRow> instructionData = FXCollections.observableArrayList();
    private ObservableList<ReservationStationRow> fpAddSubRsData = FXCollections.observableArrayList();
    private ObservableList<ReservationStationRow> fpMulDivRsData = FXCollections.observableArrayList();
    private ObservableList<ReservationStationRow> intAluRsData = FXCollections.observableArrayList();
    private ObservableList<ReservationStationRow> intMulDivRsData = FXCollections.observableArrayList();
    private ObservableList<LoadBufferRow> loadBufferData = FXCollections.observableArrayList();
    private ObservableList<StoreBufferRow> storeBufferData = FXCollections.observableArrayList();
    private ObservableList<RegisterRow> intRegisterData = FXCollections.observableArrayList();
    private ObservableList<RegisterRow> fpRegisterData = FXCollections.observableArrayList();
    private ObservableList<CacheLineRow> cacheData = FXCollections.observableArrayList();

    public MainSimulatorPanel(SimulatorController controller) {
        this.controller = controller;
        controller.setMainPanel(this);

        // Build the UI
        setTop(createTopToolbar());
        setCenter(createCenterContent());
        setBottom(createLogPanel());

        // Initialize data
        refreshAllTables();
    }

    private HBox createTopToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #e0e0e0;");

        // Cycle display
        cycleLabel = new Label("Cycle: 0");
        cycleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Control buttons
        stepButton = new Button("Step (1 Cycle)");
        stepButton.setOnAction(e -> {
            controller.step();
            refreshAllTables();
        });

        runButton = new Button("Run to Completion");
        runButton.setOnAction(e -> {
            controller.runToCompletion();
            refreshAllTables();
        });

        resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            controller.reset();
            refreshAllTables();
        });

        loadFileButton = new Button("Load Program");
        loadFileButton.setOnAction(e -> loadProgramFromFile());

        addInstructionButton = new Button("Add Instruction");
        addInstructionButton.setOnAction(e -> showAddInstructionDialog());

        Button editRegistersButton = new Button("Edit Registers");
        editRegistersButton.setOnAction(e -> showEditRegistersDialog());

        Button editMemoryButton = new Button("Edit Memory");
        editMemoryButton.setOnAction(e -> showEditMemoryDialog());

        // Separator for visual grouping
        Separator sep1 = new Separator();
        sep1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        Separator sep2 = new Separator();
        sep2.setOrientation(javafx.geometry.Orientation.VERTICAL);

        toolbar.getChildren().addAll(
            cycleLabel,
            sep1,
            stepButton, runButton, resetButton,
            sep2,
            loadFileButton, addInstructionButton, editRegistersButton, editMemoryButton
        );

        return toolbar;
    }

    private ScrollPane createCenterContent() {
        // Main content area with tables
        VBox content = new VBox(15);
        content.setPadding(new Insets(10));

        // Top section: Instruction Queue and Timing Table
        TitledPane instructionPane = new TitledPane("Instruction Queue & Timing", createInstructionTable());
        instructionPane.setCollapsible(false);

        // Middle section: Reservation Stations (side by side)
        HBox rsSection = new HBox(10);
        
        VBox fpRsBox = new VBox(10);
        TitledPane fpAddSubPane = new TitledPane("FP Add/Sub Reservation Stations", createFpAddSubRsTable());
        fpAddSubPane.setCollapsible(false);
        TitledPane fpMulDivPane = new TitledPane("FP Mul/Div Reservation Stations", createFpMulDivRsTable());
        fpMulDivPane.setCollapsible(false);
        fpRsBox.getChildren().addAll(fpAddSubPane, fpMulDivPane);
        HBox.setHgrow(fpRsBox, Priority.ALWAYS);

        VBox intRsBox = new VBox(10);
        TitledPane intAluPane = new TitledPane("Integer ALU Reservation Stations", createIntAluRsTable());
        intAluPane.setCollapsible(false);
        TitledPane intMulDivPane = new TitledPane("Integer Mul/Div Reservation Stations", createIntMulDivRsTable());
        intMulDivPane.setCollapsible(false);
        intRsBox.getChildren().addAll(intAluPane, intMulDivPane);
        HBox.setHgrow(intRsBox, Priority.ALWAYS);

        rsSection.getChildren().addAll(fpRsBox, intRsBox);

        // Load/Store Buffers section
        HBox bufferSection = new HBox(10);
        TitledPane loadPane = new TitledPane("Load Buffers", createLoadBufferTable());
        loadPane.setCollapsible(false);
        HBox.setHgrow(loadPane, Priority.ALWAYS);
        TitledPane storePane = new TitledPane("Store Buffers", createStoreBufferTable());
        storePane.setCollapsible(false);
        HBox.setHgrow(storePane, Priority.ALWAYS);
        bufferSection.getChildren().addAll(loadPane, storePane);

        // Register Files section
        HBox registerSection = new HBox(10);
        TitledPane intRegPane = new TitledPane("Integer Registers (R0-R31)", createIntRegisterTable());
        intRegPane.setCollapsible(false);
        HBox.setHgrow(intRegPane, Priority.ALWAYS);
        TitledPane fpRegPane = new TitledPane("FP Registers (F0-F31)", createFpRegisterTable());
        fpRegPane.setCollapsible(false);
        HBox.setHgrow(fpRegPane, Priority.ALWAYS);
        registerSection.getChildren().addAll(intRegPane, fpRegPane);

        // Cache section
        TitledPane cachePane = new TitledPane("Data Cache", createCacheTable());
        cachePane.setCollapsible(false);

        content.getChildren().addAll(
            instructionPane,
            rsSection,
            bufferSection,
            registerSection,
            cachePane
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        return scrollPane;
    }

    private VBox createLogPanel() {
        VBox logPanel = new VBox(5);
        logPanel.setPadding(new Insets(5, 10, 10, 10));
        logPanel.setStyle("-fx-background-color: #f5f5f5;");

        Label logLabel = new Label("Simulation Log:");
        logLabel.setStyle("-fx-font-weight: bold;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(5);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace;");

        logPanel.getChildren().addAll(logLabel, logArea);
        return logPanel;
    }

    // --- Table Creation Methods ---

    private TableView<InstructionQueueRow> createInstructionTable() {
        instructionTable = new TableView<>();
        instructionTable.setItems(instructionData);
        instructionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        instructionTable.setPrefHeight(200);

        TableColumn<InstructionQueueRow, Integer> indexCol = new TableColumn<>("#");
        indexCol.setCellValueFactory(new PropertyValueFactory<>("index"));
        indexCol.setPrefWidth(40);

        TableColumn<InstructionQueueRow, String> instrCol = new TableColumn<>("Instruction");
        instrCol.setCellValueFactory(new PropertyValueFactory<>("instruction"));
        instrCol.setPrefWidth(200);

        TableColumn<InstructionQueueRow, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("issueStatus"));
        statusCol.setPrefWidth(100);

        TableColumn<InstructionQueueRow, String> issueCol = new TableColumn<>("Issue");
        issueCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getIssueCycleString()));
        issueCol.setPrefWidth(60);

        TableColumn<InstructionQueueRow, String> execStartCol = new TableColumn<>("Exec Start");
        execStartCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getExecStartCycleString()));
        execStartCol.setPrefWidth(80);

        TableColumn<InstructionQueueRow, String> execEndCol = new TableColumn<>("Exec End");
        execEndCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getExecEndCycleString()));
        execEndCol.setPrefWidth(80);

        TableColumn<InstructionQueueRow, String> wbCol = new TableColumn<>("Write Back");
        wbCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getWriteBackCycleString()));
        wbCol.setPrefWidth(80);

        instructionTable.getColumns().addAll(indexCol, instrCol, statusCol, issueCol, execStartCol, execEndCol, wbCol);
        return instructionTable;
    }

    private TableView<ReservationStationRow> createFpAddSubRsTable() {
        fpAddSubRsTable = createRsTable();
        fpAddSubRsTable.setItems(fpAddSubRsData);
        return fpAddSubRsTable;
    }

    private TableView<ReservationStationRow> createFpMulDivRsTable() {
        fpMulDivRsTable = createRsTable();
        fpMulDivRsTable.setItems(fpMulDivRsData);
        return fpMulDivRsTable;
    }

    private TableView<ReservationStationRow> createIntAluRsTable() {
        intAluRsTable = createRsTable();
        intAluRsTable.setItems(intAluRsData);
        return intAluRsTable;
    }

    private TableView<ReservationStationRow> createIntMulDivRsTable() {
        intMulDivRsTable = createRsTable();
        intMulDivRsTable.setItems(intMulDivRsData);
        return intMulDivRsTable;
    }

    private TableView<ReservationStationRow> createRsTable() {
        TableView<ReservationStationRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(120);

        TableColumn<ReservationStationRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<ReservationStationRow, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(new PropertyValueFactory<>("busy"));

        TableColumn<ReservationStationRow, String> opCol = new TableColumn<>("Op");
        opCol.setCellValueFactory(new PropertyValueFactory<>("op"));

        TableColumn<ReservationStationRow, String> vjCol = new TableColumn<>("Vj");
        vjCol.setCellValueFactory(new PropertyValueFactory<>("vj"));

        TableColumn<ReservationStationRow, String> vkCol = new TableColumn<>("Vk");
        vkCol.setCellValueFactory(new PropertyValueFactory<>("vk"));

        TableColumn<ReservationStationRow, String> qjCol = new TableColumn<>("Qj");
        qjCol.setCellValueFactory(new PropertyValueFactory<>("qj"));

        TableColumn<ReservationStationRow, String> qkCol = new TableColumn<>("Qk");
        qkCol.setCellValueFactory(new PropertyValueFactory<>("qk"));

        TableColumn<ReservationStationRow, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));

        table.getColumns().addAll(nameCol, busyCol, opCol, vjCol, vkCol, qjCol, qkCol, stateCol);
        return table;
    }

    private TableView<LoadBufferRow> createLoadBufferTable() {
        loadBufferTable = new TableView<>();
        loadBufferTable.setItems(loadBufferData);
        loadBufferTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loadBufferTable.setPrefHeight(120);

        TableColumn<LoadBufferRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<LoadBufferRow, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(new PropertyValueFactory<>("busy"));

        TableColumn<LoadBufferRow, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<LoadBufferRow, String> destCol = new TableColumn<>("Dest");
        destCol.setCellValueFactory(new PropertyValueFactory<>("destReg"));

        TableColumn<LoadBufferRow, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));

        loadBufferTable.getColumns().addAll(nameCol, busyCol, addrCol, destCol, stateCol);
        return loadBufferTable;
    }

    private TableView<StoreBufferRow> createStoreBufferTable() {
        storeBufferTable = new TableView<>();
        storeBufferTable.setItems(storeBufferData);
        storeBufferTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        storeBufferTable.setPrefHeight(120);

        TableColumn<StoreBufferRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<StoreBufferRow, String> busyCol = new TableColumn<>("Busy");
        busyCol.setCellValueFactory(new PropertyValueFactory<>("busy"));

        TableColumn<StoreBufferRow, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<StoreBufferRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));

        TableColumn<StoreBufferRow, String> srcCol = new TableColumn<>("Src");
        srcCol.setCellValueFactory(new PropertyValueFactory<>("srcReg"));

        TableColumn<StoreBufferRow, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));

        storeBufferTable.getColumns().addAll(nameCol, busyCol, addrCol, valueCol, srcCol, stateCol);
        return storeBufferTable;
    }

    private TableView<RegisterRow> createIntRegisterTable() {
        intRegisterTable = new TableView<>();
        intRegisterTable.setItems(intRegisterData);
        intRegisterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        intRegisterTable.setPrefHeight(200);

        TableColumn<RegisterRow, String> nameCol = new TableColumn<>("Reg");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<RegisterRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));

        TableColumn<RegisterRow, String> qiCol = new TableColumn<>("Qi");
        qiCol.setCellValueFactory(new PropertyValueFactory<>("qi"));

        intRegisterTable.getColumns().addAll(nameCol, valueCol, qiCol);
        return intRegisterTable;
    }

    private TableView<RegisterRow> createFpRegisterTable() {
        fpRegisterTable = new TableView<>();
        fpRegisterTable.setItems(fpRegisterData);
        fpRegisterTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fpRegisterTable.setPrefHeight(200);

        TableColumn<RegisterRow, String> nameCol = new TableColumn<>("Reg");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<RegisterRow, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));

        TableColumn<RegisterRow, String> qiCol = new TableColumn<>("Qi");
        qiCol.setCellValueFactory(new PropertyValueFactory<>("qi"));

        fpRegisterTable.getColumns().addAll(nameCol, valueCol, qiCol);
        return fpRegisterTable;
    }

    private TableView<CacheLineRow> createCacheTable() {
        cacheTable = new TableView<>();
        cacheTable.setItems(cacheData);
        cacheTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cacheTable.setPrefHeight(150);

        TableColumn<CacheLineRow, Integer> indexCol = new TableColumn<>("Index");
        indexCol.setCellValueFactory(new PropertyValueFactory<>("index"));

        TableColumn<CacheLineRow, String> validCol = new TableColumn<>("Valid");
        validCol.setCellValueFactory(new PropertyValueFactory<>("valid"));

        TableColumn<CacheLineRow, String> tagCol = new TableColumn<>("Tag");
        tagCol.setCellValueFactory(new PropertyValueFactory<>("tag"));

        TableColumn<CacheLineRow, String> dataCol = new TableColumn<>("Data");
        dataCol.setCellValueFactory(new PropertyValueFactory<>("data"));

        TableColumn<CacheLineRow, String> accessCol = new TableColumn<>("Last Access");
        accessCol.setCellValueFactory(new PropertyValueFactory<>("lastAccess"));

        cacheTable.getColumns().addAll(indexCol, validCol, tagCol, dataCol, accessCol);
        return cacheTable;
    }

    // --- Action Handlers ---

    private void loadProgramFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load MIPS Assembly Program");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Assembly Files", "*.asm", "*.s", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            controller.loadProgramFromFile(file);
            refreshAllTables();
            appendLog("Loaded program from: " + file.getName());
        }
    }

    private void showAddInstructionDialog() {
        AddInstructionDialog dialog = new AddInstructionDialog();
        dialog.showAndWait().ifPresent(instruction -> {
            controller.addInstruction(instruction);
            refreshAllTables();
            appendLog("Added instruction: " + instruction);
        });
    }

    private void showEditRegistersDialog() {
        EditRegistersDialog dialog = new EditRegistersDialog(controller);
        dialog.showAndWait();
        refreshAllTables();
    }

    private void showEditMemoryDialog() {
        EditMemoryDialog dialog = new EditMemoryDialog(controller);
        dialog.showAndWait();
        refreshAllTables();
    }

    // --- Table Refresh Methods ---

    public void refreshAllTables() {
        updateCycleLabel();
        updateInstructionTable();
        updateReservationStationTables();
        updateLoadStoreBufferTables();
        updateRegisterTables();
        updateCacheTable();
    }

    private void updateCycleLabel() {
        cycleLabel.setText("Cycle: " + controller.getCurrentCycle());
    }

    private void updateInstructionTable() {
        instructionData.setAll(controller.getInstructionQueueData());
    }

    private void updateReservationStationTables() {
        fpAddSubRsData.setAll(controller.getFpAddSubRsData());
        fpMulDivRsData.setAll(controller.getFpMulDivRsData());
        intAluRsData.setAll(controller.getIntAluRsData());
        intMulDivRsData.setAll(controller.getIntMulDivRsData());
    }

    private void updateLoadStoreBufferTables() {
        loadBufferData.setAll(controller.getLoadBufferData());
        storeBufferData.setAll(controller.getStoreBufferData());
    }

    private void updateRegisterTables() {
        intRegisterData.setAll(controller.getIntRegisterData());
        fpRegisterData.setAll(controller.getFpRegisterData());
    }

    private void updateCacheTable() {
        cacheData.setAll(controller.getCacheData());
    }

    public void appendLog(String message) {
        logArea.appendText("[Cycle " + controller.getCurrentCycle() + "] " + message + "\n");
    }

    // Getters for observable lists (for external updates)
    public ObservableList<InstructionQueueRow> getInstructionData() { return instructionData; }
    public ObservableList<ReservationStationRow> getFpAddSubRsData() { return fpAddSubRsData; }
    public ObservableList<ReservationStationRow> getFpMulDivRsData() { return fpMulDivRsData; }
    public ObservableList<ReservationStationRow> getIntAluRsData() { return intAluRsData; }
    public ObservableList<ReservationStationRow> getIntMulDivRsData() { return intMulDivRsData; }
    public ObservableList<LoadBufferRow> getLoadBufferData() { return loadBufferData; }
    public ObservableList<StoreBufferRow> getStoreBufferData() { return storeBufferData; }
    public ObservableList<RegisterRow> getIntRegisterData() { return intRegisterData; }
    public ObservableList<RegisterRow> getFpRegisterData() { return fpRegisterData; }
    public ObservableList<CacheLineRow> getCacheData() { return cacheData; }
}
