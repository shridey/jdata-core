package io.github.shridey.jdata.core.column;

import io.github.shridey.jdata.core.ArrowType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A concrete implementation of an Arrow-compliant 64-bit signed integer column.
 * This physical layout is used for INT64, DATE64, and all TIMESTAMP types.
 *
 * @version 1.0
 * @since 1.0
 */
public class Int64Column extends AbstractArrowColumn {

    private final MemorySegment dataBuffer;
    private final ArrowType logicalType; // Allows this column to act as a Timestamp or Int64

    private Int64Column(String name, Arena arena, long length, ArrowType logicalType,
                        MemorySegment validityBitmap, MemorySegment dataBuffer) {
        super(name, arena, length, validityBitmap);
        this.logicalType = logicalType;
        this.dataBuffer = dataBuffer;
    }

    /**
     * Allocates a new off-heap 64-bit integer column.
     *
     * @param logicalType Must be INT64, DATE64, or a TIMESTAMP type.
     */
    public static Int64Column allocate(Arena arena, String name, long length, ArrowType logicalType) {
        long bitmapBytes = (length + 7) / 8;
        MemorySegment validityBitmap = ArrowMemoryAllocator.allocateAligned(arena, bitmapBytes);
        validityBitmap.fill((byte) 0xFF);

        long dataBytes = length * ValueLayout.JAVA_LONG.byteSize();
        MemorySegment dataBuffer = ArrowMemoryAllocator.allocateAligned(arena, dataBytes);

        return new Int64Column(name, arena, length, logicalType, validityBitmap, dataBuffer);
    }

    public void set(long index, long value) {
        long byteOffset = index * ValueLayout.JAVA_LONG.byteSize();
        dataBuffer.set(ValueLayout.JAVA_LONG, byteOffset, value);
        markValid(index);
    }

    public long get(long index) {
        long byteOffset = index * ValueLayout.JAVA_LONG.byteSize();
        return dataBuffer.get(ValueLayout.JAVA_LONG, byteOffset);
    }

    @Override public ArrowType type() { return logicalType; }
    @Override public int bufferCount() { return 2; }

    @Override
    public MemorySegment buffer(int index) {
        return switch (index) {
            case 0 -> super.buffer(0);
            case 1 -> dataBuffer;
            default -> throw new IndexOutOfBoundsException("Int64Column has exactly 2 buffers.");
        };
    }
}