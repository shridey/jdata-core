package io.github.shridey.jdata.core.ffi;

import io.github.shridey.jdata.core.ArrowColumn;
import io.github.shridey.jdata.core.ArrowType;
import io.github.shridey.jdata.core.internal.ArrowCStructs;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Bridges native memory back into Java.
 * This utility reads Arrow C-structs populated by native languages (Rust, C++, Python)
 * and wraps them in jdata's {@link ArrowColumn} interface for zero-copy ingestion.
 *
 * @version 1.0
 * @since 1.0
 */
public final class ArrowCImporter {

    private ArrowCImporter() {}

    // A downcall handle to dynamically invoke the C-level 'release' function provided by the foreign language.
    // The C-signature is: void (*release)(struct ArrowArray*)
    private static final MethodHandle NATIVE_RELEASE_CALL = Linker.nativeLinker().downcallHandle(
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
    );

    /**
     * Imports an Arrow C-struct into a Java ArrowColumn.
     *
     * @param arrayPtr The raw memory address of the struct ArrowArray.
     * @param schemaPtr The raw memory address of the struct ArrowSchema.
     * @return An {@link ArrowColumn} ready for use in jdata.
     */
    public static ArrowColumn importColumn(MemorySegment arrayPtr, MemorySegment schemaPtr) {

        // 1. Read the Schema to determine the data type
        MemorySegment formatPtr = schemaPtr.get(ValueLayout.ADDRESS, ArrowCStructs.SCHEMA_OFFSET_FORMAT);
        // Safely interpret the raw C pointer as a max-16-byte string to extract the format
        String format = formatPtr.reinterpret(16).getString(0);

        MemorySegment namePtr = schemaPtr.get(ValueLayout.ADDRESS, ArrowCStructs.SCHEMA_OFFSET_NAME);
        String name = namePtr.equals(MemorySegment.NULL) ? "imported_col" : namePtr.reinterpret(256).getString(0);

        // Dynamically resolve the native C-format string to our Java ArrowType enum
        ArrowType type = ArrowType.fromFormat(format);

        // 2. Read the Array Metadata
        long length = arrayPtr.get(ValueLayout.JAVA_LONG, ArrowCStructs.ARRAY_OFFSET_LENGTH);
        long nullCount = arrayPtr.get(ValueLayout.JAVA_LONG, ArrowCStructs.ARRAY_OFFSET_NULL_COUNT);

        // Read the number of buffers to determine the bounds of the pointer array
        long nBuffers = arrayPtr.get(ValueLayout.JAVA_LONG, ArrowCStructs.ARRAY_OFFSET_N_BUFFERS);

        // 3. Extract the Buffer Pointers
        MemorySegment rawBuffersPtr = arrayPtr.get(ValueLayout.ADDRESS, ArrowCStructs.ARRAY_OFFSET_BUFFERS);

        // Reinterpret the 0-byte C-pointer into a bounded array of addresses
        MemorySegment buffersArrayPtr = rawBuffersPtr.reinterpret(nBuffers * ValueLayout.ADDRESS.byteSize());

        MemorySegment validityPtr = buffersArrayPtr.getAtIndex(ValueLayout.ADDRESS, 0);
        MemorySegment dataPtr = buffersArrayPtr.getAtIndex(ValueLayout.ADDRESS, 1);

        // 4. Reinterpret the C-pointers to give them strict Java safety boundaries
        // We use Arena.global() because the foreign language owns the memory lifecycle, not Java.
        long bitmapBytes = (length + 7) / 8;
        MemorySegment validityBuffer = validityPtr.reinterpret(bitmapBytes, Arena.global(), null);

        MemorySegment dataBuffer;

        // Dynamically size the data buffer based on the resolved ArrowType
        if (type == ArrowType.FLOAT64 || type == ArrowType.INT64 || type == ArrowType.DATE64) {
            dataBuffer = dataPtr.reinterpret(length * 8, Arena.global(), null);
        } else if (type == ArrowType.INT32 || type == ArrowType.FLOAT32 || type == ArrowType.DATE32) {
            dataBuffer = dataPtr.reinterpret(length * 4, Arena.global(), null);
        } else {
            // For variable length types (UTF8), we cannot easily know the total bytes without
            // reading the last element of the offsets buffer. For safety, we bound to max int.
            dataBuffer = dataPtr.reinterpret(Integer.MAX_VALUE, Arena.global(), null);
        }

        // 5. Extract the Native Release Callback
        MemorySegment releaseFuncPtr = arrayPtr.get(ValueLayout.ADDRESS, ArrowCStructs.ARRAY_OFFSET_RELEASE);

        // 6. Wrap the memory in an anonymous ArrowColumn to satisfy jdata-core's contract
        return new ArrowColumn() {
            @Override public String name() { return name; }
            @Override public ArrowType type() { return type; }
            @Override public long length() { return length; }
            @Override public long nullCount() { return nullCount; }
            @Override public int bufferCount() { return (int) nBuffers; }

            @Override
            public MemorySegment buffer(int index) {
                return index == 0 ? validityBuffer : dataBuffer;
            }

            @Override
            public boolean isNull(long index) {
                long byteIndex = index / 8;
                int bitIndex = (int) (index % 8);
                byte b = validityBuffer.get(ValueLayout.JAVA_BYTE, byteIndex);
                return (b & (1 << bitIndex)) == 0;
            }

            @Override
            public void close() {
                // If the pointer is not NULL, the foreign language hasn't cleaned it up yet.
                if (!releaseFuncPtr.equals(MemorySegment.NULL)) {
                    try {
                        // Dynamically call the C++/Rust release function to trigger memory cleanup on their end
                        NATIVE_RELEASE_CALL.invokeExact(releaseFuncPtr, arrayPtr);

                        // Set the pointer to NULL locally to prevent catastrophic double-freeing
                        arrayPtr.set(ValueLayout.ADDRESS, ArrowCStructs.ARRAY_OFFSET_RELEASE, MemorySegment.NULL);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to invoke native Arrow release callback", t);
                    }
                }
            }
        };
    }
}