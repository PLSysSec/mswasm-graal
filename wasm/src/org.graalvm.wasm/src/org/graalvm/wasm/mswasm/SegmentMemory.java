package org.graalvm.wasm.mswasm;

import java.util.Map;
import java.util.HashMap;

public class SegmentMemory {
    private Map<Handle,SegmentValue> segments;

    public SegmentMemory() {
        segments = new HashMap<>();
    }

    // Return true iff store operation is successful. Prohibits null values
    public boolean storeToSegment(Handle handle, Object value) {
        if ( ! handle.isValid() || ! segments.containsKey(handle) || value == null) {
            return false;
        }

        if (value instanceof Handle) {
            segments.put(handle, new SegmentValue(value, SegmentType.HANDLE_SEGMENT));
        } else {
            segments.put(handle, new SegmentValue(value, SegmentType.DATA_SEGMENT));
        }

        return true;
    }

    // Return null if load operation is unsuccessful
    public Object loadFromSegment(Handle handle) {
        if ( ! handle.isValid() || ! segments.containsKey(handle)) {
            return null;
        }

        SegmentValue storedValue = segments.get(handle);

        if (storedValue.getValue() instanceof Handle && 
            storedValue.getType() == SegmentType.DATA_SEGMENT) {
            Handle result = (Handle)storedValue.getValue();
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