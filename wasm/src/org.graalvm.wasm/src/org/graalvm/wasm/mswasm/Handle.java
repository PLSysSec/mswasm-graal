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

public class Handle {
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
    
    // segment model used to check if memory is free
    private Segment segment;

    // static mapping table for Handle keys
    private static HashMap<Long, Handle> keysToHandles = new HashMap<>();

    // memory access validation
    private final long base;
    private final long bound;
    private final long key;
    private int offset;

    // flags
    private boolean isCorrupted;
    private final boolean isSlice;

    // Manual constructor used to generate slices
    private Handle(Segment segment, long base, long bound, int offset, 
                   boolean isCorrupted, boolean isSlice) {
        this.segment = segment;

        this.base = base;
        this.bound = bound;
        this.offset = offset;

        // this.isCorrupted = isCorrupted;
        this.isSlice = isSlice;
        this.key = generateKey(this);
        keysToHandles.put(key, this);
        // System.out.println("[DEBUG] Manually defined segment with address = " + startAddress()
        //                    + ", new key = " + this.key + ": " + keysToHandles.containsKey(this.key));
    }

    /*
     * Allocate new segment
     */
    public Handle(int byteSize) {
        // define new segment
        this.segment = new Segment();

        // allocate memory
        this.base = unsafe.allocateMemory(byteSize); // start of allocation
        this.offset = 0; // where we begin looking at memory
        this.bound = this.base + byteSize;
        // unsafe.setMemory(this.base + this.offset, byteSize, (byte) 0);

        // set flags
        this.isCorrupted = false;
        this.isSlice = false;
        this.key = generateKey(this);
        keysToHandles.put(key, this);
        // System.out.println("[DEBUG] Created new segment of size " + byteSize + ", address = " + startAddress()
        //                    + ", key = " + this.key + ": " + keysToHandles.containsKey(this.key));
    }

    // Duplicate handle
    public Handle(Handle other) {
        this.segment = other.segment;

        this.base = other.base;
        this.offset = other.offset;
        this.bound = other.bound;
        // this.isCorrupted = other.isCorrupted;
        this.isSlice = other.isSlice;
        this.key = generateKey(this);
        keysToHandles.put(key, this);
        // System.out.println("[DEBUG] Duplicated segment with address = " + startAddress()
        //                    + ", new key = " + this.key + ": " + keysToHandles.containsKey(this.key));
    }


    public static Handle nullHandle() {
        // isCorrupted bit set, can never be dereferenced
        return new Handle(new Segment(), 0, 0, 0, true, false);
    }

    public static long handleToRawLongBits(Handle value) {
        // System.out.println("[DEBUG] Getting key " + value.key + " for handle with address " + value.startAddress());
        return value.key;
    }

    public static Handle longBitsToHandle(long key) {
        if (!keysToHandles.containsKey(key)) {
            // System.out.println("[DEBUG] Couldn't find key " + key + ", returning null handle");
            return nullHandle();
        }
        // System.out.println("[DEBUG] Found key " + key + ", returning handle with address " + keysToHandles.get(key).startAddress());
        return keysToHandles.get(key);
    }

    /**
     * Generate random key for the given handle. Returns key in
     *      (Integer.MIN_VALUE, Integer.MAX_VALUE)
     */
    @CompilerDirectives.TruffleBoundary
    public static long generateKey(Handle handle) {
        double rand = Math.random();
        return (long) (Long.MAX_VALUE * rand);
    }    
    
    public String toString() {
        return "Handle: (" + this.base + ", " + this.offset + ", " + this.bound + ", "
               + this.isCorrupted + ", " + this.isSlice + ")";
    }

    public long getKey() {
        return this.key;
    }

    public int getOffset() {
        return this.offset;
    }
    
    public void setOffset(Node node, int offset) {
        this.offset = offset;
    }

    public void validateHandleAccess(Node node, long accessSize) {
        if (this.isCorrupted) {
            throw trapCorrupted(node);
        } else
        if (this.segment.isFree()) {
            throw trapFreed(node);
        } else if (this.offset < 0 || this.base + this.offset + accessSize > this.bound) {
            throw trapOutOfBounds(node, accessSize);
        }
    }


