package org.graalvm.wasm.mswasm;

public class Segment {
    protected final long memoryBase;
    protected final long memoryBound;
    private boolean isFree;

    // Used as a unique random key to track this segment.
    // We use only the lower 30 bits for keys to leave room for null and corrupted bits
    // in the 64-bit long representation of a handle.
    private final int key;
    private static final int MAX_KEY = 0x3fffffff;

    public Segment(long base, long bound) {
        this.memoryBase = base;
        this.memoryBound = bound;
        this.key = (int)(Math.random() * MAX_KEY);
    }

    public boolean isFree() {
        return isFree;
    }

    public void free() {
        isFree = true;
    }

    /**
     * Get an integer key corresponding to this segment
     */
    public int key() {
        return key;
    }

    @Override
    public String toString() {
        return String.format("Segment { base: %d, bound: %d, free: %b }",
            memoryBase, memoryBound, isFree);
    }
}