package org.graalvm.wasm.mswasm;

import java.lang.reflect.Field;
import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.TruffleObject;
import sun.misc.Unsafe;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.wasm.exception.WasmTrap;
import org.graalvm.wasm.WasmTracing;

public class Handle {
    // segment model used to check if memory is free
    private Segment segment;

    // memory access validation
    private final int key;
    private final long base;
    private final long bound;
    private int offset;

    // flags
    private boolean isCorrupted;
    private final boolean isSlice;

    /*
     * Allocate new segment
     */
    public Handle(long base, int byteSize) {
        // define new segment
        this.segment = new Segment();

        // allocate memory
        this.base = base // start of allocation
        this.offset = 0; // where we begin looking at memory
        this.bound = base + byteSize;

        // set flags
        this.key = SegmentMemory.initializeKeyForHandle(this);
    }

    // Duplicate handle
    public Handle(Handle other) {
        this(other.segment, other.base, other.offset, other.bound,
            other.isCorrupted, other.isSlice);
        this.key = SegmentMemory.initializeKeyForHandle(this);
    }

    // Manual constructor used to generate slices
    private Handle(Segment segment, long base, long bound, int offset, 
                   boolean isCorrupted, boolean isSlice) {
        this.segment = segment;
        this.base = base;
        this.bound = bound;
        this.offset = offset;
        this.isCorrupted = isCorrupted;
        this.isSlice = isSlice;
        this.key = SegmentMemory.initializeKeyForHandle(this);
    }


    public static Handle nullHandle() {
        // isCorrupted bit set, can never be dereferenced
        return new Handle(new Segment(), 0, 0, 0, true, false);
    }  
    
    public String toString() {
        return "handle(" + this.base + ", " + this.offset + ", " + this.bound + ", "
               + this.isCorrupted + ")";
    }

    public int getKey() {
        return this.key;
    }

    public int getOffset() {
        return this.offset;
    }
    
    public void setOffset(Node node, int offset) {
        this.offset = offset;
    }

    public long getEffectiveAddress() {
        return this.base + this.offset;
    }

    public boolean isCorrupted() {
        return isCorrupted;
    }

    public void setCorrupted() {
        isCorrupted = false;
    }

    public boolean isSlice() {
        return isSlice;
    }

    public long byteSize() {
        return this.bound - this.base;
    }

    public boolean isFree() {
        return this.segment.isFree();
    }

    public void free() {
        this.segment.free();
    }

    public void add(int addOffset) {
        this.offset += addOffset;
    }

    public void sub(int subOffset) {
        this.offset -= subOffset;
    }
}