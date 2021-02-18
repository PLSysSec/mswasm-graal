// package org.graalvm.wasm.mswasm;

// import java.util.Map;
// import java.util.HashMap;

// public class SegmentMemory {
//     private Map<Handle,SegmentValue> segments;
//     int size;

//     public SegmentMemory() {
//         segments = new HashMap<>();
//         size = 0;
//     }

//     public int size() {
//         return size;
//     }

//     public Handle newSegment(int segSize) {
//         Handle result = new Handle(size, 0, size + segSize, false);

//         if ( ! segments.containsKey(result)) {
//             segments.put(result, new SegmentValue(null, SegmentType.DATA_SEGMENT));
//             size = size + segSize;
//         }

//         return result;
//     }

//     public Handle shiftSegment(Handle handle, int offset) {
//         Handle result = new Handle(handle.getBase(), handle.getOffset() + offset,
//                                    handle.getBound(), false);
//         if ( ! segments.containsKey(result)) {
//             segments.put(result, new SegmentValue(null, SegmentType.DATA_SEGMENT));
//         }
//         return result;
//     }

//     public boolean freeSegment(Handle handle) {
//         if ( ! segments.containsKey(handle)) {
//             return false;
//         }
//         segments.remove(handle);
//         return true;
//     }

//     public Handle sliceSegment(Handle handle, int base, int bound) {
//         Handle result = new Handle(handle.getBase() + base, handle.getOffset(),
//                                    handle.getBound() - bound, false);

//         if ( ! segments.containsKey(result)) {
//             segments.put(result, new SegmentValue(null, SegmentType.DATA_SEGMENT));
//         }
//         return result;
//     }

//     // Return true iff store operation is successful. Prohibits null values
//     public boolean storeToSegment(Handle handle, Object value) {
//         if ( ! handle.isValid() || value == null) {
//             return false;
//         }

//         if ( ! segments.containsKey(handle)) {
//             size = Math.max(size, handle.getBound());
//         }

//         if (value instanceof Handle) {
//             segments.put(handle, new SegmentValue(value, SegmentType.HANDLE_SEGMENT));
//         } else {
//             segments.put(handle, new SegmentValue(value, SegmentType.DATA_SEGMENT));
//         }

//         return true;
//     }

//     // Return null if load operation is unsuccessful
//     public Object loadFromSegment(Handle handle) {
//         if ( ! handle.isValid() || ! segments.containsKey(handle)) {
//             return null;
//         }

//         SegmentValue storedValue = segments.get(handle);

//         if (storedValue.getValue() instanceof Handle && 
//             storedValue.getType() == SegmentType.DATA_SEGMENT) {
//             Handle result = (Handle)storedValue.getValue();
//             result.setCorrupted();
//             return result;
//         }

//         return storedValue.getValue();
//     }

//     public enum SegmentType {
//         DATA_SEGMENT, 
//         HANDLE_SEGMENT
//     }
// }