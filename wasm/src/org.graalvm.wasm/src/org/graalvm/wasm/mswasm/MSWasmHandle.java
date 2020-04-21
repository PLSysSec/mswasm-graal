package org.graalvm.wasm.mswasm;

public class MSWasmHandle {
    private int base;
    private int offset;
    private int bound;
    private boolean isCorrupted;

    public MSWasmHandle(int base, int offset, int bound, boolean isCorrupted) {
        this.base = base;
        this.offset = offset;
        this.bound = bound;
        this.isCorrupted = isCorrupted;
    }

    public MSWasmHandle(MSWasmHandle other) {
        this.base = other.getBase();
        this.offset = other.getOffset();
        this.bound = other.getBound();
        this.isCorrupted = other.isCorrupted();
    }

    @Override
    public String toString() {
        return "Handle: (" + base + ", " + offset + ", " + bound + ", " + isCorrupted + ")"; 
    }

    @Override
    public boolean equals(Object obj) {
        if ( ! (obj instanceof MSWasmHandle)) {
            return false;
        }

        MSWasmHandle handle = (MSWasmHandle) obj;
        return handle.getOffset() == offset && handle.getBase() == base && 
               handle.getBound() == bound;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public int getOffset() {
        return offset;
    }

    public int getBase() {
        return base;
    }

    public int getBound() {
        return bound;
    }

    public boolean isCorrupted() {
        return isCorrupted;
    }

    public void setCorrupted() {
        isCorrupted = true;
    }

    public boolean isValid() {
        return ! isCorrupted && base + offset <= bound && base + offset >= base;
    }

    /* MSWASM-TODO: remove?
    // Converts to long containing base & offset
    public long convertToLongBaseOffset() {
        return ((long)base << 32) | ((long)offset);
    }

    // Converts to long containing bound & isCorrupted
    public long convertToLongBoundIsC() {
        return ((long)bound << 32) | ((long)isCorrupted);
    }

    public static Handle retrieveFromLong(long baseOffset, long boundIsC) {
        int base = (int) (baseOffset >> 32);
        int offset = (int) baseOffset;
        int bound = (int) (boundIsC >> 32);
        boolean isCorrupted = (boolean) boundIsC;
        return new Handle(base, offset, bound, isCorrupted);
    }*/
}