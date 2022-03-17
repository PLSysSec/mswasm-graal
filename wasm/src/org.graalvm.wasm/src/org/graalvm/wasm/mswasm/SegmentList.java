package org.graalvm.wasm.mswasm;

import java.util.Arrays;

import org.graalvm.wasm.mswasm.Segment;
import org.graalvm.wasm.mswasm.SegmentMemory;

public class SegmentList {
    private static final int INITIAL_CAPACITY = 11;

    protected Segment[] segments;
    protected int[] keys;
    private int size;

    public SegmentList() {
        segments = new Segment[INITIAL_CAPACITY];
        keys = new int[INITIAL_CAPACITY];
        size = 0;
    }

    public void insert(Segment s) {
        int idx = s.key() % segments.length;
        while (segments[idx] != null) {
            expandCapacity();
            idx = s.key() % segments.length;
        }
        segments[idx] = s;
        keys[idx] = s.key();
        ++size;
        if (SegmentMemory.DEBUG)
            System.err.println("[SegmentList.insert] Inserted " + s + " at index " + idx);
    }

    public void remove(int key) {
        int idx = key % segments.length;
        segments[idx] = null;
        keys[idx] = 0;
        --size;
        if (SegmentMemory.DEBUG)
            System.err.println("[SegmentList.remove] Removed segment " + key);
    }

    public Segment get(int key) {
        int idx = key % segments.length;
        return segments[idx];
    }

    public boolean contains(int key) {
        if (key == 0)
            return false;
        int idx = key % segments.length;
        return keys[idx] == key;
    }

    public int size() {
        return size;
    }

    public void clear() {
        if (size == 0)
            return;
        for (int i = 0; i < keys.length; i++) {
            keys[i] = 0;
            segments[i] = null;
        }
        size = 0;
        if (SegmentMemory.DEBUG)
            System.err.println("[SegmentList.clear] Cleared segments");
    }

    /**
     * Expand the capacity of the segment list and copy existing contents to the new lists.
     */
    private void expandCapacity() {
        int cap = segments.length;
        Segment[] newSegs;
        int[] newKeys;

        boolean success = false;
        do {
            cap = cap * 2 + 1;
            newSegs = new Segment[cap];
            newKeys = new int[cap];
            success = attemptToCopyValues(newSegs, newKeys);
            // Keep expanding until we succeed without conflicts
        } while (!success);

        segments = newSegs;
        keys = newKeys;
        if (SegmentMemory.DEBUG) {
            System.err.println("[SegmentList.expandCapacity] Expanded capacity to " + segments.length);
            System.err.println("[SegmentList.expandCapacity] segments: " + Arrays.toString(segments));
        }
    }

    private boolean attemptToCopyValues(Segment[] targetSegments, int[] targetKeys) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != 0) {
                int idx = keys[i] % targetKeys.length;
                if (targetKeys[idx] != 0) {
                    // Found a conflict
                    return false;
                }
                targetKeys[idx] = keys[i];
                targetSegments[idx] = segments[i];
            }
        }
        return true;
    }
}
