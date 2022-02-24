package org.graalvm.wasm.mswasm;

public class Handle implements Comparable<Handle> {
    /** The integer key of the segment this handle can be used to access */
    protected int segment;
    /** The handle's offset into its segment. The address this handle points to is given by
     * <base address of segment> + <offset>.
     */
    protected int offset;
    protected boolean isNull;
    protected boolean isCorrupted;

    /**
     * Create a new handle into the given segment, with an offset of zero.
     */
    public Handle(int segment) {
        this.segment = segment;
    }

    public Handle(Handle h) {
        this.segment = h.segment;
        this.offset = h.offset;
        this.isNull = h.isNull;
        this.isCorrupted = h.isCorrupted;
    }

    /**
     * Generate a new handle into the same segment, with offset shifted by the given amount.
     * @param shift
     * @return
     */
    public Handle add(int shift) {
        Handle h = new Handle(this);
        h.offset += shift;
        return h;
    }

    public int getOffset() {
        return offset;
    }

    public static Handle nullHandle() {
        Handle h = new Handle(0);
        h.isNull = true;
        return h;
    }

    /**
     * Encode a handle as a long encoding its null/corrupted status, segment key, and offset.
     * @param h
     * @return
     */
    public static long handleToRawLongBits(Handle h) {
        long nullLong = h.isNull ? 1 : 0;
        long corrLong = h.isCorrupted ? 1 : 0;
        return (nullLong << 63) | (corrLong << 62) | ((long)h.segment << 32) | ((long)h.offset);
    }

    public static Handle longBitsToHandle(long key) {
        boolean isNull = (key & (1L << 63)) != 0;
        boolean isCorrupted = (key & (1L << 62)) != 0;
        int segment = (int)(key >>> 32);
        int offset = (int)key;

        Handle h = new Handle(segment);
        h.offset = offset;
        h.isNull = isNull;
        h.isCorrupted = isCorrupted;
        return h;
    }

    public boolean equals(Handle h) {
        return h.segment == this.segment &&
               h.offset == this.offset &&
               h.isNull == this.isNull &&
               h.isCorrupted == this.isCorrupted;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public int compareTo(Handle h) {
        return this.offset - h.offset;
    }

    @Override
    public String toString() {
        return String.format("Handle { segment: %d, offset: %d, null: %b, corrupted: %b }", 
            this.segment, this.offset, this.isNull, this.isCorrupted);
    }
}
