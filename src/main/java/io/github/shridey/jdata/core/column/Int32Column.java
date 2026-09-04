package io.github.shridey.jdata.core.column;

import io.github.shridey.jdata.core.ArrowType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A concrete implementation of an Arrow-compliant 32-bit signed integer column.
 * Inherits validity bitmap management and memory lifecycle from {@link AbstractArrowColumn}.
 *
 * @version 1.0
 * @since 1.0
 */
public class Int32Column extends AbstractArrowColumn {

    private final MemorySegment dataBuffer;

    /**
     * Internal constructor linking the abstract parent to the concrete data buffer.
     */
    private Int32Column(String name, Arena arena, long length,
                        MemorySegment validityBitmap, MemorySegment dataBuffer) {
        super(name, arena, length, validityBitmap);
        this.dataBuffer = dataBuffer;
    }

    /**
     * Allocates a new off-heap 32-bit integer column.
     *
     * @param arena The memory arena governing this column's lifecycle.
     * @param name The logical name of the column.
     * @param length The total number of elements to allocate space for.
     * @return A fully initialized {@link Int32Column}.
     */
    public static Int32Column allocate(Arena arena, String name, long length) {
        long bitmapBytes = (length + 7) / 8;
        MemorySegment validityBitmap = ArrowMemoryAllocator.allocateAligned(arena, bitmapBytes);
        validityBitmap.fill((byte) 0xFF);

        long dataBytes = length * ValueLayout.JAVA_INT.byteSize();
        MemorySegment dataBuffer = ArrowMemoryAllocator.allocateAligned(arena, dataBytes);

        return new Int32Column(name, arena, length, validityBitmap, dataBuffer);
    }

    /**
     * Sets a 32-bit integer value at the specified physical index and marks it valid.
     */
    public void set(long index, int value) {
        long byteOffset = index * ValueLayout.JAVA_INT.byteSize();
        dataBuffer.set(ValueLayout.JAVA_INT, byteOffset, value);

        // Let the base class handle flipping the bit back to 1 if it was null
        markValid(index);
    }

    /**
     * Reads a 32-bit integer value from the specified physical index.
     */
    public int get(long index) {
        long byteOffset = index * ValueLayout.JAVA_INT.byteSize();
        return dataBuffer.get(ValueLayout.JAVA_INT, byteOffset);
    }

    @Override
    public ArrowType type() {
        return ArrowType.INT32;
    }

    @Override
    public int bufferCount() {
        return 2;
    }

    @Override
    public MemorySegment buffer(int index) {
        return switch (index) {
            case 0 -> super.buffer(0); // Validity bitmap
            case 1 -> dataBuffer;      // 32-bit Integers
            default -> throw new IndexOutOfBoundsException("Int32Column has exactly 2 buffers.");
        };
    }
}