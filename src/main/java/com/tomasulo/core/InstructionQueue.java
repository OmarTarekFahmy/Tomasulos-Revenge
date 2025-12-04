package com.tomasulo.core;

import java.util.ArrayDeque;
import java.util.Queue;

public class InstructionQueue {

    private final Queue<Instruction> queue = new ArrayDeque<>();

    public void enqueue(Instruction instr) {
        queue.add(instr);
    }

    public Instruction peek() {
        return queue.peek();
    }

    /**
     * Remove head if we actually issue it this cycle.
     */
    public Instruction dequeue() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
    public int size() {
        return queue.size();
    }

    public java.util.List<Instruction> toList() {
        return new java.util.ArrayList<>(queue);
    }
}

