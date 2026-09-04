package io.github.shridey.jdata.core.column;

import io.github.shridey.jdata.core.ArrowType;
import io.github.shridey.jdata.core.internal.ArrowMemoryAllocator;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A concrete implementation of an Arrow-compliant 32-bit floating-point column.
 * Inherits metadata, null-counting, and validity bitmap management from {@link AbstractArrowColumn}.
 * This column type is crucial for machine learning workloads where single-precision
 * math offers a 2x memory and throughput advantage over 64-bit doubles.
 *
 * @version 1.0
 * @since 1.0
 */
public class Float32Column extends AbstractArrowColumn {

    private final MemorySegment dataBuffer;

    /**
     * Internal constructor linking the abstract parent's state to this concrete data buffer.
     *
     * @param name The logical name of the column.
     * @param arena The memory arena governing this column's lifecycle.
     * @param length The total number of physical elements.
     * @param validityBitmap The pre-allocated shared validity bitmap.
     * @param dataBuffer The 64-byte aligned buffer specifically for 32-bit floats.
     */
    private Float32Column(String name, Arena arena, long length,
                          MemorySegment validityBitmap, MemorySegment dataBuffer) {
        super(name, arena, length, validityBitmap);
        this.dataBuffer = dataBuffer;
    }

    /**
     * Allocates a new off-heap 32-bit float column.
     *
     * @param arena The memory arena governing this column's lifecycle.
     * @param name The logical name of the column.
     * @param length The total number of elements to allocate space for.
     * @return A fully initialized {@link Float32Column}.
     */
    public static Float32Column allocate(Arena arena, String name, long length) {
        // Validity bitmap requires 1 bit per element
        long bitmapBytes = (length + 7) / 8;
        MemorySegment validityBitmap = ArrowMemoryAllocator.allocateAligned(arena, bitmapBytes);
        validityBitmap.fill((byte) 0xFF); // Initialize all to valid

        // Data buffer requires exactly 4 bytes per float (32 bits)
        long dataBytes = length * ValueLayout.JAVA_FLOAT.byteSize();
        MemorySegment dataBuffer = ArrowMemoryAllocator.allocateAligned(arena, dataBytes);

        return new Float32Column(name, arena, length, validityBitmap, dataBuffer);
    }

    /**
     * Sets a 32-bit float value at the specified physical index.
     * Automatically marks the corresponding index in the validity bitmap as valid (not null).
     *
     * @param index The zero-based row index.
     * @param value The single-precision float value to write.
     */
    public void set(long index, float value) {
        long byteOffset = index * ValueLayout.JAVA_FLOAT.byteSize();
        dataBuffer.set(ValueLayout.JAVA_FLOAT, byteOffset, value);

        // Delegate to the parent class to safely flip the bit from 0 to 1 if it was previously null
        markValid(index);
    }

    /**
     * Reads a 32-bit float value from the specified physical index.
     *
     * @param index The zero-based row index.
     * @return The 32-bit float value.
     */
    public float get(long index) {
        long byteOffset = index * ValueLayout.JAVA_FLOAT.byteSize();
        return dataBuffer.get(ValueLayout.JAVA_FLOAT, byteOffset);
    }

    @Override
    public ArrowType type() {
        return ArrowType.FLOAT32;
    }

    @Override
    public int bufferCount() {
        return 2;
    }

    @Override
    public MemorySegment buffer(int index) {
        return switch (index) {
            case 0 -> super.buffer(0); // Safely pull the validity bitmap from AbstractArrowColumn
            case 1 -> dataBuffer;      // Return the physical float buffer
            default -> throw new IndexOutOfBoundsException("Float32Column has exactly 2 buffers.");
        };
    }
}