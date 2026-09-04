package io.github.shridey.jdata.core.column;

import io.github.shridey.jdata.core.ArrowType;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 * A concrete implementation of an Arrow-compliant variable-length UTF-8 string column.
 * Manages three buffers: validity (via parent), offsets, and raw byte data.
 *
 * @version 1.0
 * @since 1.0
 */
public class UTF8Column extends AbstractArrowColumn {

    private final MemorySegment offsetsBuffer;
    private final MemorySegment dataBuffer;

    /**
     * Internal constructor.
     */
    private UTF8Column(String name, Arena arena, long length, MemorySegment validityBitmap,
                       MemorySegment offsetsBuffer, MemorySegment dataBuffer) {
        super(name, arena, length, validityBitmap);
        this.offsetsBuffer = offsetsBuffer;
        this.dataBuffer = dataBuffer;
    }

    /**
     * Allocates and populates an off-heap UTF-8 column from an array of Java Strings.
     */
    public static UTF8Column allocate(Arena arena, String name, String[] values) {
        long length = values.length;

        // 1. Calculate Buffer Sizes
        long bitmapBytes = (length + 7) / 8;
        long offsetsBytes = (length + 1) * ValueLayout.JAVA_INT.byteSize();
        long totalDataBytes = 0;

        byte[][] utf8BytesArray = new byte[(int)length][];

        for (int i = 0; i < length; i++) {
            if (values[i] != null) {
                utf8BytesArray[i] = values[i].getBytes(StandardCharsets.UTF_8);
                totalDataBytes += utf8BytesArray[i].length;
            }
        }

        // 2. Allocate the 64-byte aligned off-heap buffers
        MemorySegment validityBitmap = ArrowMemoryAllocator.allocateAligned(arena, bitmapBytes);
        MemorySegment offsetsBuffer = ArrowMemoryAllocator.allocateAligned(arena, offsetsBytes);

        long safeDataBytes = totalDataBytes == 0 ? 64 : totalDataBytes;
        MemorySegment dataBuffer = ArrowMemoryAllocator.allocateAligned(arena, safeDataBytes);

        validityBitmap.fill((byte) 0xFF);

        // 3. Instantiate the column BEFORE filling data so we can reuse the parent's setNull method
        UTF8Column col = new UTF8Column(name, arena, length, validityBitmap, offsetsBuffer, dataBuffer);

        // 4. Write data to the buffers
        int currentOffset = 0;

        for (int i = 0; i < length; i++) {
            // Write the start boundary
            col.offsetsBuffer.set(ValueLayout.JAVA_INT, (long) i * ValueLayout.JAVA_INT.byteSize(), currentOffset);

            if (values[i] == null) {
                // Rely cleanly on the inherited base class method
                col.setNull(i);
            } else {
                byte[] stringBytes = utf8BytesArray[i];
                MemorySegment.copy(stringBytes, 0, col.dataBuffer, ValueLayout.JAVA_BYTE, currentOffset, stringBytes.length);
                currentOffset += stringBytes.length;
            }
        }

        // Write the absolute end boundary
        col.offsetsBuffer.set(ValueLayout.JAVA_INT, length * ValueLayout.JAVA_INT.byteSize(), currentOffset);

        return col;
    }

    /**
     * Reads a String from the off-heap buffers at the specified physical index.
     */
    public String getString(long index) {
        if (isNull(index)) {
            return null;
        }

        long startOffsetByteIndex = index * ValueLayout.JAVA_INT.byteSize();
        int startByte = offsetsBuffer.get(ValueLayout.JAVA_INT, startOffsetByteIndex);

        long endOffsetByteIndex = (index + 1) * ValueLayout.JAVA_INT.byteSize();
        int endByte = offsetsBuffer.get(ValueLayout.JAVA_INT, endOffsetByteIndex);

        int stringLength = endByte - startByte;
        if (stringLength == 0) return "";

        MemorySegment stringSegment = dataBuffer.asSlice(startByte, stringLength);
        byte[] bytes = stringSegment.toArray(ValueLayout.JAVA_BYTE);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public ArrowType type() {
        return ArrowType.UTF8;
    }

    @Override
    public int bufferCount() {
        return 3;
    }

    @Override
    public MemorySegment buffer(int index) {
        return switch (index) {
            case 0 -> super.buffer(0); // Validity bitmap
            case 1 -> offsetsBuffer;   // 32-bit string offsets
            case 2 -> dataBuffer;      // Raw UTF-8 string data
            default -> throw new IndexOutOfBoundsException("UTF8Column has exactly 3 buffers.");
        };
    }
}