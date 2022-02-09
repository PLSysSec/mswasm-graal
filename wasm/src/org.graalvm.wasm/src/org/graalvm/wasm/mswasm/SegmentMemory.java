package org.graalvm.wasm.mswasm;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.graalvm.wasm.exception.Failure;
import org.graalvm.wasm.exception.WasmException;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import sun.misc.Unsafe;
import com.oracle.truffle.api.nodes.Node;

import org.graalvm.wasm.mswasm.*;

public class SegmentMemory {
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
    private static HashMap<Integer, Segment> segments = new HashMap<>();


    // Methods to allocate and free memory

    /**
     * Allocate a segment with the given size in bytes. Returns a handle that accesses that
     * segment with offset 0.
     */
    public Handle allocSegment(long byteSize) {
        // Allocate segment with byte size
        long base = unsafe.allocateMemory(byteSize);
        long bound = base + byteSize;
        Segment s = new Segment(base, bound);

        // Record segment and create handle
        segments.put(s.key(), s);
        return new Handle(s.key());
    }

    /**
     * Free the segment associated with the given handle. Traps if the handle is corrupted or
     * the segment is already freed.
     */
    public void freeSegment(Node node, Handle h) {
        Segment seg = getAndValidateSegment(node, h);

        // Safe to free the memory and the segment
        unsafe.freeMemory(seg.memoryBase);
        seg.free();
    }


    // Methods to validate attempts to use a handle

    /**
     * Retrieve the segment referenced by the given handle, validating
     * 
     *   (1) the handle is not corrupted;
     *   (2) the segment exists; and
     *   (3) the segment has not been freed.
     * 
     * Traps if any of these conditions are violated. Otherwise, returns the segment.
     */
    public Segment getAndValidateSegment(Node node, Handle h) {
        if (h.isCorrupted) {
            throw trapCorrupted(node, h);
        }
        
        int key = h.segment;
        if (!segments.containsKey(key)) {
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
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }
    
    /**
     * Trap on an attempt to store to or load from memory out-of-bounds.
     */
    @TruffleBoundary
    protected final WasmException trapOutOfBounds(Node node, long length, long address) {
        final String message = String.format("%d-byte segment memory access at address 0x%016X (%d) is out-of-bounds",
                        length, address, address);
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }


    // Methods to load from a segment

    public int load_i32(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        int value = unsafe.getInt(addr);
        return value;
    }
    
    public long load_i64(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 8);
        long value = unsafe.getLong(addr);
        return value;
    }
    
    public float load_f32(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        float value = unsafe.getFloat(addr);
        return value;
    }
    
    public double load_f64(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 8);
        double value = unsafe.getDouble(addr);
        return value;
    }
    
    public int load_i32_8s(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        int value = unsafe.getByte(addr);
        return value;
    }
    
    public int load_i32_8u(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        int value = 0x0000_00ff & unsafe.getByte(addr);
        return value;
    }
    
    public int load_i32_16s(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        int value = unsafe.getShort(addr);
        return value;
    }
    
    public int load_i32_16u(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        int value = 0x0000_ffff & unsafe.getShort(addr);
        return value;
    }
    
    public long load_i64_8s(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        long value = unsafe.getByte(addr);
        return value;
    }
    
    public long load_i64_8u(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        long value = 0x0000_0000_0000_00ffL & unsafe.getByte(addr);
        return value;
    }
    
    public long load_i64_16s(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        long value = unsafe.getShort(addr);
        return value;
    }

    public long load_i64_16u(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        long value = 0x0000_0000_0000_ffffL & unsafe.getShort(addr);
        return value;
    }
    
    public long load_i64_32s(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        long value = unsafe.getInt(addr);
        return value;
    }
    
    public long load_i64_32u(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        long value = 0x0000_0000_ffff_ffffL & unsafe.getInt(addr);
        return value;
    }

    public Handle load_handle(Node node, Handle h) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        int key = unsafe.getInt(addr);
        return Handle.longBitsToHandle(key);
    }


    // Methods to store data to segments
    
    public void store_i32(Node node, Handle h, int value) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        unsafe.putInt(addr, value);
    }

    public void store_i64(Node node, Handle h, long value) {
        long addr = getAndValidateEffectiveAddress(node, h, 8);
        unsafe.putLong(addr, value);
    }
    
    public void store_f32(Node node, Handle h, float value) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        unsafe.putFloat(addr, value);
    }
    
    public void store_f64(Node node, Handle h, double value) {
        long addr = getAndValidateEffectiveAddress(node, h, 8);
        unsafe.putDouble(addr, value);
    }

    public void store_i32_8(Node node, Handle h, byte value) {
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        unsafe.putByte(addr, value);
    }
    
    public void store_i32_16(Node node, Handle h, short value) {
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        unsafe.putShort(addr, value);
    }
    
    public void store_i64_8(Node node, Handle h, byte value) {
        long addr = getAndValidateEffectiveAddress(node, h, 1);
        unsafe.putByte(addr, value);
    }
    
    public void store_i64_16(Node node, Handle h, short value) {
        long addr = getAndValidateEffectiveAddress(node, h, 2);
        unsafe.putShort(addr, value);
    }

    public void store_i64_32(Node node, Handle h, int value) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);
        unsafe.putInt(addr, value);
    }

    public void store_handle(Node node, Handle h, Handle value) {
        long addr = getAndValidateEffectiveAddress(node, h, 4);        
        unsafe.putLong(addr, Handle.handleToRawLongBits(h));
    }

}