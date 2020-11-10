package org.graalvm.wasm.mswasm;

import org.graalvm.wasm.memory.WasmMemory;
import org.graalvm.wasm.mswasm.Handle;

// Alternative segment memory, based on WasmMemory
public class SegmentWasmMemory {
    private WasmMemory memory;

    public SegmentWasmMemory(WasmMemory memory) {
        this.memory = memory.duplicate();
    }

    public long size() {
        return memory.byteSize();
    }

    // segSize: size of segment in bytes
    public Handle newSegment(int segSize) {
        long start = size();
        long end = start + segSize;
        memory.growToAddress(end);

        Handle view = new Handle(start, 0, end, false);

        // Handle result = new Handle(size, 0, size + segSize, false);

        // if ( ! segments.containsKey(result)) {
        //     segments.put(result, new SegmentValue(null, SegmentType.DATA_SEGMENT));
        //     size = size + segSize;
        // }

        return view;
    }

    public Handle shiftSegment(Handle handle, int offset) {
        // Handle result = new Handle(handle.getBase(), handle.getOffset() + offset,
        //                            handle.getBound(), false);
        // if ( ! segments.containsKey(result)) {
        //     segments.put(result, new SegmentValue(null, SegmentType.DATA_SEGMENT));
        // }
        // return result;
        return null;
    }

    public boolean freeSegment(Handle handle) {
        // if ( ! segments.containsKey(handle)) {
        //     return false;
        // }
        // segments.remove(handle);
        // return true;
        return false;
    }

    public Handle sliceSegment(Handle handle, int base, int bound) {
        // Handle result = new Handle(handle.getBase() + base, handle.getOffset(),
        //                            handle.getBound() - bound, false);

        // if ( ! segments.containsKey(result)) {
        //     segments.put(result, new SegmentValue(null, SegmentType.DATA_SEGMENT));
        // }
        // return result;
        return null;
    }

    // Return true iff store operation is successful. Prohibits null values
    public boolean storeToSegment(Handle handle, Object value) {
        // if ( ! handle.isValid() || value == null) {
        //     return false;
        // }

        // if ( ! segments.containsKey(handle)) {
        //     size = Math.max(size, handle.getBound());
        // }

        // if (value instanceof Handle) {
        //     segments.put(handle, new SegmentValue(value, SegmentType.HANDLE_SEGMENT));
        // } else {
        //     segments.put(handle, new SegmentValue(value, SegmentType.DATA_SEGMENT));
        // }

        // return true;
        return false;
    }

    // Return null if load operation is unsuccessful
    public Object loadFromSegment(Handle handle) {
        // if ( ! handle.isValid() || ! segments.containsKey(handle)) {
        //     return null;
        // }

        // SegmentValue storedValue = segments.get(handle);

        // if (storedValue.getValue() instanceof Handle && 
        //     storedValue.getType() == SegmentType.DATA_SEGMENT) {
        //     Handle result = (Handle)storedValue.getValue();
        //     result.setCorrupted();
        //     return result;
        // }

        // return storedValue.getValue();
        return null;
    }

    public enum SegmentType {
        DATA_SEGMENT, 
        HANDLE_SEGMENT
    }


}
