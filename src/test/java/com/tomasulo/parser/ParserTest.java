package com.tomasulo.parser;

import java.util.ArrayList;

import com.tomasulo.core.Instruction;

public class ParserTest {
    public static void main(String[] args) {
        // Create a temporary file
        String filename = "temp_test.txt";
        try {
            java.io.FileWriter fw = new java.io.FileWriter(filename);
            fw.write("DADDI R1, R0, 5\n");
            fw.write("DADDI R2, R1, 10\n");
            fw.close();

            ArrayList<Instruction> instructions = InstructionParser.parseFile(filename);
            for (Instruction instr : instructions) {
                System.out.println("Op: " + instr.getOpcode() + 
                                   ", Dest: " + instr.getDestReg() + 
                                   ", Src1: " + instr.getSrc1Reg() + 
                                   ", Imm: " + instr.getImmediate());
            }
            
            new java.io.File(filename).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
