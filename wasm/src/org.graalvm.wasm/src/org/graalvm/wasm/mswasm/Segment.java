package org.graalvm.wasm.mswasm;

public class Segment {
    protected final long memoryBase;
    protected final long memoryBound;

    // Used as a unique random key to track this segment.
    // We use only the lower 30 bits for keys to leave room for null and corrupted bits
    // in the 64-bit long representation of a handle.
    private final int key;
    private static final int MIN_KEY = 1;
    private static final int MAX_KEY = Integer.MAX_VALUE;

    public Segment(long base, long bound) {
        this.memoryBase = base;
        this.memoryBound = bound;
        this.key = (int)(Math.random() * (MAX_KEY - MIN_KEY)) + MIN_KEY;
    }

    /**
     * Get an integer key corresponding to this segment
     */
    public int key() {
        return key;
    }

    @Override
    public String toString() {
        return String.format("Segment %d { base: %x, bound: %x }",
            key, memoryBase, memoryBound);
    }
}
