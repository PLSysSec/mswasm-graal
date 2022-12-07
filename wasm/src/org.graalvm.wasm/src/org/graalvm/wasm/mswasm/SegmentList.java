package org.graalvm.wasm.mswasm;

import java.util.Random;
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

    // For security, start all the keys at start
    // and shift them by shift
    private int start;
    private int shift;

    public SegmentList() {
        segments = new Segment[INITIAL_CAPACITY];
        size = 0;
        current = 1;
        Random rand = new Random();
        start = rand.nextInt(3);
        shift = rand.nextInt(10);
    }

    public void insert(Segment s) {
        int idx = convertKey(s.key());
        if (idx >= segments.length) {
            expandCapacity();
        }
        segments[idx] = s;
        ++size;
        ++current;
        if (SegmentMemory.DEBUG)
            System.err.println("[SegmentList.insert] Inserted " + s + " at index " + idx);
    }

    public int newKey() {
        return (current + start) << shift;
    }

    int convertKey(int key) {
        return (key >> shift) - start;
    }

    public void remove(int key) {
        int idx = convertKey(key);
        segments[idx] = null;
        --size;
        if (SegmentMemory.DEBUG)
            System.err.println("[SegmentList.remove] Removed segment " + key);
    }

    public Segment get(int key) {
        return segments[convertKey(key)];
    }

    public boolean contains(int key) {
        if (key == 0)
            return false;
        return segments[convertKey(key)] != null;
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
        Segment[] newSegs;

        newSegs = new Segment[segments.length*2];
        System.arraycopy(segments, 0, newSegs, 0, segments.length);

        segments = newSegs;
        if (SegmentMemory.DEBUG) {
            System.err.println("[SegmentList.expandCapacity] Expanded capacity to " + segments.length);
            System.err.println("[SegmentList.expandCapacity] segments: " + Arrays.toString(segments));
        }
    }
}
