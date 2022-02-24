package org.graalvm.wasm.mswasm;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.graalvm.wasm.constants.Sizes.MAX_MEMORY_DECLARATION_SIZE;
import static org.graalvm.wasm.constants.Sizes.MAX_MEMORY_INSTANCE_SIZE;

import java.nio.ByteBuffer;

import org.graalvm.wasm.exception.Failure;
import org.graalvm.wasm.exception.WasmException;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import sun.misc.Unsafe;
import com.oracle.truffle.api.nodes.Node;

import org.graalvm.wasm.mswasm.*;
import org.graalvm.wasm.memory.WasmMemory;

public class SegmentMemory extends WasmMemory {
    public static final boolean DEBUG = false;

    private static final Unsafe unsafe;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Stores segments according to their integer keys */
    private HashMap<Integer, Segment> segments;

    public SegmentMemory() {
        // Provide dummy values for WasmMemory constructor
        super(0, MAX_MEMORY_DECLARATION_SIZE, 0, MAX_MEMORY_INSTANCE_SIZE);
        segments = new HashMap<>();
    }

    // Methods to allocate and free memory

    /**
     * Allocate a segment with the given size in bytes. Returns a handle that accesses that
     * segment with offset 0.
     */
    public Handle allocSegment(int byteSize) {
        if (DEBUG) {
            System.err.println("\n[allocSegment] called");
        }
        // Allocate segment with byte size
        long base = unsafe.allocateMemory(byteSize);
        long bound = base + byteSize;
        Segment s = new Segment(base, bound);

        // Record segment and create handle
        segments.put(s.key(), s);
        if (DEBUG) {
            System.err.println("[allocSegment] Created segment " + s.key() + 
                               " of size " + byteSize);
            System.err.println("[allocSegment] segments: " + segments);
        }
        return new Handle(s.key());
    }

    /**
     * Free the segment associated with the given handle. Traps if the handle is corrupted or
     * the segment is already freed.
     */
    public void freeSegment(Node node, Handle h) {
        if (DEBUG) {
            System.err.println("\n[freeSegment] called");
        }
        Segment seg = getAndValidateSegment(node, h);

        // Safe to free the memory and the segment
        unsafe.freeMemory(seg.memoryBase);
        seg.free();
        if (DEBUG) {
            System.err.println("[freeSegment] Freed segment " + seg.key());
            System.err.println("[freeSegment] segments: " + segments);
        }
    }


    // Methods to validate attempts to use a handle

    /**
     * Retrieve the segment referenced by the given handle, validating
     * 
     *   (1) the handle is neither null nor corrupted;
     *   (2) the segment exists; and
     *   (3) the segment has not been freed.
     * 
     * Traps if any of these conditions are violated. Otherwise, returns the segment.
     */
    public Segment getAndValidateSegment(Node node, Handle h) {
        if (DEBUG) {
            System.err.println("[getAndValidateSegment] called on " + h);
        }
        if (h.isNull) {
            throw trapNull(node, h);
        }

        int key = h.segment;
        if (!segments.containsKey(key)) {
            if (DEBUG) {
                System.err.println("[getAndValidateSegment] Couldn't find segment with key " + key);
                System.err.println("[getAndValidateSegment] segments: " + segments);
            }
            // If the segment does not exist, assume the handle is corrupted
            throw trapCorrupted(node, h);
        }

        Segment segment = segments.get(key);
        if (segment.isFree()) {
            throw trapFreed(node, key);
        }

        return segment;
    }

    /**
     * Calculate the effective address accessed by the handle with the given size in bytes,
     * where the effective address is (the base address of the segment) + (the handle offset).
     * Validates that
     * 
     *  (1) the handle references a valid segment;
     *  (2) the effective address is within bounds for the segment; and
     *  (3) (the effective address) + (the access size) is also within bounds for the segment.
     * 
     * If either condition is validated, traps. Otherwise, returns the address.
     */
    public long getAndValidateEffectiveAddress(Node node, Handle h, long accessSize) {
        if (DEBUG) {
            System.err.println("\n[getAndValidateEffectiveAddress] called");
        }
        Segment s = getAndValidateSegment(node, h);
        long effectiveAddr = s.memoryBase + h.offset;

        if (effectiveAddr >= s.memoryBound || effectiveAddr + accessSize >= s.memoryBound) {
            throw trapOutOfBounds(node, accessSize, effectiveAddr);
        }
        return effectiveAddr;
    }

    /**
     * Trap on an attempt to dereference a corrupted handle.
     * @param node
     * @return
     */
    @TruffleBoundary
    protected final WasmException trapCorrupted(Node node, Handle h) {
        final String message = String.format("Handle into segment %d with offset %d is corrupted",
                                             h.segment, h.offset);
        if (DEBUG) {
            System.out.println("[trapCorrupted] " + message + ". Printing stack trace...");
            new Exception().printStackTrace();
        }
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }

    /**
     * Trap on an attempt to dereference a null handle.
     * @param node
     * @return
     */
    @TruffleBoundary
    protected final WasmException trapNull(Node node, Handle h) {
        final String message = String.format("Handle into segment %d with offset %d is null",
                                             h.segment, h.offset);
        if (DEBUG) {
            System.out.println("[trapNull] " + message + ". Printing stack trace...");
            new Exception().printStackTrace();
        }
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }

    /**
     * Trap on an attempt to access a freed segment of memory.
     * @param node
     * @return
     */
    @TruffleBoundary
    protected final WasmException trapFreed(Node node, int segmentKey) {
        final String message = String.format("Segment with key %d has already been freed",
                                    segmentKey);
        if (DEBUG) {
            System.out.println("[trapCorrupted] " + message + ". Printing stack trace...");
            new Exception().printStackTrace();
        }
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }
    
