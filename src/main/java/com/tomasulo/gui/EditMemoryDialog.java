package com.tomasulo.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * Dialog for editing memory values before simulation.
 */
public class EditMemoryDialog extends Dialog<Void> {

    private final SimulatorController controller;
    private TextField addressField;
    private TextField valueField;
    private TableView<MemoryEntry> memoryTable;
    private ObservableList<MemoryEntry> memoryData = FXCollections.observableArrayList();

    public EditMemoryDialog(SimulatorController controller) {
        this.controller = controller;

        setTitle("Edit Memory");
        setHeaderText("View and modify memory values");

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        // Input section
        HBox inputBox = new HBox(10);
        addressField = new TextField();
        addressField.setPromptText("Address (e.g., 100)");
        addressField.setPrefWidth(120);

        valueField = new TextField();
        valueField.setPromptText("Value (double)");
        valueField.setPrefWidth(150);

        Button setButton = new Button("Set Value");
        setButton.setOnAction(e -> setMemoryValue());

        Button readButton = new Button("Read Value");
        readButton.setOnAction(e -> readMemoryValue());

        inputBox.getChildren().addAll(
            new Label("Address:"), addressField,
            new Label("Value:"), valueField,
            setButton, readButton
        );

        // Memory table
        memoryTable = new TableView<>();
        memoryTable.setItems(memoryData);
        memoryTable.setPrefHeight(300);

        TableColumn<MemoryEntry, Long> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addrCol.setPrefWidth(100);

        TableColumn<MemoryEntry, String> valueCol = new TableColumn<>("Value (Double)");
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueCol.setPrefWidth(150);

        TableColumn<MemoryEntry, String> hexCol = new TableColumn<>("Hex");
        hexCol.setCellValueFactory(new PropertyValueFactory<>("hexValue"));
        hexCol.setPrefWidth(150);

        memoryTable.getColumns().addAll(addrCol, valueCol, hexCol);

        // Quick set section
        TitledPane quickSetPane = new TitledPane();
        quickSetPane.setText("Quick Set Common Addresses");
        quickSetPane.setCollapsible(true);
        quickSetPane.setExpanded(false);

        GridPane quickGrid = new GridPane();
        quickGrid.setHgap(10);
        quickGrid.setVgap(5);
        quickGrid.setPadding(new Insets(10));

        int row = 0;
        long[] commonAddresses = {0, 8, 16, 24, 32, 100, 108, 116, 120, 128};
        for (long addr : commonAddresses) {
            Label label = new Label(String.format("Addr %d:", addr));
            TextField field = new TextField();
            field.setPromptText("Value");
            field.setPrefWidth(100);
            
            Button btn = new Button("Set");
            final long finalAddr = addr;
            btn.setOnAction(e -> {
                try {
                    double val = Double.parseDouble(field.getText());
                    controller.setMemoryValue(finalAddr, val);
                    updateTable(finalAddr, val);
                } catch (NumberFormatException ex) {
                    // Ignore invalid input
                }
            });

            quickGrid.add(label, 0, row);
            quickGrid.add(field, 1, row);
            quickGrid.add(btn, 2, row);
            row++;
        }
        quickSetPane.setContent(quickGrid);

        content.getChildren().addAll(inputBox, memoryTable, quickSetPane);

        // Pre-populate with some addresses
        for (long addr : commonAddresses) {
            double val = controller.getMemoryValue(addr);
            memoryData.add(new MemoryEntry(addr, val));
        }

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        getDialogPane().setPrefWidth(550);
        getDialogPane().setPrefHeight(600);
    }

    private void setMemoryValue() {
        try {
            long address = Long.parseLong(addressField.getText().trim());
            double value = Double.parseDouble(valueField.getText().trim());
            controller.setMemoryValue(address, value);
            updateTable(address, value);
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid address or value");
            alert.showAndWait();
        }
    }

    private void readMemoryValue() {
        try {
            long address = Long.parseLong(addressField.getText().trim());
            double value = controller.getMemoryValue(address);
            valueField.setText(String.format("%.6f", value));
            updateTable(address, value);
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid address");
            alert.showAndWait();
        }
    }

    private void updateTable(long address, double value) {
        // Update existing entry or add new one
        for (MemoryEntry entry : memoryData) {
            if (entry.getAddress() == address) {
                entry.setValue(String.format("%.6f", value));
                entry.setHexValue(Long.toHexString(Double.doubleToLongBits(value)));
                memoryTable.refresh();
                return;
            }
        }
        memoryData.add(new MemoryEntry(address, value));
    }

    public static class MemoryEntry {
        private long address;
        private String value;
        private String hexValue;

        public MemoryEntry(long address, double value) {
            this.address = address;
            this.value = String.format("%.6f", value);
            this.hexValue = Long.toHexString(Double.doubleToLongBits(value));
        }

        public long getAddress() { return address; }
        public void setAddress(long address) { this.address = address; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getHexValue() { return hexValue; }
        public void setHexValue(String hexValue) { this.hexValue = hexValue; }
    }
}
