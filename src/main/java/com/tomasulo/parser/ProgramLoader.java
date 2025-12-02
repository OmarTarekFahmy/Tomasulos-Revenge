package com.tomasulo.parser;

import com.tomasulo.core.Instruction;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a program from a file.
 */
public class ProgramLoader {

    private final InstructionParser parser = new InstructionParser();

    /**
     * Load instructions from a file.
     * @param filePath Path to the assembly file
     * @return List of parsed Instructions
     * @throws IOException if file cannot be read
     */
    public List<Instruction> load(String filePath) throws IOException {
        List<Instruction> program = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                try {
                    Instruction instr = parser.parse(line);
                    if (instr != null) {
                        program.add(instr);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Line " + lineNum + ": " + e.getMessage());
                }
            }
        }

        return program;
    }

    /**
     * Load instructions from a string (multiple lines).
     * @param programText The program text
     * @return List of parsed Instructions
     */
    public List<Instruction> loadFromString(String programText) {
        List<Instruction> program = new ArrayList<>();
        String[] lines = programText.split("\\r?\\n");

        int lineNum = 0;
        for (String line : lines) {
            lineNum++;
            try {
                Instruction instr = parser.parse(line);
                if (instr != null) {
                    program.add(instr);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Line " + lineNum + ": " + e.getMessage());
            }
        }

        return program;
    }
}
