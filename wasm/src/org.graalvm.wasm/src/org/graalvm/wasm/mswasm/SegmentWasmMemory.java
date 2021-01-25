package org.graalvm.wasm.mswasm;

import org.graalvm.wasm.memory.WasmMemory;
import org.graalvm.wasm.mswasm.Handle;
import java.util.Set;
import com.oracle.truffle.api.nodes.Node;
import java.util.HashSet;

// Alternative segment memory, based on WasmMemory
public class SegmentWasmMemory {
    private WasmMemory memory;
    private Set<Handle> segmentKeys;

    public SegmentWasmMemory(WasmMemory memory) {
        this.memory = memory.duplicate();
        this.segmentKeys = new HashSet<>();
    }

    public long size() {
        return memory.byteSize();
    }

    // segSize: size of segment in bytes
    public Handle newSegment(int segSize) {
        long start = size();
        long end = start + segSize;

        // BADNESS: we're growing memory for every new segment
        // we really want to only grow memory as needed
        // need to deallocate or reuse memory for freed segments
        // TODO in freeSegment?
        memory.growToAddress(end);

        // TODO: examine this again (should a handle take a long?)
        Handle view = new Handle((int) start, 0, (int) end, false);
        segmentKeys.add(view);
        return view;
    }

    public Handle shiftSegment(Handle handle, int offset) {
        // To be removed
        return null;
    }

    public boolean freeSegment(Handle handle) {
        // if ( ! segments.containsKey(handle)) {
        // return false;
        // }
        // segments.remove(handle);
        // return true;
        return false;
    }

    public Handle sliceSegment(Handle handle, int base, int bound) {
        // To be removed
        return null;
    }

    // Return true iff store operation is successful. Prohibits null values
    public boolean storeToSegment(Handle handle, Object value) {
        // Potential optimization: split into store methods per-data type

        if (!handle.isValid() || value == null) {
            return false;
        }

        // if ( ! segments.containsKey(handle)) {
        // size = Math.max(size, handle.getBound());
        // }

        // if (value instanceof Handle) {
        // segments.put(handle, new SegmentValue(value, SegmentType.HANDLE_SEGMENT));
        // } else {
        // segments.put(handle, new SegmentValue(value, SegmentType.DATA_SEGMENT));
        // }

        // return true;
        return false;
    }

    // Return null if load operation is unsuccessful
    public Object loadFromSegment(Handle handle) {
        // Potential optimization: split into load methods per-data type

        if (!handle.isValid() || !segmentKeys.contains(handle)) {
            return null;
        }

        // SegmentValue storedValue = segments.get(handle);

        // if (storedValue.getValue() instanceof Handle &&
        // storedValue.getType() == SegmentType.DATA_SEGMENT) {
        // Handle result = (Handle)storedValue.getValue();
        // result.setCorrupted();
        // return result;
        // }

        // return storedValue.getValue();
        return null;
    }

    public Integer segment_load_i32(Node node, Handle handle) {
        // if (!handle.isValid() || !segmentKeys.contains(handle)) {
        // return null;
        // }

        // int value = memory.load_i32(handle.getBase() + handle.getOffset());
        // return value;
        return null; // TODO: don't do this
    }

    public long segment_load_i64(Node node, Handle handle) {
        return 0;
    }

    public float segment_load_f32(Node node, Handle handle) {
        return 0;
    }

    public double segment_load_f64(Node node, Handle handle) {
        return 0;
    }

    public int segment_load_i32_8s(Node node, Handle handle) {
        return 0;
    }

    public int segment_load_i32_8u(Node node, Handle handle) {
        return 0;
    }

    public int segment_load_i32_16s(Node node, Handle handle) {
        return 0;
    }

    public int segment_load_i32_16u(Node node, Handle handle) {
        return 0;
    }

    public long segment_load_i64_8s(Node node, Handle handle) {
        return 0;
    }

    public long segment_load_i64_8u(Node node, Handle handle) {
        return 0;
    }

    public long segment_load_i64_16s(Node node, Handle handle) {
        return 0;
    }

    public long segment_load_i64_16u(Node node, Handle handle) {
        return 0;
    }

    public long segment_load_i64_32s(Node node, Handle handle) {
        return 0;
    }

    public long segment_load_i64_32u(Node node, Handle handle) {
        return 0;
    }

    public boolean segment_store_i32(Node node, Handle handle, int value) {
        if (!handle.isValid() || !segmentKeys.contains(handle)) {
            return false;
        }

        memory.store_i32(node, handle.getBase() + handle.getOffset(), value);
        return true;
    }

    public boolean segment_store_i64(Node node, Handle handle, long value) {
        return false;
    }

    public boolean segment_store_f32(Node node, Handle handle, float value) {
        return false;
    }

    public boolean segment_store_f64(Node node, Handle handle, double value) {
        return false;
    }

    public boolean segment_store_i32_8(Node node, Handle handle, byte value) {
        return false;
    }

    public boolean segment_store_i32_16(Node node, Handle handle, short value) {
        return false;
    }

    public boolean segment_store_i64_8(Node node, Handle handle, byte value) {
        return false;
    }

    public boolean segment_store_i64_16(Node node, Handle handle, short value) {
        return false;
    }

    public boolean segment_store_i64_32(Node node, Handle handle, int value) {
        return false;
    }

    public enum SegmentType {
        DATA_SEGMENT, HANDLE_SEGMENT
    }
}