    @TruffleBoundary
    protected final WasmException trapCorrupted(Node node) {
        long effectiveAddr = base + offset;
        final String message = String.format("Handle with effective address 0x%016X (%d) is corrupted",
                                    effectiveAddr, effectiveAddr);
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);

    }

    @TruffleBoundary
    protected final WasmException trapFreed(Node node) {
        long effectiveAddr = base + offset;
        final String message = String.format("Handle with effective address 0x%016X (%d) has been freed",
                                    effectiveAddr, effectiveAddr);
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }

    @TruffleBoundary
    protected final WasmException trapInvalidSlice(Node node) {
        long effectiveAddr = base + offset;
        final String message = String.format("Invalid use of slices with handle at 0x%016X (%d)",
                                    effectiveAddr, effectiveAddr);
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }
    
    @TruffleBoundary
    protected final WasmException trapOutOfBounds(Node node, long length) {
        final String message = String.format("%d-byte segment memory access at address 0x%016X (%d) is out-of-bounds",
                        length, base + offset, base + offset);
        return WasmException.create(Failure.OUT_OF_BOUNDS_MEMORY_ACCESS, node, message);
    }

    public long startAddress() {
        return this.base + this.offset;
    }

    public long byteSize() {
        return this.bound - this.base;
    }

    /**
     * Free memory associated with this handle. Slices cannot be freed; throw a trap
     * if this handle is a slice.
     */
    public void free(Node node) {
        if (this.isCorrupted) {
            throw trapCorrupted(node);
        } else if (this.isSlice) {
            throw trapInvalidSlice(node);
        } else if (this.segment.isFree()) {
            throw trapFreed(node);
        }

        unsafe.freeMemory(this.base);
        this.segment.free();
    }

    // Handle operations
    /**
     * Slices a handle to produce a smaller view into the segment.
     * 
     * @param sliceBaseOffset offset of the new base from the old base
     * @param sliceBoundOffset offset of the new bound form the old base
     */  
    public Handle slice(Node node, long baseOffset, long boundOffset) {
        // Validate slice offsets
        if (baseOffset < 0 || boundOffset < baseOffset ||
            this.base + baseOffset > this.bound) {
            throw trapInvalidSlice(node);
        }

        long resultBase = this.base + baseOffset;
        long resultBound = this.base + boundOffset;
        Handle result = new Handle(this.segment, resultBase, resultBound, 0, false, true);

        return result;
    }

    public void add(int addOffset) {
        this.offset += addOffset;
    }

    public void sub(int subOffset) {
        this.offset -= subOffset;
    }

    /*
     _________________________________________
    / Hi there! I see that you'd like to load \
    | data from a Handle. Would you like help |
    \ with that?                              /
    -----------------------------------------
    \
    \
        __
        /  \
        |  |
        @  @
        |  |
        || |/
        || ||
        |\_/|
        \___/
    */

    
    public int load_i32(Node node) {
        validateHandleAccess(node, 4);
        int value = unsafe.getInt(this.base + this.offset);
        return value;
    }

    
    public long load_i64(Node node) {
        validateHandleAccess(node, 8);
        long value = unsafe.getLong(this.base + this.offset);
        return value;
    }

    
    public float load_f32(Node node) {
        validateHandleAccess(node, 4);
        float value = unsafe.getFloat(this.base + this.offset);
        return value;
    }

    
    public double load_f64(Node node) {
        validateHandleAccess(node, 8);
        double value = unsafe.getDouble(this.base + this.offset);
        return value;
    }

    
    public int load_i32_8s(Node node) {
        validateHandleAccess(node, 1);
        int value = unsafe.getByte(this.base + this.offset);
        return value;
    }

    
    public int load_i32_8u(Node node) {
        validateHandleAccess(node, 1);
        int value = 0x0000_00ff & unsafe.getByte(this.base + this.offset);
        return value;
    }

    
    public int load_i32_16s(Node node) {
        validateHandleAccess(node, 2);
        int value = unsafe.getShort(this.base + this.offset);
        return value;
    }

    
    public int load_i32_16u(Node node) {
        validateHandleAccess(node, 2);
        int value = 0x0000_ffff & unsafe.getShort(this.base + this.offset);
        return value;
    }

    
    public long load_i64_8s(Node node) {
        validateHandleAccess(node, 1);
        long value = unsafe.getByte(this.base + this.offset);
        return value;
    }

    
    public long load_i64_8u(Node node) {
        validateHandleAccess(node, 1);
        long value = 0x0000_0000_0000_00ffL & unsafe.getByte(this.base + this.offset);
        return value;
    }

    
    public long load_i64_16s(Node node) {
        validateHandleAccess(node, 2);
        long value = unsafe.getShort(this.base + this.offset);
        return value;
    }

    
    public long load_i64_16u(Node node) {
        validateHandleAccess(node, 2);
        long value = 0x0000_0000_0000_ffffL & unsafe.getShort(this.base + this.offset);
        return value;
    }

    
    public long load_i64_32s(Node node) {
        validateHandleAccess(node, 4);
        long value = unsafe.getInt(this.base + this.offset);
        return value;
    }

    
    public long load_i64_32u(Node node) {
        validateHandleAccess(node, 4);
        long value = 0x0000_0000_ffff_ffffL & unsafe.getInt(this.base + this.offset);
        return value;
    }

    /**
     * Load a key corresponding to a handle. Use internal mapping to check whether that
     * handle is valid
     */
    public Handle load_handle(Node node) {
        validateHandleAccess(node, 4);

        // load key at address
        int key = unsafe.getInt(this.base + this.offset);

        // validate key
        if ( ! keysToHandles.containsKey(key)) {
            return nullHandle();
        }

        // return valid handle
        return keysToHandles.get(key);
    }


    
    public void store_i32(Node node, int value) {
        validateHandleAccess(node, 4);
        unsafe.putInt(this.base + this.offset, value);
    }

    
    public void store_i64(Node node, long value) {
        validateHandleAccess(node, 8);
        unsafe.putLong(this.base + this.offset, value);
    }

    
    public void store_f32(Node node, float value) {
        validateHandleAccess(node, 4);
        unsafe.putFloat(this.base + this.offset, value);
    }

    
    public void store_f64(Node node, double value) {
        validateHandleAccess(node, 8);
        unsafe.putDouble(this.base + this.offset, value);
    }

    
    public void store_i32_8(Node node, byte value) {
        validateHandleAccess(node, 1);
        unsafe.putByte(this.base + this.offset, value);
    }

    
    public void store_i32_16(Node node, short value) {
        validateHandleAccess(node, 2);
        unsafe.putShort(this.base + this.offset, value);
    }

    
    public void store_i64_8(Node node, byte value) {
        validateHandleAccess(node, 1);
        unsafe.putByte(this.base + this.offset, value);
    }

    
    public void store_i64_16(Node node, short value) {
        validateHandleAccess(node, 2);
        unsafe.putShort(this.base + this.offset, value);
    }

    
    public void store_i64_32(Node node, int value) {
        validateHandleAccess(node, 4);
        unsafe.putInt(this.base + this.offset, value);
    }

    public void store_handle(Node node, Handle value) {
        validateHandleAccess(node, 4);        
        unsafe.putLong(this.base + this.offset, value.getKey());
    }

}
