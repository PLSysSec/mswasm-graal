package org.graalvm.wasm.mswasm;

import org.graalvm.wasm.exception.Failure;
import org.graalvm.wasm.memory.ByteArrayWasmMemory;
import org.graalvm.wasm.exception.WasmException;
import com.oracle.truffle.api.memory.ByteArraySupport;

public class Segment {
    protected byte[] buffer;
    private boolean isFree;

    // Used as a unique random key to track this segment.
    // We use only the lower 30 bits for keys to leave room for null and corrupted bits
    // in the 64-bit long representation of a handle.
    private final int key;
    private static final int MIN_KEY = 1;
    private static final int MAX_KEY = Integer.MAX_VALUE;

    public Segment(int byteSize) {
        try {
            buffer = new byte[byteSize];
        } catch (OutOfMemoryError error) {
            throw WasmException.create(Failure.MEMORY_ALLOCATION_FAILED);
        }
        this.key = (int)(Math.random() * (MAX_KEY - MIN_KEY)) + MIN_KEY;
    }

    public boolean isFree() {
        return isFree;
    }

    public void free() {
        buffer = null;
        isFree = true;
    }

    /**
     * Get the integer key corresponding to this segment
     */
    public int key() {
        return key;
    }

    // Buffer access methods

    public int getInt(int offset) {
        return ByteArraySupport.littleEndian().getInt(buffer, offset);
    }

    public long getLong(int offset) {
        return ByteArraySupport.littleEndian().getLong(buffer, offset);
    }

    public float getFloat(int offset) {
        return ByteArraySupport.littleEndian().getFloat(buffer, offset);
    }

    public double getDouble(int offset) {
        return ByteArraySupport.littleEndian().getDouble(buffer, offset);
    }

    public short getShort(int offset) {
        return ByteArraySupport.littleEndian().getShort(buffer, offset);
    }

    public byte getByte(int offset) {
        return ByteArraySupport.littleEndian().getByte(buffer, offset);
    }

    public void putInt(int offset, int value) {
        ByteArraySupport.littleEndian().putInt(buffer, offset, value);
    }

    public void putLong(int offset, long value) {
        ByteArraySupport.littleEndian().putLong(buffer, offset, value);
    }

    public void putFloat(int offset, float value) {
        ByteArraySupport.littleEndian().putFloat(buffer, offset, value);
    }

    public void putDouble(int offset, double value) {
        ByteArraySupport.littleEndian().putDouble(buffer, offset, value);
    }

    public void putShort(int offset, short value) {
        ByteArraySupport.littleEndian().putShort(buffer, offset, value);
    }

    public void putByte(int offset, byte value) {
        ByteArraySupport.littleEndian().putByte(buffer, offset, value);
    }

    @Override
    public String toString() {
        return String.format("Segment %d {size: %d, free: %b}",
            key, buffer.length, isFree);
    }
}
