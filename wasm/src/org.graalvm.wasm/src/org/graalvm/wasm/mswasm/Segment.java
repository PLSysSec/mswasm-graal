package org.graalvm.wasm.mswasm;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import java.util.Random;

public class Segment implements TruffleObject {
    @CompilationFinal private static final Random rand = new Random();

    @CompilationFinal protected final long memoryBase;
    @CompilationFinal protected final long memoryBound;

    // Used as a unique random key to track this segment.
    // We use only the lower 30 bits for keys to leave room for null and corrupted bits
    // in the 64-bit long representation of a handle.
    private final int key;

    public Segment(long base, long bound, int key) {
        this.memoryBase = base;
        this.memoryBound = bound;
        this.key = key;
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
