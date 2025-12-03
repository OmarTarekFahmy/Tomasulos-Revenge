package com.tomasulo.parser;

import java.util.ArrayList;
import java.util.HashMap;

import com.tomasulo.core.Instruction;
import com.tomasulo.core.Instruction.Opcode;;

public class InstructionParser {

    // private final static Configuration config = Configuration.getInstance();
    public static ArrayList<String> loadFile(String filePath) {
        ArrayList<String> lines = new ArrayList<>();

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (java.io.IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return null;
        }
        return lines;
    }

    private static Instruction parseLine(String line, HashMap<String, Integer> labels, int lineNumber) {

        // Split by whitespace and commas
        String[] tokens = line.split("[\\s,]+");
        String opStr = tokens[0].toUpperCase();

        Opcode opcode;
        int destReg = -1, src1Reg = -1, src2Reg = -1;
        int baseReg = -1, offset = 0, immediate = 0;

        if (tokens[0].contains(":")) {
            labels.put(tokens[0].replace(":", ""), lineNumber); // Placeholder address
            tokens = java.util.Arrays.copyOfRange(tokens, 1, tokens.length); // Skip label for further parsing
            opStr = tokens[0].toUpperCase();
        }

        try {
            switch (opStr) {
                // Integer arithmetic with immediate
                case "DADDI", "DSUBI":
                    opcode = Opcode.valueOf(opStr);
                    destReg = parseRegister(tokens[1]);
                    src1Reg = parseRegister(tokens[2]);
                    immediate = Integer.parseInt(tokens[3]);
                    break;

                // FP ADD/SUB
                case "ADD.D":
                    opcode = Opcode.ADD_D;
                    destReg = parseRegister(tokens[1]);
                    src1Reg = parseRegister(tokens[2]);
                    src2Reg = parseRegister(tokens[3]);
                    break;

                case "SUB.D":
                    opcode = Opcode.SUB_D;
                    destReg = parseRegister(tokens[1]);
                    src1Reg = parseRegister(tokens[2]);
                    src2Reg = parseRegister(tokens[3]);
                    break;

                // FP MUL/DIV
                case "MUL.D":
                    opcode = Opcode.MUL_D;
                    destReg = parseRegister(tokens[1]);
                    src1Reg = parseRegister(tokens[2]);
                    src2Reg = parseRegister(tokens[3]);
                    break;

                case "DIV.D":
                    opcode = Opcode.DIV_D;
                    destReg = parseRegister(tokens[1]);
                    src1Reg = parseRegister(tokens[2]);
                    src2Reg = parseRegister(tokens[3]);
                    break;

                // Loads
                case "LD":
                    opcode = Opcode.LD;
                    destReg = parseRegister(tokens[1]);
                    int[] memOp = parseMemoryOperand(tokens[2]);
                    offset = memOp[0];
                    baseReg = memOp[1];
                    break;

                case "LW":
                    opcode = Opcode.LW;
                    destReg = parseRegister(tokens[1]);
                    memOp = parseMemoryOperand(tokens[2]);
                    offset = memOp[0];
                    baseReg = memOp[1];
                    break;

                case "L.S":
                    opcode = Opcode.L_S;
                    destReg = parseRegister(tokens[1]);
                    memOp = parseMemoryOperand(tokens[2]);
                    offset = memOp[0];
                    baseReg = memOp[1];
                    break;

                case "L.D":
                    opcode = Opcode.L_D;
                    destReg = parseRegister(tokens[1]);
                    memOp = parseMemoryOperand(tokens[2]);
                    offset = memOp[0];
                    baseReg = memOp[1];
                    break;

                // Stores
                case "SD":
                    opcode = Opcode.SD;
                    src1Reg = parseRegister(tokens[1]); // value to store
                    memOp = parseMemoryOperand(tokens[2]);
                    offset = memOp[0];
                    baseReg = memOp[1];
                    break;

                case "SW":
                    opcode = Opcode.SW;
                    src1Reg = parseRegister(tokens[1]);
                    memOp = parseMemoryOperand(tokens[2]);
                    offset = memOp[0];
                    baseReg = memOp[1];
                    break;

                case "S.S":
                    opcode = Opcode.S_S;
                    src1Reg = parseRegister(tokens[1]);
                    memOp = parseMemoryOperand(tokens[2]);
                    offset = memOp[0];
                    baseReg = memOp[1];
                    break;

                case "S.D":
                    opcode = Opcode.S_D;
                    src1Reg = parseRegister(tokens[1]);
                    memOp = parseMemoryOperand(tokens[2]);
                    offset = memOp[0];
                    baseReg = memOp[1];
                    break;

                // Branches
                case "BEQ":
                    opcode = Opcode.BEQ;
                    src1Reg = parseRegister(tokens[1]);
                    src2Reg = parseRegister(tokens[2]);

                    // Check if third token is a label or immediate
                    if (labels.containsKey(tokens[3])) {
                        immediate = labels.get(tokens[3]);
                    } else {
                        immediate = Integer.parseInt(tokens[3]);
                    }
                    break;

                case "BNE":
                    opcode = Opcode.BNE;
                    src1Reg = parseRegister(tokens[1]);
                    src2Reg = parseRegister(tokens[2]);

                    // Check if third token is a label or immediate
                    if (labels.containsKey(tokens[3])) {
                        immediate = labels.get(tokens[3]);
                    } else {
                        immediate = Integer.parseInt(tokens[3]);
                    }
                    break;

                default:
                    System.err.println("Unknown instruction: " + opStr);
                    return null;
            }

            return new Instruction(opcode, destReg, src1Reg, src2Reg, baseReg, offset, immediate);

        } catch (Exception e) {
            System.err.println("Error parsing line: " + line + " - " + e.getMessage());
            return null;
        }
    }

