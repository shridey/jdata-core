package io.github.shridey.jdata.core.column;

import io.github.shridey.jdata.core.ArrowType;
import io.github.shridey.jdata.core.internal.ArrowMemoryAllocator;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A concrete implementation of an Arrow-compliant Boolean column.
 * Unlike other primitives, the boolean data buffer is bit-packed (8 values per byte),
 * meaning it shares the exact same memory sizing math as the validity bitmap.
 *
 * @version 1.0
 * @since 1.0
 */
public class BooleanColumn extends AbstractArrowColumn {

    private final MemorySegment dataBuffer;

    private BooleanColumn(String name, Arena arena, long length,
                          MemorySegment validityBitmap, MemorySegment dataBuffer) {
        super(name, arena, length, validityBitmap);
        this.dataBuffer = dataBuffer;
    }

    public static BooleanColumn allocate(Arena arena, String name, long length) {
        // Both the validity bitmap and the boolean data buffer require 1 bit per element
        long byteSize = (length + 7) / 8;

        MemorySegment validityBitmap = ArrowMemoryAllocator.allocateAligned(arena, byteSize);
        validityBitmap.fill((byte) 0xFF);

        MemorySegment dataBuffer = ArrowMemoryAllocator.allocateAligned(arena, byteSize);
        // Initialize booleans to false (all 0s)
        dataBuffer.fill((byte) 0x00);

        return new BooleanColumn(name, arena, length, validityBitmap, dataBuffer);
    }

    /**
     * Sets a boolean value using bit-manipulation.
     */
    public void set(long index, boolean value) {
        long byteIndex = index / 8;
        int bitIndex = (int) (index % 8);

        byte b = dataBuffer.get(ValueLayout.JAVA_BYTE, byteIndex);

        if (value) {
            b = (byte) (b | (1 << bitIndex));  // Force bit to 1
        } else {
            b = (byte) (b & ~(1 << bitIndex)); // Force bit to 0
        }

        dataBuffer.set(ValueLayout.JAVA_BYTE, byteIndex, b);
        markValid(index);
    }

    /**
     * Reads a boolean value directly from the bit-packed buffer.
     */
    public boolean get(long index) {
        long byteIndex = index / 8;
        int bitIndex = (int) (index % 8);

        byte b = dataBuffer.get(ValueLayout.JAVA_BYTE, byteIndex);
        return (b & (1 << bitIndex)) != 0;
    }

    @Override public ArrowType type() { return ArrowType.BOOLEAN; }
    @Override public int bufferCount() { return 2; }

    @Override
    public MemorySegment buffer(int index) {
        return switch (index) {
            case 0 -> super.buffer(0);
            case 1 -> dataBuffer;
            default -> throw new IndexOutOfBoundsException("BooleanColumn has exactly 2 buffers.");
        };
    }
}