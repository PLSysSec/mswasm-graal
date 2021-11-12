package org.graalvm.wasm.mswasm;

import java.lang.reflect.Field;
import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.TruffleObject;
import sun.misc.Unsafe;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.wasm.exception.WasmTrap;
import org.graalvm.wasm.WasmTracing;

/** MSWasm segment memory model. Backed by Java's Unsafe 
 */
public class SegmentMemory extends WasmMemory {

    private static final Unsafe unsafe;
    static {
        try {
            final Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }

    // We store handles as long keys in the stack and track the key-handle mapping here
    private static HashMap<Long, Handle> handles = new HashMap<>();

    public SegmentMemory() {
        // We don't care about the size parameters kept by WasmMemory
        super(0, 0, 0, 0);
    }

    /** Generate random key for the given handle and store it in our mapping
     */
    public static long initializeKeyForHandle(Handle handle) {
        double rand = Math.random();
        long key = (long) (0xffffffff_ffffffffL * rand);
        handles.put(key, handle);
        return key;
    }

    public static Handle getHandleForKey(long key) {
        return handles.get(key);
    }
    
    public String toString() {
        return "Segment memory with handles [" + keysToHandles + "]";
    }

    public Handle newSegment(Node node, int byteSize) {
        long base = unsafe.allocateMemory(byteSize);
        return new Handle(base, byteSize);
    }

    /**
     * Free memory associated with this handle. Slices cannot be freed; throw a trap
     * if this handle is a slice.
     */
    public void freeSegment(Node node, Handle address) {
        if (address.isCorrupted()) {
            String message = "Corrupted handle can't be freed";
            throw new WasmTrap(node, message);
        } else if (address.isSlice()) {
            String message = "Slices of handles can't be freed";
            throw new WasmTrap(node, message);
        } else if (address.isFree()) {
            trapFreed(node);
        }

        unsafe.freeMemory(this.base);
        address.free();
    }

    /**
     * Slices a handle to produce a smaller view into the segment.
     * 
     * @param sliceBaseOffset offset of the new base from the old base
     * @param sliceBoundOffset offset of the new bound form the old base
     */  
    public Handle slice(Node node, Handle oldAddr, long baseOffset, long boundOffset) {
        // Validate slice offsets
        if (baseOffset < 0 || boundOffset < baseOffset ||
            oldAddr.base + baseOffset > oldAddr.bound) {
            String message = "Slice base and/or bound offset is invalid";
            throw new WasmTrap(node, message);
        }

        long newBase = oldAddr.base + baseOffset;
        long newBound = oldAddr.base + boundOffset;
        return new Handle(oldAddr.segment, newBase, newBound, 0, false, true);
    }

    // Validation

    public void validateHandleAccess(Node node, Handle address, long accessSize) {
        // WasmTracing.trace("validating handle at 0x%016X (%d)", this.base + this.offset, this.base + this.offset);
        if (address.isCorrupted()) {
            trapCorrupted(node, address);
        } else if (address.isFree()) {
            trapFreed(node, address);
        } else if (address.getOffset() < 0 || address.getEffectiveAddress() + accessSize >= this.bound) {
            trapOutOfBounds(node, address, accessSize);
        }
    }
 
    private void trapOutOfBounds(Node node, Handle address, long accessSize) {
        // String message = String.format("%d-byte segment memory access at address 0x%016X (%d) is out-of-bounds (memory size %d bytes).",
        //                 accessSize, this.base + this.offset, this.base + this.offset, byteSize());
        System.out.println("trapOutOfBounds");
        String message = "Segment memory access of size " + accessSize + " is out-of-bounds on " + address;
        throw new WasmTrap(node, message);
    }

    private void trapCorrupted(Node node, Handle address) {
        // String message = String.format("Segment memory access at address 0x%016X (%d) is corrupted.",
        //                 this.base + this.offset, this.base + this.offset);
        String message = "Segment memory pointer " + address + " is corrupted";
        throw new WasmTrap(node, message);
    }

    private void trapFreed(Node node, Handle address) {
        // String message = String.format("Segment memory at address 0x%016X (%d) is not allocated.",
        //                 this.base + this.offset, this.base + this.offset);
        String message = "Segment memory at " + address + " is not allocated";
        throw new WasmTrap(node, message);
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

    @Override
    public int load_i32(Node node) {
        WasmTracing.trace("load.i32 address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);
        int value = unsafe.getInt(this.base + this.offset);
        WasmTracing.trace("load.i32 value = 0x%08X (%d)", value, value);
        return value;
    }

    @Override
    public long load_i64(Node node) {
        WasmTracing.trace("load.i64 address = %d", this.base + this.offset);
        validateHandleAccess(node, 8);
        long value = unsafe.getLong(this.base + this.offset);
        WasmTracing.trace("load.i64 value = 0x%08X (%d)", value, value);
        return value;
    }

    @Override    
    public float load_f32(Node node) {
        WasmTracing.trace("load.f32 address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);
        float value = unsafe.getFloat(this.base + this.offset);
        WasmTracing.trace("load.f32 address = %d, value = 0x%08X (%f)", this.base + this.offset, Float.floatToRawIntBits(value), value);
        return value;
    }

    @Override 
    public double load_f64(Node node) {
        WasmTracing.trace("load.f64 address = %d", this.base + this.offset);
        validateHandleAccess(node, 8);
        double value = unsafe.getDouble(this.base + this.offset);
        WasmTracing.trace("load.f64 address = %d, value = 0x%016X (%f)", this.base + this.offset, Double.doubleToRawLongBits(value), value);
        return value;
    }

    @Override 
    public int load_i32_8s(Node node) {
        WasmTracing.trace("load.i32_8s address = %d", this.base + this.offset);
        validateHandleAccess(node, 1);
        int value = unsafe.getByte(this.base + this.offset);
        WasmTracing.trace("load.i32_8s value = 0x%02X (%d)", value, value);
        return value;
    }

    @Override 
    public int load_i32_8u(Node node) {
        WasmTracing.trace("load.i32_8u address = %d", this.base + this.offset);
        validateHandleAccess(node, 1);
        int value = 0x0000_00ff & unsafe.getByte(this.base + this.offset);
        WasmTracing.trace("load.i32_8u value = 0x%02X (%d)", value, value);
        return value;
    }

    @Override 
    public int load_i32_16s(Node node) {
        WasmTracing.trace("load.i32_16s address = %d", this.base + this.offset);
        validateHandleAccess(node, 2);
        int value = unsafe.getShort(this.base + this.offset);
        WasmTracing.trace("load.i32_16s value = 0x%04X (%d)", value, value);
        return value;
    }

    @Override 
    public int load_i32_16u(Node node) {
        WasmTracing.trace("load.i32_16u address = %d", this.base + this.offset );
        validateHandleAccess(node, 2);
        int value = 0x0000_ffff & unsafe.getShort(this.base + this.offset);
        WasmTracing.trace("load.i32_16u value = 0x%04X (%d)", value, value);
        return value;
    }

    @Override 
    public long load_i64_8s(Node node) {
        WasmTracing.trace("load.i64_8s address = %d", this.base + this.offset);
        validateHandleAccess(node, 1);
        long value = unsafe.getByte(this.base + this.offset);
        WasmTracing.trace("load.i64_8s value = 0x%02X (%d)", value, value);
        return value;
    }

    @Override 
    public long load_i64_8u(Node node) {
        WasmTracing.trace("load.i64_8u address = %d", this.base + this.offset);
        validateHandleAccess(node, 1);
        long value = 0x0000_0000_0000_00ffL & unsafe.getByte(this.base + this.offset);
        WasmTracing.trace("load.i64_8u value = 0x%02X (%d)", value, value);
        return value;
    }

    @Override 
    public long load_i64_16s(Node node) {
        WasmTracing.trace("load.i64_16s address = %d", this.base + this.offset);
        validateHandleAccess(node, 2);
        long value = unsafe.getShort(this.base + this.offset);
        WasmTracing.trace("load.i64_16s value = 0x%04X (%d)", value, value);
        return value;
    }

    @Override 
    public long load_i64_16u(Node node) {
        WasmTracing.trace("load.i64_16u address = %d", this.base + this.offset);
        validateHandleAccess(node, 2);
        long value = 0x0000_0000_0000_ffffL & unsafe.getShort(this.base + this.offset);
        WasmTracing.trace("load.i64_16u value = 0x%04X (%d)", value, value);
        return value;
    }

    @Override 
    public long load_i64_32s(Node node) {
        WasmTracing.trace("load.i64_32s address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);
        long value = unsafe.getInt(this.base + this.offset);
        WasmTracing.trace("load.i64_32s value = 0x%08X (%d)", value, value);
        return value;
    }

    @Override 
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
        long key = unsafe.getLong(this.base + this.offset);
        WasmTracing.trace("load.handle key = 0x%08X (%d)", key, key);

        // validate key
        if ( ! handles.containsKey(key)) {
            return Handle.nullHandle();
        }
        return handles.get(key);
    }


    @Override 
    public void store_i32(Node node, int value) {
        WasmTracing.trace("store.i32 address = %d, value = 0x%08X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 4);
        unsafe.putInt(this.base + this.offset, value);
    }

    @Override 
    public void store_i64(Node node, long value) {
        WasmTracing.trace("store.i64 address = %d, value = 0x%016X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 8);
        unsafe.putLong(this.base + this.offset, value);
    }

    @Override 
    public void store_f32(Node node, float value) {
        WasmTracing.trace("store.f32 address = %d, value = 0x%08X (%f)", this.base + this.offset, Float.floatToRawIntBits(value), value);
        validateHandleAccess(node, 4);
        unsafe.putFloat(this.base + this.offset, value);
    }

    @Override 
    public void store_f64(Node node, double value) {
        WasmTracing.trace("store.f64 address = %d, value = 0x%016X (%f)", this.base + this.offset, Double.doubleToRawLongBits(value), value);
        validateHandleAccess(node, 8);
        unsafe.putDouble(this.base + this.offset, value);
    }

    @Override 
    public void store_i32_8(Node node, byte value) {
        WasmTracing.trace("store.i32_8 address = %d, value = 0x%02X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 1);
        unsafe.putByte(this.base + this.offset, value);
    }

    @Override 
    public void store_i32_16(Node node, short value) {
        WasmTracing.trace("store.i32_16 address = %d, value = 0x%04X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 2);
        unsafe.putShort(this.base + this.offset, value);
    }

    @Override 
    public void store_i64_8(Node node, byte value) {
        WasmTracing.trace("store.i64_8 address = %d, value = 0x%02X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 1);
        unsafe.putByte(this.base + this.offset, value);
    }

    @Override 
    public void store_i64_16(Node node, short value) {
        WasmTracing.trace("store.i64_16 address = %d, value = 0x%04X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 2);
        unsafe.putShort(this.base + this.offset, value);
    }

    @Override 
    public void store_i64_32(Node node, int value) {
        WasmTracing.trace("store.i64_32 address = %d, value = 0x%08X (%d)", this.base + this.offset, value, value);
        validateHandleAccess(node, 4);
        unsafe.putInt(this.base + this.offset, value);
    }

    public void store_handle(Node node, Handle value) {
        WasmTracing.trace("store.handle address = %d", this.base + this.offset);
        validateHandleAccess(node, 4);        
        unsafe.putLong(this.base + this.offset, value.getKey());
    }

    // Residual functions from WasmMemory

    @Override
    public void copy(Node node, int src, int dst, int n) {
        throw new WasmTrap(node, "Can't copy segment memory to absolute address!");
    }

    @Override
    public void reset() {} // Nothing to do

    @Override
    public int size() {
        return handles.size();
    }

    @Override
    public long byteSize() {
        // Used to check if certain data can fit, which isn't a concern for us
        return Long.MAX_VALUE;
    }

    @Override
    public boolean grow(int extraPageSize) {
        // Nothing to do
        return true;
    }

    @Override
    public WasmMemory duplicate() {
        // No internal data to copy
        return new SegmentMemory();
    }

    @Override
    public void close() {} // Nothing to do

    @Override
    public ByteBuffer asByteBuffer() {
        throw new WasmTrap(node, "Can't represent segment memory as a byte buffer!");
    }

}