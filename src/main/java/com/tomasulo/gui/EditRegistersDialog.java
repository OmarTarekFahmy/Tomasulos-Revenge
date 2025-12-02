package com.tomasulo.gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Dialog for editing register values before simulation.
 */
public class EditRegistersDialog extends Dialog<Void> {

    private final SimulatorController controller;
    private GridPane intGrid;
    private GridPane fpGrid;
    private TextField[] intFields;
    private TextField[] fpFields;

    public EditRegistersDialog(SimulatorController controller) {
        this.controller = controller;

        setTitle("Edit Registers");
        setHeaderText("Set initial register values");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
            createIntRegisterTab(),
            createFpRegisterTab()
        );

        getDialogPane().setContent(tabPane);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefWidth(500);
        getDialogPane().setPrefHeight(500);

        // Apply changes on OK
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                applyChanges();
            }
            return null;
        });
    }

    private Tab createIntRegisterTab() {
        Tab tab = new Tab("Integer Registers");
        
        intGrid = new GridPane();
        intGrid.setHgap(10);
        intGrid.setVgap(5);
        intGrid.setPadding(new Insets(10));

        int numRegs = controller.getNumIntRegisters();
        intFields = new TextField[numRegs];

        int col = 0;
        int row = 0;
        for (int i = 0; i < numRegs; i++) {
            Label label = new Label("R" + i + ":");
            intFields[i] = new TextField();
            intFields[i].setPrefWidth(80);
            intFields[i].setText(String.valueOf(controller.getRegisterFile().get(i).getIntValue()));
            
            intGrid.add(label, col * 2, row);
            intGrid.add(intFields[i], col * 2 + 1, row);

            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }

        ScrollPane scrollPane = new ScrollPane(intGrid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        return tab;
    }

    private Tab createFpRegisterTab() {
        Tab tab = new Tab("FP Registers");
        
        fpGrid = new GridPane();
        fpGrid.setHgap(10);
        fpGrid.setVgap(5);
        fpGrid.setPadding(new Insets(10));

        int numRegs = controller.getNumFpRegisters();
        fpFields = new TextField[numRegs];

        int col = 0;
        int row = 0;
        int fpOffset = controller.getNumIntRegisters();
        for (int i = 0; i < numRegs; i++) {
            Label label = new Label("F" + i + ":");
            fpFields[i] = new TextField();
            fpFields[i].setPrefWidth(100);
            fpFields[i].setText(String.format("%.4f", controller.getRegisterFile().get(fpOffset + i).getValue()));
            
            fpGrid.add(label, col * 2, row);
            fpGrid.add(fpFields[i], col * 2 + 1, row);

            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }

        ScrollPane scrollPane = new ScrollPane(fpGrid);
        scrollPane.setFitToWidth(true);
        tab.setContent(scrollPane);
        return tab;
    }

    private void applyChanges() {
        // Apply integer register changes
        for (int i = 0; i < intFields.length; i++) {
            try {
                long value = Long.parseLong(intFields[i].getText().trim());
                controller.setRegisterValue(i, value);
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        }

        // Apply FP register changes
        int fpOffset = controller.getNumIntRegisters();
        for (int i = 0; i < fpFields.length; i++) {
            try {
                double value = Double.parseDouble(fpFields[i].getText().trim());
                controller.setRegisterValue(fpOffset + i, value);
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        }
    }
}
