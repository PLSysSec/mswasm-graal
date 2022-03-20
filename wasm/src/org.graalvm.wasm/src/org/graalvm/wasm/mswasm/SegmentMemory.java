package org.graalvm.wasm.mswasm;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.graalvm.wasm.constants.Sizes.MAX_MEMORY_DECLARATION_SIZE;
import static org.graalvm.wasm.constants.Sizes.MAX_MEMORY_INSTANCE_SIZE;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.graalvm.wasm.collection.ByteArrayList;

import org.graalvm.wasm.exception.Failure;
import org.graalvm.wasm.exception.WasmException;
import org.graalvm.wasm.nodes.WasmNode;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import sun.misc.Unsafe;
import com.oracle.truffle.api.nodes.Node;

import org.graalvm.wasm.mswasm.*;
import org.graalvm.wasm.memory.WasmMemory;

public class SegmentMemory extends WasmMemory {
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_FINE = false;

    private static final long KEY_MASK = 0xffffffff_00000000L;
    private static final long OFFSET_MASK = 0x00000000_ffffffffL;
    private static final int KEY_SHIFT = 32;
    private static final int OFFSET_SHIFT = 0;

    private static final Unsafe unsafe;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }

    /** Stores segments according to their integer keys */
    private SegmentList segments;

    public SegmentMemory() {
        // Provide dummy values for WasmMemory constructor
        super(0, MAX_MEMORY_DECLARATION_SIZE, 0, MAX_MEMORY_INSTANCE_SIZE);
        segments = new SegmentList();
    }

    // Methods to allocate and free memory

    /**
     * Allocate a segment with the given size in bytes. Returns a handle that accesses that
     * segment with offset 0.
     */
    public long allocSegment(int byteSize) {
        // Allocate segment with byte size
        long base = unsafe.allocateMemory(byteSize);
        long bound = base + byteSize;
        Segment s = new Segment(base, bound);

        // Record segment and create handle
        segments.insert(s);
        if (DEBUG) {
            System.err.println("[allocSegment] Created segment " + s.key() + 
                               " of size " + byteSize);
            System.err.println("[allocSegment] segments: " + segments);
        }
        return (long)s.key() << KEY_SHIFT;
    }

    /**
     * Free the segment associated with the given handle. Traps if the handle is corrupted or
     * the segment is already freed.
     */
    public void freeSegment(Node node, long handle) {
        Segment seg = getAndValidateSegment(node, handle);

        // Safe to free the memory and the segment
        unsafe.freeMemory(seg.memoryBase);
        segments.remove(seg.key());
        if (DEBUG_FINE) {
            System.err.println("[freeSegment] Freed segment " + seg.key());
            System.err.println("[freeSegment] segments: " + segments);
        }
    }

    public static int getKey(long handle) {
        return (int)((handle & KEY_MASK) >>> KEY_SHIFT);
    }

    public static int getOffset(long handle) {
        return (int)((handle & OFFSET_MASK) >>> OFFSET_SHIFT);
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
    public Segment getAndValidateSegment(Node node, long handle) {
        if (DEBUG_FINE) {
            System.err.println(String.format("[getAndValidateSegment] called on handle %016x", handle));
        }

        int key = getKey(handle);
        if (key == 0) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw trapNull(node, handle);
        }

        if (!segments.contains(key)) {
            if (DEBUG) {
                System.err.println("[getAndValidateSegment] Couldn't find segment with key " + key);
                System.err.println("[getAndValidateSegment] segments: " + segments);
            }
            // If the segment does not exist, assume the handle is corrupted
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw trapCorrupted(node, handle);
        }

        Segment segment = segments.get(key);
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
    public long getAndValidateEffectiveAddress(Node node, long handle, long accessSize) {
        if (DEBUG_FINE) {
            System.err.println("[getAndValidateEffectiveAddress] called");
        }
        Segment s = getAndValidateSegment(node, handle);
        int offset = getOffset(handle);
        long effectiveAddr = s.memoryBase + offset;

        if (effectiveAddr >= s.memoryBound || effectiveAddr + accessSize > s.memoryBound) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
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
    protected final WasmException trapCorrupted(Node node, long handle) {
        final String message = String.format("Handle %016x references an invalid segment", handle);
        if (DEBUG) {
            System.err.println("[trapCorrupted] " + message + ". Printing stack trace...");
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
    protected final WasmException trapNull(Node node, long h) {
        final String message = String.format("Handle %016x is null", h);
        if (DEBUG) {
            System.err.println("[trapNull] " + message + ". Printing stack trace...");
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
            System.err.println("[trapCorrupted] " + message + ". Printing stack trace...");
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
            System.err.println("[trapOutOfBounds] " + message + ". Printing stack trace...");
            new Exception().printStackTrace();
        }
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }

    // Private methods for loading & storing different value types

    private int getInt(long addr) {
        return unsafe.getInt(addr);
    }

    private long getLong(long addr) {
        return unsafe.getLong(addr);
    }

    private float getFloat(long addr) {
        return unsafe.getFloat(addr);
    }

    private double getDouble(long addr) {
        return unsafe.getDouble(addr);
    }

    private short getShort(long addr) {
        return unsafe.getShort(addr);
    }

    private byte getByte(long addr) {
        return unsafe.getByte(addr);
    }

    private void putInt(long addr, int value) {
        unsafe.putInt(addr, value);
    }

    private void putLong(long addr, long value) {
        unsafe.putLong(addr, value);
    }

    private void putFloat(long addr, float value) {
        unsafe.putFloat(addr, value);
    }

    private void putDouble(long addr, double value) {
        unsafe.putDouble(addr, value);
    }

    private void putShort(long addr, short value) {
        unsafe.putShort(addr, value);
    }

    private void putByte(long addr, byte value) {
        unsafe.putByte(addr, value);
    }


    // Methods to load from a segment

    @Override
    public int load_i32(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);
        int value = getInt(addr);
        return value;
    }
    
    @Override
    public long load_i64(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 8);
        long value = getLong(addr);
        return value;
    }
    
    @Override
    public float load_f32(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);
        float value = getFloat(addr);
        return value;
    }
    
    @Override
    public double load_f64(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 8);
        double value = getDouble(addr);
        return value;
    }
    
    @Override
    public int load_i32_8s(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 1);
        int value = getByte(addr);
        return value;
    }
    
    public int load_i32_8u(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 1);
        int value = 0x0000_00ff & getByte(addr);
        return value;
    }
    
    public int load_i32_16s(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 2);
        int value = getShort(addr);
        return value;
    }
    
    public int load_i32_16u(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 2);
        int value = 0x0000_ffff & getShort(addr);
        return value;
    }
    
    public long load_i64_8s(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 1);
        long value = getByte(addr);
        return value;
    }
    
    public long load_i64_8u(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 1);
        long value = 0x0000_0000_0000_00ffL & getByte(addr);
        return value;
    }
    
    public long load_i64_16s(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 2);
        long value = getShort(addr);
        return value;
    }

    public long load_i64_16u(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 2);
        long value = 0x0000_0000_0000_ffffL & getShort(addr);
        return value;
    }
    
    public long load_i64_32s(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);
        long value = getInt(addr);
        return value;
    }
    
    public long load_i64_32u(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);
        long value = 0x0000_0000_ffff_ffffL & getInt(addr);
        return value;
    }

    /**
     * Load a handle from memory as a 64-bit long.
     */
    public long load_handle(Node node, long handle) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);
        long value = getLong(addr);
        return value;
    }


    // Methods to store data to segments
    
    public void store_i32(Node node, long handle, int value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);
        putInt(addr, value);
    }

    public void store_i64(Node node, long handle, long value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 8);
        putLong(addr, value);
    }
    
    public void store_f32(Node node, long handle, float value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);
        putFloat(addr, value);
    }
    
    public void store_f64(Node node, long handle, double value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 8);
        putDouble(addr, value);
    }

    public void store_i32_8(Node node, long handle, byte value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 1);
        putByte(addr, value);
    }
    
    public void store_i32_16(Node node, long handle, short value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 2);
        putShort(addr, value);
    }
    
    public void store_i64_8(Node node, long handle, byte value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 1);
        putByte(addr, value);
    }
    
    public void store_i64_16(Node node, long handle, short value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 2);
        putShort(addr, value);
    }

    public void store_i64_32(Node node, long handle, int value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);
        putInt(addr, value);
    }

    /**
     * Store a handle to memory as a 64-bit long.
     */
    public void store_handle(Node node, long handle, long value) {
        long addr = getAndValidateEffectiveAddress(node, handle, 4);        
        putLong(addr, value);
    }


    // Misc WasmMemory methods

    /** 
     * Copy should never be called with segment memory. Traps if invoked. 
     */
    @Override
    public void copy(Node node, int src, int dst, int n) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
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
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final String message = "Segment memory does not support growing memory";
        throw WasmException.create(Failure.INVALID_MSWASM_OPERATION, message);
    }

    /**
     * Frees all segments and empties the segment hashmap. (Equivalent to close().)
     */
    @Override
    public void reset() {
        for (Segment s : segments.segments) {
            if (s != null) {
                unsafe.freeMemory(s.memoryBase);
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
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final String message = "Segment memory cannot be converted to a byte buffer";
        throw WasmException.create(Failure.INVALID_MSWASM_OPERATION, message);
    }


    // String reading/writing methods
    
    /**
     * Reads the null-terminated UTF-8 string starting at {@code startOffset}.
     *
     * @param startOffset memory index of the first character
     * @param node a node indicating the location where this read occurred in the Truffle AST. It
     *            may be {@code null} to indicate that the location is not available.
     * @return the read {@code String}
     */
    @CompilerDirectives.TruffleBoundary
    public String readString(long startOffset, WasmNode node) {
        ByteArrayList bytes = new ByteArrayList();
        byte currentByte;
        long offset = startOffset;

        while ((currentByte = (byte) load_i32_8u(node, offset)) != 0) {
            bytes.add(currentByte);
            ++offset;
        }

        return new String(bytes.toArray(), StandardCharsets.UTF_8);
    }

    /**
     * Reads the UTF-8 string of length {@code length} starting at {@code startOffset}.
     *
     * @param startOffset memory index of the first character
     * @param length length of the UTF-8 string to read in bytes
     * @param node a node indicating the location where this read occurred in the Truffle AST. It
     *            may be {@code null} to indicate that the location is not available.
     * @return the read {@code String}
     */
    @CompilerDirectives.TruffleBoundary
    public final String readString(long startOffset, int length, Node node) {
        ByteArrayList bytes = new ByteArrayList();

        for (int i = 0; i < length; ++i) {
            bytes.add((byte) load_i32_8u(node, startOffset + i));
        }

        return new String(bytes.toArray(), StandardCharsets.UTF_8);
    }

    /**
     * Writes a Java String at offset {@code offset}.
     * <p>
     * The written string is encoded as UTF-8 and <em>not</em> terminated with a null character.
     *
     * @param node a node indicating the location where this write occurred in the Truffle AST. It
     *            may be {@code null} to indicate that the location is not available.
     * @param string the string to write
     * @param offset memory index where to write the string
     * @param length the maximum number of bytes to write, including the trailing null character
     * @return the number of bytes written, including the trailing null character
     */
    @CompilerDirectives.TruffleBoundary
    public final int writeString(Node node, String string, long offset, int length) {
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        int i = 0;
        for (; i < bytes.length && i < length; ++i) {
            store_i32_8(node, offset + i, bytes[i]);
        }
        return i;
    }

    public final int writeString(Node node, String string, long offset) {
        return writeString(node, string, offset, Integer.MAX_VALUE);
    }

}