    // Parse register name like "R1", "F2" to integer index
    private static int parseRegister(String reg) {
        reg = reg.trim().toUpperCase();
        if (reg.startsWith("R")) {
            return Integer.parseInt(reg.substring(1));
        } else if (reg.startsWith("F")) {
            return /* TODO: Configurable offset for FP registers */ Integer.parseInt(reg.substring(1));
        }
        throw new IllegalArgumentException("Invalid register: " + reg);
    }

    // Parse memory operand like "100(R2)" into [offset, baseReg]
    private static int[] parseMemoryOperand(String memOp) {
        memOp = memOp.trim();
        int openParen = memOp.indexOf('(');
        int closeParen = memOp.indexOf(')');

        if (openParen == -1 || closeParen == -1) {
            throw new IllegalArgumentException("Invalid memory operand: " + memOp);
        }

        int offset = Integer.parseInt(memOp.substring(0, openParen).trim());
        String baseRegStr = memOp.substring(openParen + 1, closeParen).trim();
        int baseReg = parseRegister(baseRegStr);

        return new int[] { offset, baseReg };
    }

    public static ArrayList<Instruction> parseFile(String Path) {
        ArrayList<String> lines = loadFile(Path);
        if (lines == null) {
            System.err.println("Failed to load file: " + Path);
            return new ArrayList<>(); // Return empty list instead of null
        }

        ArrayList<Instruction> instructions = new ArrayList<>();

        HashMap<String, Integer> labels = new HashMap<>();

        int i = 0;
        for (String line : lines) {
            Instruction instr = parseLine(line, labels, i++);
            if (instr != null) {
                instructions.add(instr);
            }
        }
        return instructions;
    }

    public static void main(String[] args) {
        InstructionParser parser = new InstructionParser();
        ArrayList<Instruction> instructions = InstructionParser.parseFile("src/parser/test.txt");

        for (Instruction instr : instructions) {
            System.out.println("Opcode: " + instr.getOpcode() +
                    ", DestReg: " + instr.getDestReg() +
                    ", Src1Reg: " + instr.getSrc1Reg() +
                    ", Src2Reg: " + instr.getSrc2Reg() +
                    ", BaseReg: " + instr.getBaseReg() +
                    ", Offset: " + instr.getOffset() +
                    ", Immediate: " + instr.getImmediate());
        }
    }

}
