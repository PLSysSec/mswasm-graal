package org.graalvm.wasm.mswasm;

import java.lang.reflect.Field;
import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.TruffleObject;
import sun.misc.Unsafe;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.wasm.exception.WasmTrap;
import org.graalvm.wasm.WasmTracing;

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
    private static HashMap<Integer, Handle> keysToHandles = new HashMap<>();

    // memory access validation
    private final long base;
    private final long bound;
    private final int key;
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
        // this.isCorrupted = false;
        this.isSlice = false;
        this.key = generateKey(this);
        keysToHandles.put(key, this);
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
    }


    public static Handle nullHandle() {
        // isCorrupted bit set, can never be dereferenced
        return new Handle(new Segment(), 0, 0, 0, true, false);
    }

    public static long handleToRawLongBits(Handle value) {
        return ((long)value.offset << 32) | value.key;
    }

    public static Handle longBitsToHandle(long value) {
        return keysToHandles.get((int)(value & 0xffffffffL));
    }

    /**
     * Generate random key for the given handle. Returns key in
     *      (Integer.MIN_VALUE, Integer.MAX_VALUE)
     */
    @CompilerDirectives.TruffleBoundary
    public static int generateKey(Handle handle) {
        double rand = Math.random();
        double signRand = Math.random();
        int unsKey = (int) (Integer.MAX_VALUE * rand);
        return signRand >= 0.5 ? unsKey : -1 * unsKey;
    }    
    
    public String toString() {
        return "Handle: (" + this.base + ", " + this.offset + ", " + this.bound + ", "
               + this.isCorrupted + ", " + this.isSlice + ")";
    }

    public int getKey() {
        return this.key;
    }

    public int getOffset() {
        return this.offset;
    }
    
    public void setOffset(Node node, int offset) {
        this.offset = offset;
    }

    public void validateHandleAccess(Node node, long accessSize) {
        // WasmTracing.trace("validating handle at 0x%016X (%d)", this.base + this.offset, this.base + this.offset);
        // if (this.isCorrupted) {
        //     trapCorrupted(node);
        // } else
        if (this.segment.isFree()) {
            trapFreed(node);
        } else if (this.offset < 0 || this.base + this.offset + accessSize > this.bound) {
            trapOutOfBounds(node, accessSize);
        }
    }

    

    // @CompilerDirectives.TruffleBoundary
    private void trapOutOfBounds(Node node, long accessSize) {
        // String message = String.format("%d-byte segment memory access at address 0x%016X (%d) is out-of-bounds (memory size %d bytes).",
        //                 accessSize, this.base + this.offset, this.base + this.offset, byteSize());
        System.out.println("trapOutOfBounds");
        String message = "Segment memory access of size " + accessSize + " is out-of-bounds";
        message = message + "\n\ton handle " + this;
        throw new WasmTrap(node, message);
    }

    // @CompilerDirectives.TruffleBoundary
    private void trapCorrupted(Node node) {
        // String message = String.format("Segment memory access at address 0x%016X (%d) is corrupted.",
        //                 this.base + this.offset, this.base + this.offset);
        String message = "Segment memory pointer is corrupted";
        throw new WasmTrap(node, message);
    }

    // @CompilerDirectives.TruffleBoundary
    private void trapFreed(Node node) {
        // String message = String.format("Segment memory at address 0x%016X (%d) is not allocated.",
        //                 this.base + this.offset, this.base + this.offset);
        String message = "Segment memory is not allocated";
        throw new WasmTrap(node, message);
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
            String message = "Corrupted handle can't be freed";
            throw new WasmTrap(node, message);
        } else if (this.isSlice) {
            String message = "Slices of handles can't be freed";
            throw new WasmTrap(node, message);
        } else if (this.segment.isFree()) {
            trapFreed(node);
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
            String message = "Slice base and/or bound offset is invalid";
            throw new WasmTrap(node, message);
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
        WasmTracing.trace("load.i32 address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);
        int value = unsafe.getInt(this.base + this.offset);
        WasmTracing.trace("load.i32 value = 0x%08X (%d)", value, value);
        return value;
    }

    
    public long load_i64(Node node) {
        WasmTracing.trace("load.i64 address = %d", this.base + this.offset);
        validateHandleAccess(node, 8);
        long value = unsafe.getLong(this.base + this.offset);
        WasmTracing.trace("load.i64 value = 0x%08X (%d)", value, value);
        return value;
    }

    
    public float load_f32(Node node) {
        WasmTracing.trace("load.f32 address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);
        float value = unsafe.getFloat(this.base + this.offset);
        WasmTracing.trace("load.f32 address = %d, value = 0x%08X (%f)", this.base + this.offset, Float.floatToRawIntBits(value), value);
        return value;
    }

    
    public double load_f64(Node node) {
        WasmTracing.trace("load.f64 address = %d", this.base + this.offset);
        validateHandleAccess(node, 8);
        double value = unsafe.getDouble(this.base + this.offset);
        WasmTracing.trace("load.f64 address = %d, value = 0x%016X (%f)", this.base + this.offset, Double.doubleToRawLongBits(value), value);
        return value;
    }

    
    public int load_i32_8s(Node node) {
        WasmTracing.trace("load.i32_8s address = %d", this.base + this.offset);
        validateHandleAccess(node, 1);
        int value = unsafe.getByte(this.base + this.offset);
        WasmTracing.trace("load.i32_8s value = 0x%02X (%d)", value, value);
        return value;
    }

    
    public int load_i32_8u(Node node) {
        WasmTracing.trace("load.i32_8u address = %d", this.base + this.offset);
        validateHandleAccess(node, 1);
        int value = 0x0000_00ff & unsafe.getByte(this.base + this.offset);
        WasmTracing.trace("load.i32_8u value = 0x%02X (%d)", value, value);
        return value;
    }

    
    public int load_i32_16s(Node node) {
        WasmTracing.trace("load.i32_16s address = %d", this.base + this.offset);
        validateHandleAccess(node, 2);
        int value = unsafe.getShort(this.base + this.offset);
        WasmTracing.trace("load.i32_16s value = 0x%04X (%d)", value, value);
        return value;
    }

    
    public int load_i32_16u(Node node) {
        WasmTracing.trace("load.i32_16u address = %d", this.base + this.offset );
        validateHandleAccess(node, 2);
        int value = 0x0000_ffff & unsafe.getShort(this.base + this.offset);
        WasmTracing.trace("load.i32_16u value = 0x%04X (%d)", value, value);
        return value;
    }

    
    public long load_i64_8s(Node node) {
        WasmTracing.trace("load.i64_8s address = %d", this.base + this.offset);
        validateHandleAccess(node, 1);
        long value = unsafe.getByte(this.base + this.offset);
        WasmTracing.trace("load.i64_8s value = 0x%02X (%d)", value, value);
        return value;
    }

    
    public long load_i64_8u(Node node) {
        WasmTracing.trace("load.i64_8u address = %d", this.base + this.offset);
        validateHandleAccess(node, 1);
        long value = 0x0000_0000_0000_00ffL & unsafe.getByte(this.base + this.offset);
        WasmTracing.trace("load.i64_8u value = 0x%02X (%d)", value, value);
        return value;
    }

    
    public long load_i64_16s(Node node) {
        WasmTracing.trace("load.i64_16s address = %d", this.base + this.offset);
        validateHandleAccess(node, 2);
        long value = unsafe.getShort(this.base + this.offset);
        WasmTracing.trace("load.i64_16s value = 0x%04X (%d)", value, value);
        return value;
    }

    
    public long load_i64_16u(Node node) {
        WasmTracing.trace("load.i64_16u address = %d", this.base + this.offset);
        validateHandleAccess(node, 2);
        long value = 0x0000_0000_0000_ffffL & unsafe.getShort(this.base + this.offset);
        WasmTracing.trace("load.i64_16u value = 0x%04X (%d)", value, value);
        return value;
    }

    
    public long load_i64_32s(Node node) {
        WasmTracing.trace("load.i64_32s address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);
        long value = unsafe.getInt(this.base + this.offset);
        WasmTracing.trace("load.i64_32s value = 0x%08X (%d)", value, value);
        return value;
    }

    
    public long load_i64_32u(Node node) {
        WasmTracing.trace("load.i64_32u address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);
        long value = 0x0000_0000_ffff_ffffL & unsafe.getInt(this.base + this.offset);
        WasmTracing.trace("load.i64_32u value = 0x%08X (%d)", value, value);
        return value;
    }

    /**
     * Load a key corresponding to a handle. Use internal mapping to check whether that
     * handle is valid
     */
    public Handle load_handle(Node node) {
        WasmTracing.trace("load.handle address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);

        // load key at address
        int key = unsafe.getInt(this.base + this.offset);
        WasmTracing.trace("load.handle key = 0x%08X (%d)", key, key);

        // validate key
        if ( ! keysToHandles.containsKey(key)) {
            // invalid key; throw a trap
            // TODO do we want to return a corrupted handle instead?

            // ideally, invalid key references a Handle that is corrupted
            // now that the key is corrupted, we don't know what Handle it refers to
            // when a handle is stored and modified, programmer expects that the modified Handle can be loaded
            // our representation - you modified that handle, it's actually a KEY, not the handle itself, can't reference
            // we want to postpone this trap until we dereference this handle
            return nullHandle();
        }

        // return valid handle
        return keysToHandles.get(key);
    }


    
    public void store_i32(Node node, int value) {
        WasmTracing.trace("store.i32 address = %d, value = 0x%08X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 4);
        unsafe.putInt(this.base + this.offset, value);
    }

    
    public void store_i64(Node node, long value) {
        WasmTracing.trace("store.i64 address = %d, value = 0x%016X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 8);
        unsafe.putLong(this.base + this.offset, value);
    }

    
    public void store_f32(Node node, float value) {
        WasmTracing.trace("store.f32 address = %d, value = 0x%08X (%f)", this.base + this.offset, Float.floatToRawIntBits(value), value);
        validateHandleAccess(node, 4);
        unsafe.putFloat(this.base + this.offset, value);
    }

    
    public void store_f64(Node node, double value) {
        WasmTracing.trace("store.f64 address = %d, value = 0x%016X (%f)", this.base + this.offset, Double.doubleToRawLongBits(value), value);
        validateHandleAccess(node, 8);
        unsafe.putDouble(this.base + this.offset, value);
    }

    
    public void store_i32_8(Node node, byte value) {
        WasmTracing.trace("store.i32_8 address = %d, value = 0x%02X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 1);
        unsafe.putByte(this.base + this.offset, value);
    }

    
    public void store_i32_16(Node node, short value) {
        WasmTracing.trace("store.i32_16 address = %d, value = 0x%04X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 2);
        unsafe.putShort(this.base + this.offset, value);
    }

    
    public void store_i64_8(Node node, byte value) {
        WasmTracing.trace("store.i64_8 address = %d, value = 0x%02X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 1);
        unsafe.putByte(this.base + this.offset, value);
    }

    
    public void store_i64_16(Node node, short value) {
        WasmTracing.trace("store.i64_16 address = %d, value = 0x%04X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 2);
        unsafe.putShort(this.base + this.offset, value);
    }

    
    public void store_i64_32(Node node, int value) {
        WasmTracing.trace("store.i64_32 address = %d, value = 0x%08X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 4);
        unsafe.putInt(this.base + this.offset, value);
    }

    public void store_handle(Node node, Handle value) {
        WasmTracing.trace("store.handle address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);        
        unsafe.putInt(this.base + this.offset, value.getKey());
    }

}