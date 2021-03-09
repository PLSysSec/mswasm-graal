package org.graalvm.wasm.mswasm;

import java.lang.reflect.Field;
import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives;
import sun.misc.Unsafe;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.wasm.exception.WasmTrap;
import org.graalvm.wasm.WasmTracing;

public class Handle {
    private final Unsafe unsafe;
    
    // segment model used to check if memory is free
    private Segment segment;

    // static mapping table for Handle keys
    private static HashMap<Integer, Handle> keysToHandles = new HashMap<>();

    // memory access validation
    private final long base;
    private final long bound;
    private long offset;

    // flags
    private boolean isCorrupted;
    private final boolean isSlice;

    // Manual constructor used to generate slices
    private Handle(Unsafe unsafe, Segment segment, long base, long bound, long offset, 
                   boolean isCorrupted, boolean isSlice) {
        this.unsafe = unsafe;
        this.segment = segment;

        this.base = base;
        this.bound = bound;
        this.offset = offset;

        this.isCorrupted = isCorrupted;
        this.isSlice = isSlice;
    }

    /*
     * Allocate new segment
     */
    public Handle(int byteSize) {
        // Get unsafe
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            this.unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // define new segment
        this.segment = new Segment();

        // allocate memory
        this.base = this.unsafe.allocateMemory(byteSize); // start of allocation
        this.offset = 0; // where we begin looking at memory
        this.bound = this.base + byteSize;
        this.unsafe.setMemory(startAddress(), byteSize, (byte) 0);

        // set flags
        this.isCorrupted = false;
        this.isSlice = false;
    }

    // Duplicate handle
    public Handle(Handle other) {
        this.unsafe = other.unsafe;
        this.segment = other.segment;

        this.base = other.base;
        this.offset = other.offset;
        this.bound = other.bound;
        this.isCorrupted = other.isCorrupted;
        this.isSlice = other.isSlice;
    }

    /**
     * Generate random key for the given handle. Returns key in
     *      (Integer.MIN_VALUE, Integer.MAX_VALUE)
     */
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

    public int getOffset() {
        return (int)this.offset;
    }

    
    public void validateHandle(Node node, long accessSize) {
        WasmTracing.trace("validating handle at 0x%016X (%d)", startAddress(), startAddress());
        if (this.isCorrupted) {
            trapCorrupted(node);
        } else if (this.segment.isFree()) {
            trapFreed(node);
        } else if (this.segment.isFree() || this.offset < 0 || startAddress() + accessSize > this.bound) {
            trapOutOfBounds(node, accessSize);
        }
    }

    @CompilerDirectives.TruffleBoundary
    private void trapOutOfBounds(Node node, long accessSize) {
        // String message = String.format("%d-byte segment memory access at address 0x%016X (%d) is out-of-bounds (memory size %d bytes).",
        //                 accessSize, startAddress(), startAddress(), byteSize());
        String message = "Segment memory access is out-of-bounds";
        throw new WasmTrap(node, message);
    }

    @CompilerDirectives.TruffleBoundary
    private void trapCorrupted(Node node) {
        // String message = String.format("Segment memory access at address 0x%016X (%d) is corrupted.",
        //                 startAddress(), startAddress());
        String message = "Segment memory pointer is corrupted";
        throw new WasmTrap(node, message);
    }

    @CompilerDirectives.TruffleBoundary
    private void trapFreed(Node node) {
        // String message = String.format("Segment memory at address 0x%016X (%d) is not allocated.",
        //                 startAddress(), startAddress());
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
        if (this.isSlice) {
            String message = "Slices of handles can't be freed";
            throw new WasmTrap(node, message);
        } else if (this.segment.isFree()) {
            trapFreed(node);
        }

        unsafe.freeMemory(this.base);
        this.segment.free();
    }

    // Handle operations
    public Handle slice(long sliceBaseOffset, long sliceBoundOffset) {
        // TODO throw a trap if result base is out of bounds instead of silently failing

        // this all relies on this.base being an address, this.bound being an offset,
        // and both the parameters to slice being an offset.

        long resultBase = 0; // default error val, should change to throw trap
        if (this.base + sliceBaseOffset <= this.base + this.bound) { // ADDRESS validation check
            resultBase = this.base + sliceBaseOffset;
        }
        
        long resultBound = 0; // default error val, should change to throw trap
        if (resultBase + sliceBoundOffset <= this.base + this.bound) { // ADDRESS validation check
            resultBound = sliceBoundOffset;
        }

        Handle result = new Handle(this.unsafe, this.segment, resultBase, resultBound, 0, false, true);
        return result;
    }

    public void add(long addOffset) {
        this.offset += addOffset;
    }

