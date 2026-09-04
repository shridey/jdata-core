package io.github.shridey.jdata.core;

import java.lang.foreign.MemorySegment;

/**
 * The foundational interface for all columnar data structures in the jdata ecosystem.
 * It mandates that all implementing classes manage off-heap memory and adhere
 * to the Apache Arrow physical layout specification.
 *
 * @version 1.0
 * @since 1.0
 */
public interface ArrowColumn extends AutoCloseable {

    /**
     * Retrieves the logical name of this column.
     *
     * @return The column name as a String.
     */
    String name();

    /**
     * Identifies the physical memory layout of this column.
     *
     * @return The {@link ArrowType} representing the data type.
     */
    ArrowType type();

    /**
     * Returns the total number of logical elements (rows) in this column,
     * including null values.
     *
     * @return The total element count.
     */
    long length();

    /**
     * Returns the exact number of null values present in this column.
     * This is crucial for optimizing queries (e.g., skipping null checks if count is 0).
     *
     * @return The count of null values.
     */
    long nullCount();

    /**
     * Checks if the value at the specified physical index is null.
     * This reads directly from the off-heap validity bitmap.
     *
     * @param index The zero-based row index.
     * @return True if the value is null, false otherwise.
     */
    boolean isNull(long index);

    /**
     * Returns the number of underlying memory buffers used by this column type.
     * Primitive types (Float64) return 2. Variable-length types (UTF8) return 3.
     *
     * @return The number of physical memory buffers.
     */
    int bufferCount();

    /**
     * Exposes the raw off-heap memory segment for a specific buffer.
     * This is required for zero-copy sharing with C++, Rust, or Python via FFM.
     *
     * @param index The zero-based index of the buffer (0 is always the validity bitmap).
     * @return The raw {@link MemorySegment}.
     * @throws IndexOutOfBoundsException if the index is greater than or equal to bufferCount().
     */
    MemorySegment buffer(int index);

    /**
     * Safely releases all off-heap memory associated with this column back to the OS.
     * Required to prevent memory leaks, as off-heap memory bypasses Java Garbage Collection.
     */
    @Override
    void close();
}