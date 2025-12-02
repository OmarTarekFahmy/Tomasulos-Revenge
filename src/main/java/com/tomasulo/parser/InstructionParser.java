package com.tomasulo.parser;

import com.tomasulo.core.Instruction;
import com.tomasulo.core.Instruction.Opcode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses MIPS-like assembly instructions into Instruction objects.
 */
public class InstructionParser {

    private static final int FP_BASE = 32; // F0 = 32, F1 = 33, etc.

    // Patterns for different instruction formats
    private static final Pattern R_TYPE = Pattern.compile(
            "(\\w+)\\s+(\\w+)\\s*,\\s*(\\w+)\\s*,\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern I_TYPE_REG = Pattern.compile(
            "(\\w+)\\s+(\\w+)\\s*,\\s*(\\w+)\\s*,\\s*(-?\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LOAD_STORE = Pattern.compile(
            "(\\w+)\\s+(\\w+)\\s*,\\s*(-?\\d+)\\s*\\(\\s*(\\w+)\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BRANCH = Pattern.compile(
            "(\\w+)\\s+(\\w+)\\s*,\\s*(\\w+)\\s*,\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern JUMP = Pattern.compile(
            "(J|JAL)\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Parse a single line of assembly into an Instruction.
     * @param line The assembly line (e.g., "ADD.D F0, F2, F4")
     * @return The parsed Instruction, or null if the line is empty/comment
     * @throws IllegalArgumentException if the instruction format is invalid
     */
    public Instruction parse(String line) {
        if (line == null) return null;

        // Remove comments and trim
        int commentIdx = line.indexOf('#');
        if (commentIdx >= 0) {
            line = line.substring(0, commentIdx);
        }
        line = line.trim();

        if (line.isEmpty()) return null;

        // Try each pattern
        Instruction instr = tryLoadStore(line);
        if (instr != null) return instr;

        instr = tryRType(line);
        if (instr != null) return instr;

        instr = tryITypeReg(line);
        if (instr != null) return instr;

        instr = tryJump(line);
        if (instr != null) return instr;

        throw new IllegalArgumentException("Cannot parse instruction: " + line);
    }

    private Instruction tryLoadStore(String line) {
        Matcher m = LOAD_STORE.matcher(line);
        if (!m.matches()) return null;

        String op = m.group(1).toUpperCase().replace(".", "_");
        String destOrSrc = m.group(2);
        int offset = Integer.parseInt(m.group(3));
        String base = m.group(4);

        Opcode opcode = parseOpcode(op);
        int reg = parseRegister(destOrSrc);
        int baseReg = parseRegister(base);

        if (opcode.name().startsWith("L_") || opcode.name().startsWith("LD") ||
            opcode == Opcode.LW || opcode == Opcode.LWU) {
            // Load: dest = reg
            return new Instruction(opcode, reg, -1, -1, baseReg, offset, 0);
        } else {
            // Store: src = reg
            return new Instruction(opcode, -1, reg, -1, baseReg, offset, 0);
        }
    }

    private Instruction tryRType(String line) {
        Matcher m = R_TYPE.matcher(line);
        if (!m.matches()) return null;

        String op = m.group(1).toUpperCase().replace(".", "_");
        String dest = m.group(2);
        String src1 = m.group(3);
        String src2 = m.group(4);

        // Check if src2 is immediate (not a register)
        if (src2.matches("-?\\d+")) {
            return null; // Let I_TYPE handle it
        }

        Opcode opcode = parseOpcode(op);
        int destReg = parseRegister(dest);
        int src1Reg = parseRegister(src1);
        int src2Reg = parseRegister(src2);

        return new Instruction(opcode, destReg, src1Reg, src2Reg, -1, 0, 0);
    }

    private Instruction tryITypeReg(String line) {
        Matcher m = I_TYPE_REG.matcher(line);
        if (!m.matches()) return null;

        String op = m.group(1).toUpperCase().replace(".", "_");
        String dest = m.group(2);
        String src = m.group(3);
        int imm = Integer.parseInt(m.group(4));

        Opcode opcode = parseOpcode(op);
        int destReg = parseRegister(dest);
        int srcReg = parseRegister(src);

        return new Instruction(opcode, destReg, srcReg, -1, -1, 0, imm);
    }

    private Instruction tryJump(String line) {
        Matcher m = JUMP.matcher(line);
        if (!m.matches()) return null;

        String op = m.group(1).toUpperCase();
        String target = m.group(2);

        Opcode opcode = op.equals("J") ? Opcode.J : Opcode.JAL;
        int addr = 0;
        try {
            addr = Integer.parseInt(target);
        } catch (NumberFormatException e) {
            // Could be a label - handle elsewhere
        }

        return new Instruction(opcode, -1, -1, -1, -1, addr, 0);
    }

    private Opcode parseOpcode(String op) {
        // Normalize opcode string
        op = op.replace(".", "_").replace("-", "_");
        try {
            return Opcode.valueOf(op);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown opcode: " + op);
        }
    }

    private int parseRegister(String reg) {
        reg = reg.toUpperCase().trim();

        if (reg.startsWith("F")) {
            // Floating-point register
            int num = Integer.parseInt(reg.substring(1));
            return FP_BASE + num;
        } else if (reg.startsWith("R")) {
            // Integer register
            return Integer.parseInt(reg.substring(1));
        } else if (reg.startsWith("$")) {
            // MIPS-style $0, $1, etc.
            return Integer.parseInt(reg.substring(1));
        } else {
            // Might be a number directly
            return Integer.parseInt(reg);
        }
    }
}