    public void sub(long subOffset) {
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
        WasmTracing.trace("load.i32 address = %d", startAddress());
        validateHandle(node, 4);
        int value = this.unsafe.getInt(startAddress());
        WasmTracing.trace("load.i32 value = 0x%08X (%d)", value, value);
        return value;
    }

    
    public long load_i64(Node node) {
        WasmTracing.trace("load.i64 address = %d", startAddress());
        validateHandle(node, 8);
        long value = this.unsafe.getLong(startAddress());
        WasmTracing.trace("load.i64 value = 0x%08X (%d)", value, value);
        return value;
    }

    
    public float load_f32(Node node) {
        WasmTracing.trace("load.f32 address = %d", startAddress());
        validateHandle(node, 4);
        float value = this.unsafe.getFloat(startAddress());
        WasmTracing.trace("load.f32 address = %d, value = 0x%08X (%f)", startAddress(), Float.floatToRawIntBits(value), value);
        return value;
    }

    
    public double load_f64(Node node) {
        WasmTracing.trace("load.f64 address = %d", startAddress());
        validateHandle(node, 8);
        double value = unsafe.getDouble(startAddress());
        WasmTracing.trace("load.f64 address = %d, value = 0x%016X (%f)", startAddress(), Double.doubleToRawLongBits(value), value);
        return value;
    }

    
    public int load_i32_8s(Node node) {
        WasmTracing.trace("load.i32_8s address = %d", startAddress());
        validateHandle(node, 1);
        int value = this.unsafe.getByte(startAddress());
        WasmTracing.trace("load.i32_8s value = 0x%02X (%d)", value, value);
        return value;
    }

    
    public int load_i32_8u(Node node) {
        WasmTracing.trace("load.i32_8u address = %d", startAddress());
        validateHandle(node, 1);
        int value = 0x0000_00ff & this.unsafe.getByte(startAddress());
        WasmTracing.trace("load.i32_8u value = 0x%02X (%d)", value, value);
        return value;
    }

    
    public int load_i32_16s(Node node) {
        WasmTracing.trace("load.i32_16s address = %d", startAddress());
        validateHandle(node, 2);
        int value = this.unsafe.getShort(startAddress());
        WasmTracing.trace("load.i32_16s value = 0x%04X (%d)", value, value);
        return value;
    }

    
    public int load_i32_16u(Node node) {
        WasmTracing.trace("load.i32_16u address = %d", startAddress() );
        validateHandle(node, 2);
        int value = 0x0000_ffff & this.unsafe.getShort(startAddress());
        WasmTracing.trace("load.i32_16u value = 0x%04X (%d)", value, value);
        return value;
    }

    
    public long load_i64_8s(Node node) {
        WasmTracing.trace("load.i64_8s address = %d", startAddress());
        validateHandle(node, 1);
        long value = this.unsafe.getByte(startAddress());
        WasmTracing.trace("load.i64_8s value = 0x%02X (%d)", value, value);
        return value;
    }

    
    public long load_i64_8u(Node node) {
        WasmTracing.trace("load.i64_8u address = %d", startAddress());
        validateHandle(node, 1);
        long value = 0x0000_0000_0000_00ffL & this.unsafe.getByte(startAddress());
        WasmTracing.trace("load.i64_8u value = 0x%02X (%d)", value, value);
        return value;
    }

    
    public long load_i64_16s(Node node) {
        WasmTracing.trace("load.i64_16s address = %d", startAddress());
        validateHandle(node, 2);
        long value = this.unsafe.getShort(startAddress());
        WasmTracing.trace("load.i64_16s value = 0x%04X (%d)", value, value);
        return value;
    }

    
    public long load_i64_16u(Node node) {
        WasmTracing.trace("load.i64_16u address = %d", startAddress());
        validateHandle(node, 2);
        long value = 0x0000_0000_0000_ffffL & this.unsafe.getShort(startAddress());
        WasmTracing.trace("load.i64_16u value = 0x%04X (%d)", value, value);
        return value;
    }

    
    public long load_i64_32s(Node node) {
        WasmTracing.trace("load.i64_32s address = %d", startAddress());
        validateHandle(node, 4);
        long value = this.unsafe.getInt(startAddress());
        WasmTracing.trace("load.i64_32s value = 0x%08X (%d)", value, value);
        return value;
    }

    
    public long load_i64_32u(Node node) {
        WasmTracing.trace("load.i64_32u address = %d", startAddress());
        validateHandle(node, 4);
        long value = 0x0000_0000_ffff_ffffL & this.unsafe.getInt(startAddress());
        WasmTracing.trace("load.i64_32u value = 0x%08X (%d)", value, value);
        return value;
    }

