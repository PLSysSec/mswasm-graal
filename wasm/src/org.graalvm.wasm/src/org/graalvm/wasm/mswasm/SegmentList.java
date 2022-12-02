package org.graalvm.wasm.mswasm;

import java.util.Arrays;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import org.graalvm.wasm.mswasm.Segment;
import org.graalvm.wasm.mswasm.SegmentMemory;

public class SegmentList implements TruffleObject {
    private static final int INITIAL_CAPACITY = 8;

    protected Segment[] segments;
    private int size;
    public int current;

    public SegmentList() {
        segments = new Segment[INITIAL_CAPACITY];
        size = 0;
        current = 1;
    }

    public void insert(Segment s) {

        int idx = s.key();
        if (idx >= segments.length) {
            expandCapacity();
        }
        segments[idx] = s;
        ++size;
        ++current;
        if (SegmentMemory.DEBUG)
            System.err.println("[SegmentList.insert] Inserted " + s + " at index " + idx);
    }

    public void remove(int key) {
        int idx = key % segments.length;
        segments[idx] = null;
        --size;
        if (SegmentMemory.DEBUG)
            System.err.println("[SegmentList.remove] Removed segment " + key);
    }

    public Segment get(int key) {
        return segments[key];
    }

    public boolean contains(int key) {
        if (key == 0)
            return false;
        return segments[key] != null;
    }

    public int size() {
        return size;
    }

    @ExplodeLoop
    public void clear() {
        if (size == 0)
            return;
        for (int i = 0; i < segments.length; i++) {
            segments[i] = null;
        }
        size = 0;
        current = 1;
        if (SegmentMemory.DEBUG)
            System.err.println("[SegmentList.clear] Cleared segments");
    }

    /**
     * Expand the capacity of the segment list and copy existing contents to the new lists.
     */
    private void expandCapacity() {
        int cap = segments.length;
        Segment[] newSegs;

        cap = cap * 2;
        newSegs = new Segment[cap];
        attemptToCopyValues(newSegs);

        segments = newSegs;
        if (SegmentMemory.DEBUG) {
            System.err.println("[SegmentList.expandCapacity] Expanded capacity to " + segments.length);
            System.err.println("[SegmentList.expandCapacity] segments: " + Arrays.toString(segments));
        }
    }

    private boolean attemptToCopyValues(Segment[] targetSegments) {
        for (int i = 0; i < segments.length; i++) {
            targetSegments[i] = segments[i];
        }
        return true;
    }
}
