package org.graalvm.wasm.mswasm;

import java.util.Map;
import java.util.HashMap;

public class SegmentMemory {
    private Map<MSWasmHandle,SegmentValue> segments;
    int size;

    public SegmentMemory() {
        segments = new HashMap<>();
        size = 0;
    }

    public int size() {
        return size;
    }

    // Return true iff store operation is successful. Prohibits null values
    public boolean storeToSegment(MSWasmHandle handle, Object value) {
        if ( ! handle.isValid() || value == null) {
            return false;
        }

        if ( ! segments.containsKey(handle)) {
            size = Math.max(size, handle.getBound());
        }

        if (value instanceof MSWasmHandle) {
            segments.put(handle, new SegmentValue(value, SegmentType.HANDLE_SEGMENT));
        } else {
            segments.put(handle, new SegmentValue(value, SegmentType.DATA_SEGMENT));
        }

        return true;
    }

    // Return null if load operation is unsuccessful
    public Object loadFromSegment(MSWasmHandle handle) {
        if ( ! handle.isValid() || ! segments.containsKey(handle)) {
            return null;
        }

        SegmentValue storedValue = segments.get(handle);

        if (storedValue.getValue() instanceof MSWasmHandle && 
            storedValue.getType() == SegmentType.DATA_SEGMENT) {
            MSWasmHandle result = (MSWasmHandle)storedValue.getValue();
            result.setCorrupted();
            return result;
        }

        return storedValue.getValue();
    }

    public enum SegmentType {
        DATA_SEGMENT, 
        HANDLE_SEGMENT
    }
}