package io.github.shridey.jdata.core.column;

import io.github.shridey.jdata.core.ArrowColumn;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * An abstract foundation for all Arrow-compliant columnar data structures.
 * This class centralizes the management of the validity bitmap, null counting,
 * and standard metadata, enforcing the DRY (Don't Repeat Yourself) principle.
 *
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractArrowColumn implements ArrowColumn {

    protected final String name;
    protected final Arena arena;
    protected final long length;

    // Buffer 0: The shared validity bitmap required by all Arrow types
    protected final MemorySegment validityBitmap;

    // -1 indicates the cache is dirty and must be recalculated
    protected long nullCount = -1;

    /**
     * Constructor for subclasses to initialize the shared state.
     *
     * @param name The logical name of the column.
     * @param arena The memory arena governing this column's lifecycle.
     * @param length The total number of elements.
     * @param validityBitmap The pre-allocated, 64-byte aligned validity bitmap.
     */
    protected AbstractArrowColumn(String name, Arena arena, long length, MemorySegment validityBitmap) {
        this.name = name;
        this.arena = arena;
        this.length = length;
        this.validityBitmap = validityBitmap;
    }

    /**
     * Marks the value at the specified physical index as null.
     * <p>
     * In accordance with the Apache Arrow specification, this method updates the
     * shared validity bitmap by setting the specific bit corresponding to this index to {@code 0}.
     * It does not modify or zero-out the underlying data buffer, as the Arrow format
     * considers data at null indices to be undefined garbage.
     * <p>
     * Calling this method automatically invalidates the internal null count cache,
     * ensuring that subsequent calls to {@link #nullCount()} will trigger a recalculation
     * to return the mathematically accurate total.
     *
     * @param index The zero-based physical row index to mark as null.
     */
    public void setNull(long index) {
        long byteIndex = index / 8;
        int bitIndex = (int) (index % 8);

        byte b = validityBitmap.get(ValueLayout.JAVA_BYTE, byteIndex);

        // Bitwise AND with the inverse of the shifted bit to set only our target to 0
        b = (byte) (b & ~(1 << bitIndex));

        validityBitmap.set(ValueLayout.JAVA_BYTE, byteIndex, b);

        // Invalidate the cache because we mutated the state
        this.nullCount = -1;
    }

    /**
     * Protected utility for subclasses to call when writing valid data to an index.
     * It ensures the validity bitmap correctly reflects that the data is not null.
     *
     * @param index The zero-based row index.
     */
    protected void markValid(long index) {
        long byteIndex = index / 8;
        int bitIndex = (int) (index % 8);

        byte b = validityBitmap.get(ValueLayout.JAVA_BYTE, byteIndex);

        // If the bit is currently 0 (null), we must flip it to 1
        if ((b & (1 << bitIndex)) == 0) {
            b = (byte) (b | (1 << bitIndex)); // Bitwise OR forces the target bit to 1
            validityBitmap.set(ValueLayout.JAVA_BYTE, byteIndex, b);
            this.nullCount = -1;
        }
    }

    @Override
    public boolean isNull(long index) {
        long byteIndex = index / 8;
        int bitIndex = (int) (index % 8);

        byte b = validityBitmap.get(ValueLayout.JAVA_BYTE, byteIndex);
        return (b & (1 << bitIndex)) == 0;
    }

    @Override
    public long nullCount() {
        if (this.nullCount == -1) {
            this.nullCount = recalculateNullCount();
        }
        return this.nullCount;
    }

    /**
     * Hardware-optimized bulk scan to count nulls.
     * Instead of checking bit-by-bit, it reads entire bytes and uses intrinsic
     * bit-counting instructions, while safely ignoring padding bits at the end.
     */
    private long recalculateNullCount() {
        long validCount = 0;
        long fullBytes = length / 8;

        // 1. Process all fully populated bytes
        for (long i = 0; i < fullBytes; i++) {
            byte b = validityBitmap.get(ValueLayout.JAVA_BYTE, i);
            // Integer.bitCount translates to a single CPU instruction (POPCNT)
            validCount += Integer.bitCount(Byte.toUnsignedInt(b));
        }

        // 2. Process the final partial byte (if the length is not a perfect multiple of 8)
        int remainderBits = (int) (length % 8);
        if (remainderBits > 0) {
            byte b = validityBitmap.get(ValueLayout.JAVA_BYTE, fullBytes);
            // Create a mask to isolate ONLY the bits that belong to our actual data
            // e.g., if remainder is 3, mask is (1 << 3) - 1 = 8 - 1 = 7 (binary 00000111)
            int mask = (1 << remainderBits) - 1;
            validCount += Integer.bitCount(Byte.toUnsignedInt(b) & mask);
        }

        // Arrow defines nulls as 0s. The total nulls is the total length minus the valid bits.
        return length - validCount;
    }

    // Standard getters inherited by all subclasses
    @Override public String name() { return name; }
    @Override public long length() { return length; }

    @Override
    public MemorySegment buffer(int index) {
        if (index == 0) return validityBitmap;
        throw new IndexOutOfBoundsException("Buffer index out of bounds. Let subclasses handle index > 0.");
    }

    @Override
    public void close() {
        // Managed by the Arena. Subclasses can override if they need specific cleanup.
    }
}