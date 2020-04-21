package org.graalvm.wasm.mswasm;

public class Handle {
    private int base;
    private int offset;
    private int base;
    private int bound;
    private boolean isCorrupted;

    public Handle(int base, int offset, int bound, boolean isCorrupted) {
        this.base = base;
        this.offset = offset;
        this.bound = bound;
        this.isCorrupted = isCorrupted;
    }

    public Handle(Handle other) {
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
        if ( ! (obj instanceof Handle)) {
            return false;
        }

        Handle handle = (Handle) obj;
        return handle.getOffset() == offset && handle.getBase() == base && 
               handle.getBound() == bound;
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
    }
}