    /**
     * Load a key corresponding to a handle. Use internal mapping to check whether that
     * handle is valid
     */
    public Handle load_handle(Node node) {
        WasmTracing.trace("load.handle address = %d", startAddress());
        validateHandle(node, 4);

        // load key at address
        int key = this.unsafe.getInt(startAddress());
        WasmTracing.trace("load.handle key = 0x%08X (%d)", key, key);

        // validate key
        if ( ! keysToHandles.containsKey(key)) {
            // invalid key; throw a trap
            // TODO do we want to return a corrupted handle instead?
            String message = "Corrupted key does not reference a valid handle";
            throw new WasmTrap(node, message);
        }

        // return valid handle
        return keysToHandles.get(key);
    }


    
    public void store_i32(Node node, int value) {
        WasmTracing.trace("store.i32 address = %d, value = 0x%08X (%d)", startAddress(), value, value);
        validateHandle(node, 4);
        unsafe.putInt(startAddress(), value);
    }

    
    public void store_i64(Node node, long value) {
        WasmTracing.trace("store.i64 address = %d, value = 0x%016X (%d)", startAddress(), value, value);
        validateHandle(node, 8);
        unsafe.putLong(startAddress(), value);
    }

    
    public void store_f32(Node node, float value) {
        WasmTracing.trace("store.f32 address = %d, value = 0x%08X (%f)", startAddress(), Float.floatToRawIntBits(value), value);
        validateHandle(node, 4);
        unsafe.putFloat(startAddress(), value);
    }

    
    public void store_f64(Node node, double value) {
        WasmTracing.trace("store.f64 address = %d, value = 0x%016X (%f)", startAddress(), Double.doubleToRawLongBits(value), value);
        validateHandle(node, 8);
        unsafe.putDouble(startAddress(), value);
    }

    
    public void store_i32_8(Node node, byte value) {
        WasmTracing.trace("store.i32_8 address = %d, value = 0x%02X (%d)", startAddress(), value, value);
        validateHandle(node, 1);
        unsafe.putByte(startAddress(), value);
    }

    
    public void store_i32_16(Node node, short value) {
        WasmTracing.trace("store.i32_16 address = %d, value = 0x%04X (%d)", startAddress(), value, value);
        validateHandle(node, 2);
        unsafe.putShort(startAddress(), value);
    }

    
    public void store_i64_8(Node node, byte value) {
        WasmTracing.trace("store.i64_8 address = %d, value = 0x%02X (%d)", startAddress(), value, value);
        validateHandle(node, 1);
        unsafe.putByte(startAddress(), value);
    }

    
    public void store_i64_16(Node node, short value) {
        WasmTracing.trace("store.i64_16 address = %d, value = 0x%04X (%d)", startAddress(), value, value);
        validateHandle(node, 2);
        unsafe.putShort(startAddress(), value);
    }

    
    public void store_i64_32(Node node, int value) {
        WasmTracing.trace("store.i64_32 address = %d, value = 0x%08X (%d)", startAddress(), value, value);
        validateHandle(node, 4);
        unsafe.putInt(startAddress(), value);
    }

    public void store_handle(Node node, Handle value) {
        WasmTracing.trace("store.handle address = %d", startAddress());
        validateHandle(node, 4);

        // add handle to key table before storing
        int key = generateKey(value);
        keysToHandles.put(key, value);
        
        unsafe.putInt(startAddress(), key);
    }


    // 
    // public boolean equals(Object obj) {
    // if (!(obj instanceof Handle)) {
    // return false;
    // }

    // Handle handle = (Handle) obj;
    // return handle.getOffset() == offset && handle.getBase() == base &&
    // handle.getBound() == bound;
    // }

    // 
    // public int hashCode() {
    // String eq = "" + base + " " + offset + " " + bound;
    // return eq.hashCode();
    // }

    // public int getBase() {
    //     return base;
    // }

    // public int getBound() {
    //     return bound;
    // }

    // public boolean isCorrupted() {
    //     return isCorrupted;
    // }

    // public void setCorrupted() {
    //     isCorrupted = true;
    // }

    // public boolean isValid() {
    //     return !isCorrupted && base + offset <= bound;
    // }

    // public long[] toLongs() {
    //     long[] longHandle = new long[2];

    //     longHandle[0] = ((long) base << 32) | ((long) offset);
    //     longHandle[1] = ((long) bound << 32) | ((long) (isCorrupted ? 1 : 0));

    //     return longHandle;
    // }

    // public static Handle fromLongs(long[] values) {
    //     int base = (int) (values[0] >> 32);
    //     int offset = (int) values[0];
    //     int bound = (int) (values[1] >> 32);
    //     boolean isCorrupted = values[1] == 1;

    //     return new Handle(base, offset, bound, isCorrupted);
    // }

    /*
     * public Handle slice(int baseShift, int boundShift) { // TODO: Defensive
     * coding on baseShift, boundShift Handle result = new Handle(base + baseShift,
     * offset, bound - boundShift, false); return result; }
     */

    /*
     * public Handle shift(int shiftOffset) { // TODO: Defensive coding on
     * shiftOffset Handle result = new Handle(base, offset + shiftOffset, bound,
     * false); return result; }
     */
}