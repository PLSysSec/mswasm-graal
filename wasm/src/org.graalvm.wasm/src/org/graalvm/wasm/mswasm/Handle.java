package org.graalvm.wasm.mswasm;

public class Handle {
    private int base;
    private int offset;
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
        if (!(obj instanceof Handle)) {
            return false;
        }

        Handle handle = (Handle) obj;
        return handle.getOffset() == offset && handle.getBase() == base && handle.getBound() == bound;
    }

    @Override
    public int hashCode() {
        String eq = "" + base + " " + offset + " " + bound;
        return eq.hashCode();
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
        return !isCorrupted && base + offset <= bound;
    }

    public long[] toLongs() {
        long[] longHandle = new long[2];

        longHandle[0] = ((long) base << 32) | ((long) offset);
        longHandle[1] = ((long) bound << 32) | ((long) (isCorrupted ? 1 : 0));

        return longHandle;
    }

    public static Handle fromLongs(long[] values) {
        int base = (int) (values[0] >> 32);
        int offset = (int) values[0];
        int bound = (int) (values[1] >> 32);
        boolean isCorrupted = values[1] == 1;

        return new Handle(base, offset, bound, isCorrupted);
    }

    public Handle slice(int baseShift, int boundShift) {
        // TODO: Defensive coding on baseShift, boundShift
        Handle result = new Handle(base + baseShift, offset, bound - boundShift, false);
        return result;
    }

    public Handle shift(int shiftOffset) {
        // TODO: Defensive coding on shiftOffset
        Handle result = new Handle(base, offset + shiftOffset, bound, false);
        return result;
    }
}