package org.graalvm.wasm.mswasm;

public class Handle implements Comparable<Handle> {
    /** The integer key of the segment this handle can be used to access */
    protected int segment;
    /** The handle's offset into its segment. The address this handle points to is given by
     * <base address of segment> + <offset>.
     */
    protected int offset;

    /**
     * Create a new handle into the given segment, with an offset of zero.
     */
    public Handle(int segment) {
        this.segment = segment;
    }

    public Handle(Handle h) {
        this.segment = h.segment;
        this.offset = h.offset;
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

    public boolean isNull() {
        return segment == 0;
    }

    public int getOffset() {
        return offset;
    }

    public static Handle nullHandle() {
        Handle h = new Handle(0);
        return h;
    }

    /**
     * Encode a handle as a long encoding its null/corrupted status, segment key, and offset.
     * @param h
     * @return
     */
    public static long handleToRawLongBits(Handle h) {
        return ((long)h.segment << 32) | ((long)h.offset);
    }

    public static Handle longBitsToHandle(long key) {
        int segment = (int)(key >>> 32);
        int offset = (int)key;

        Handle h = new Handle(segment);
        h.offset = offset;
        return h;
    }

    public boolean equals(Handle h) {
        return h.segment == this.segment &&
               h.offset == this.offset;
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
        return String.format("Handle { segment: %d, offset: %d }", this.segment, this.offset);
    }
}
