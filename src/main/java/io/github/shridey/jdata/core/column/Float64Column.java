package io.github.shridey.jdata.core.column;

import io.github.shridey.jdata.core.ArrowType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A concrete implementation of an Arrow-compliant 64-bit floating-point column.
 * Inherits validity bitmap management and memory lifecycle from {@link AbstractArrowColumn}.
 *
 * @version 1.0
 * @since 1.0
 */
public class Float64Column extends AbstractArrowColumn {

    private final MemorySegment dataBuffer;

    /**
     * Internal constructor linking the abstract parent to the concrete data buffer.
     */
    private Float64Column(String name, Arena arena, long length,
                          MemorySegment validityBitmap, MemorySegment dataBuffer) {
        super(name, arena, length, validityBitmap);
        this.dataBuffer = dataBuffer;
    }

    /**
     * Allocates a new off-heap 64-bit float column.
     *
     * @param arena The memory arena governing this column's lifecycle.
     * @param name The logical name of the column.
     * @param length The total number of elements to allocate space for.
     * @return A fully initialized {@link Float64Column}.
     */
    public static Float64Column allocate(Arena arena, String name, long length) {
        long bitmapBytes = (length + 7) / 8;
        MemorySegment validityBitmap = ArrowMemoryAllocator.allocateAligned(arena, bitmapBytes);
        validityBitmap.fill((byte) 0xFF);

        long dataBytes = length * ValueLayout.JAVA_DOUBLE.byteSize();
        MemorySegment dataBuffer = ArrowMemoryAllocator.allocateAligned(arena, dataBytes);

        return new Float64Column(name, arena, length, validityBitmap, dataBuffer);
    }

    /**
     * Sets a 64-bit float value at the specified physical index and marks it as valid.
     */
    public void set(long index, double value) {
        long byteOffset = index * ValueLayout.JAVA_DOUBLE.byteSize();
        dataBuffer.set(ValueLayout.JAVA_DOUBLE, byteOffset, value);

        // Delegate to the parent class to safely manage the validity state
        markValid(index);
    }

    /**
     * Reads a 64-bit float value from the specified physical index.
     */
    public double get(long index) {
        long byteOffset = index * ValueLayout.JAVA_DOUBLE.byteSize();
        return dataBuffer.get(ValueLayout.JAVA_DOUBLE, byteOffset);
    }

    @Override
    public ArrowType type() {
        return ArrowType.FLOAT64;
    }

    @Override
    public int bufferCount() {
        return 2;
    }

    @Override
    public MemorySegment buffer(int index) {
        return switch (index) {
            case 0 -> super.buffer(0); // Fetches the validity bitmap from the parent
            case 1 -> dataBuffer;
            default -> throw new IndexOutOfBoundsException("Float64Column has exactly 2 buffers.");
        };
    }
}