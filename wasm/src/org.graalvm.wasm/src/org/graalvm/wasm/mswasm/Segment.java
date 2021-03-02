package org.graalvm.wasm.mswasm;

/**
 * Models a segment of memory
 */
public class Segment {
    private boolean isFree;

    public Segment() {
        isFree = false;
    }

    public void free() {
        // System.out.println("Setting segment freed");
        isFree = true;
    }

    public boolean isFree() {
        // System.out.println("Segment.isFree(): " + isFree);
        return isFree;
    }
}