    /**
     * Trap on an attempt to store to or load from memory out-of-bounds.
     */
    @TruffleBoundary
    protected final WasmException trapOutOfBounds(Node node, long length, long address) {
        final String message = String.format("%d-byte segment memory access at address 0x%016X (%d) is out-of-bounds",
                        length, address, address);
        if (DEBUG) {
            System.out.println("[trapOutOfBounds] " + message + ". Printing stack trace...");
            new Exception().printStackTrace();
        }
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }


    // Methods to load from a segment

    @Override
    public int load_i32(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        int value = unsafe.getInt(addr);
        return value;
    }
    
    @Override
    public long load_i64(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 8);
        long value = unsafe.getLong(addr);
        return value;
    }
    
    @Override
    public float load_f32(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        float value = unsafe.getFloat(addr);
        return value;
    }
    
    @Override
    public double load_f64(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 8);
        double value = unsafe.getDouble(addr);
        return value;
    }
    
    @Override
    public int load_i32_8s(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        int value = unsafe.getByte(addr);
        return value;
    }
    
    public int load_i32_8u(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        int value = 0x0000_00ff & unsafe.getByte(addr);
        return value;
    }
    
    public int load_i32_16s(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        int value = unsafe.getShort(addr);
        return value;
    }
    
    public int load_i32_16u(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        int value = 0x0000_ffff & unsafe.getShort(addr);
        return value;
    }
    
    public long load_i64_8s(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        long value = unsafe.getByte(addr);
        return value;
    }
    
    public long load_i64_8u(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        long value = 0x0000_0000_0000_00ffL & unsafe.getByte(addr);
        return value;
    }
    
    public long load_i64_16s(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        long value = unsafe.getShort(addr);
        return value;
    }

    public long load_i64_16u(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        long value = 0x0000_0000_0000_ffffL & unsafe.getShort(addr);
        return value;
    }
    
    public long load_i64_32s(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        long value = unsafe.getInt(addr);
        return value;
    }
    
    public long load_i64_32u(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        long value = 0x0000_0000_ffff_ffffL & unsafe.getInt(addr);
        return value;
    }

    /**
     * Load a handle from memory as a 64-bit long.
     */
    public Handle load_handle(Node node, long handle) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        long value = unsafe.getLong(addr);
        return Handle.longBitsToHandle(value);
    }


    // Methods to store data to segments
    
    public void store_i32(Node node, long handle, int value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        unsafe.putInt(addr, value);
    }

    public void store_i64(Node node, long handle, long value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 8);
        unsafe.putLong(addr, value);
    }
    
    public void store_f32(Node node, long handle, float value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        unsafe.putFloat(addr, value);
    }
    
    public void store_f64(Node node, long handle, double value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 8);
        unsafe.putDouble(addr, value);
    }

    public void store_i32_8(Node node, long handle, byte value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        unsafe.putByte(addr, value);
    }
    
    public void store_i32_16(Node node, long handle, short value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        unsafe.putShort(addr, value);
    }
    
    public void store_i64_8(Node node, long handle, byte value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        unsafe.putByte(addr, value);
    }
    
    public void store_i64_16(Node node, long handle, short value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        unsafe.putShort(addr, value);
    }

    public void store_i64_32(Node node, long handle, int value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        unsafe.putInt(addr, value);
    }

    /**
     * Store a handle to memory as a 64-bit long.
     */
    public void store_handle(Node node, long handle, Handle value) {
        Handle h = Handle.longBitsToHandle(handle);
        long addr = getAndValidateEffectiveAddress(node, h, 4);        
        unsafe.putLong(addr, Handle.handleToRawLongBits(value));
    }


    // Misc WasmMemory methods

    /** 
     * Copy should never be called with segment memory. Traps if invoked. 
     */
    @Override
    public void copy(Node node, int src, int dst, int n) {
        final String message = "Segment memory does not support copying memory";
        throw WasmException.create(Failure.INVALID_MSWASM_OPERATION, message);
    }

    /** Returns number of segments in the segment memory. */
    @Override
    public int size() {
        return segments.size();
    }

    /** Placeholder. Returns max int. */
    @Override
    public long byteSize() {
        return Integer.MAX_VALUE;
    }

    /** 
     * Grow should never be called with segment memory. Traps if invoked.
     */
    @Override
    public boolean grow(int extraPageSize) {
        final String message = "Segment memory does not support growing memory";
        throw WasmException.create(Failure.INVALID_MSWASM_OPERATION, message);
    }

    /**
     * Frees all segments and empties the segment hashmap. (Equivalent to close().)
     */
    @Override
    public void reset() {
        for (Segment s : segments.values()) {
            if (!s.isFree()) {
                unsafe.freeMemory(s.memoryBase);
                s.free();
            }
        }
        segments.clear();
    }

    /**
     * Creates a "new" segment memory instance that references the same segment map as this
     * segment memory.
     */
    @Override
    public WasmMemory duplicate() {
        SegmentMemory memory = new SegmentMemory();
        memory.segments = this.segments;
        return memory;
    }

    /**
     * Frees all segments and empties the segment hashmap. (Equivalent to reset().)
     */
    @Override
    public void close() {
        reset();
    }

    /**
     * Segment memory cannot be converted to a byte buffer. Traps if invoked.
     */
    @Override
    public ByteBuffer asByteBuffer() {
        final String message = "Segment memory cannot be converted to a byte buffer";
        throw WasmException.create(Failure.INVALID_MSWASM_OPERATION, message);
    }

}
