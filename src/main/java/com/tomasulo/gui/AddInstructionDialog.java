package com.tomasulo.gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Dialog for adding new instructions to the queue.
 */
public class AddInstructionDialog extends Dialog<String> {

    private TextField instructionField;
    private ComboBox<String> opcodeCombo;
    private TextField destField;
    private TextField src1Field;
    private TextField src2Field;
    private TextField offsetField;
    private TextField baseField;
    private TextField immediateField;

    public AddInstructionDialog() {
        setTitle("Add Instruction");
        setHeaderText("Enter instruction or build one using the form below");

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));

        // Direct instruction entry
        TitledPane directPane = new TitledPane();
        directPane.setText("Enter Instruction Directly");
        directPane.setCollapsible(false);

        VBox directBox = new VBox(10);
        directBox.setPadding(new Insets(10));
        Label directLabel = new Label("Instruction (e.g., ADD.D F4, F2, F0):");
        instructionField = new TextField();
        instructionField.setPromptText("ADD.D F4, F2, F0");
        directBox.getChildren().addAll(directLabel, instructionField);
        directPane.setContent(directBox);

        // Instruction builder
        TitledPane builderPane = new TitledPane();
        builderPane.setText("Build Instruction");
        builderPane.setCollapsible(false);

        GridPane builderGrid = new GridPane();
        builderGrid.setHgap(10);
        builderGrid.setVgap(10);
        builderGrid.setPadding(new Insets(10));

        int row = 0;

        // Opcode selection
        builderGrid.add(new Label("Operation:"), 0, row);
        opcodeCombo = new ComboBox<>();
        opcodeCombo.getItems().addAll(
            // FP Operations
            "ADD.D", "SUB.D", "MUL.D", "DIV.D",
            "ADD.S", "SUB.S", "MUL.S", "DIV.S",
            // Integer Operations
            "DADD", "DADDI", "DADDIU",
            "DSUB", "DSUBI", "DSUBU",
            "DMUL", "DMULI", "DDIV", "DDIVU",
            "OR", "ORI", "XOR", "XORI",
            "DSLL", "DSRL", "DSRA",
            "SLT", "SLTI", "SLTU", "SLTIU",
            // Memory Operations
            "L.D", "L.S", "LW", "LD", "LB", "LBU", "LH", "LHU", "LWU",
            "S.D", "S.S", "SW", "SD", "SB", "SH",
            // Branch Operations
            "BEQ", "BNE", "BEQZ", "BNEZ",
            // Jump Operations
            "J", "JAL", "JR", "JALR"
        );
        opcodeCombo.setValue("ADD.D");
        opcodeCombo.setOnAction(e -> updateFieldVisibility());
        builderGrid.add(opcodeCombo, 1, row++);

        // Destination register
        builderGrid.add(new Label("Destination:"), 0, row);
        destField = new TextField();
        destField.setPromptText("F4 or R1");
        builderGrid.add(destField, 1, row++);

        // Source 1
        builderGrid.add(new Label("Source 1:"), 0, row);
        src1Field = new TextField();
        src1Field.setPromptText("F2 or R2");
        builderGrid.add(src1Field, 1, row++);

        // Source 2 (for R-type)
        builderGrid.add(new Label("Source 2:"), 0, row);
        src2Field = new TextField();
        src2Field.setPromptText("F0 or R3");
        builderGrid.add(src2Field, 1, row++);

        // Offset (for memory ops)
        builderGrid.add(new Label("Offset:"), 0, row);
        offsetField = new TextField();
        offsetField.setPromptText("0");
        builderGrid.add(offsetField, 1, row++);

        // Base register (for memory ops)
        builderGrid.add(new Label("Base Register:"), 0, row);
        baseField = new TextField();
        baseField.setPromptText("R2");
        builderGrid.add(baseField, 1, row++);

        // Immediate (for I-type)
        builderGrid.add(new Label("Immediate:"), 0, row);
        immediateField = new TextField();
        immediateField.setPromptText("100");
        builderGrid.add(immediateField, 1, row++);

        // Build button
        Button buildButton = new Button("Build Instruction");
        buildButton.setOnAction(e -> buildInstruction());
        builderGrid.add(buildButton, 0, row, 2, 1);

        builderPane.setContent(builderGrid);

        content.getChildren().addAll(directPane, builderPane);

        // Set up dialog
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefWidth(450);

        // Result converter
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return instructionField.getText().trim();
            }
            return null;
        });

        updateFieldVisibility();
    }

    private void updateFieldVisibility() {
        String op = opcodeCombo.getValue();
        boolean isMemory = op != null && (op.contains("L.") || op.contains("S.") || 
            op.startsWith("LW") || op.startsWith("LD") || op.startsWith("LB") ||
            op.startsWith("LH") || op.startsWith("SW") || op.startsWith("SD") ||
            op.startsWith("SB") || op.startsWith("SH"));
        boolean isImmediate = op != null && (op.endsWith("I") || op.endsWith("IU"));
        boolean isRType = !isMemory && !isImmediate;

        offsetField.setDisable(!isMemory);
        baseField.setDisable(!isMemory);
        immediateField.setDisable(!isImmediate);
        src2Field.setDisable(!isRType);
    }

    private void buildInstruction() {
        String op = opcodeCombo.getValue();
        StringBuilder sb = new StringBuilder();
        sb.append(op).append(" ");

        if (op.contains("L.") || op.startsWith("LW") || op.startsWith("LD") || 
            op.startsWith("LB") || op.startsWith("LH")) {
            // Load: L.D dest, offset(base)
            sb.append(destField.getText()).append(", ");
            sb.append(offsetField.getText().isEmpty() ? "0" : offsetField.getText());
            sb.append("(").append(baseField.getText()).append(")");
        } else if (op.contains("S.") || op.startsWith("SW") || op.startsWith("SD") || 
                   op.startsWith("SB") || op.startsWith("SH")) {
            // Store: S.D src, offset(base)
            sb.append(src1Field.getText()).append(", ");
            sb.append(offsetField.getText().isEmpty() ? "0" : offsetField.getText());
            sb.append("(").append(baseField.getText()).append(")");
        } else if (op.endsWith("I") || op.endsWith("IU")) {
            // Immediate: DADDI dest, src1, imm
            sb.append(destField.getText()).append(", ");
            sb.append(src1Field.getText()).append(", ");
            sb.append(immediateField.getText());
        } else {
            // R-type: ADD.D dest, src1, src2
            sb.append(destField.getText()).append(", ");
            sb.append(src1Field.getText()).append(", ");
            sb.append(src2Field.getText());
        }

        instructionField.setText(sb.toString());
    }
}
