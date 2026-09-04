package io.github.shridey.jdata.core.ffi;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;

/**
 * Defines the strict C-struct memory layouts required by the Apache Arrow C-Data Interface.
 * These layouts allow Java's FFM API to read and write directly to C memory structures,
 * enabling zero-copy data sharing with Rust, C++, and Python.
 *
 * @version 1.0
 * @since 1.0
 */
public final class ArrowCStructs {

    // Prevent instantiation
    private ArrowCStructs() {}

    /**
     * The layout for 'struct ArrowArray'.
     * This C-struct holds the actual memory pointers to our off-heap buffers.
     */
    public static final StructLayout ARROW_ARRAY_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("length"),
            ValueLayout.JAVA_LONG.withName("null_count"),
            ValueLayout.JAVA_LONG.withName("offset"),
            ValueLayout.JAVA_LONG.withName("n_buffers"),
            ValueLayout.JAVA_LONG.withName("n_children"),
            ValueLayout.ADDRESS.withName("buffers"),
            ValueLayout.ADDRESS.withName("children"),
            ValueLayout.ADDRESS.withName("dictionary"),
            ValueLayout.ADDRESS.withName("release"),
            ValueLayout.ADDRESS.withName("private_data")
    ).withName("ArrowArray");

    /**
     * The layout for 'struct ArrowSchema'.
     * This C-struct holds the metadata explaining what data type the buffers contain.
     */
    public static final StructLayout ARROW_SCHEMA_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("format"),       // const char*
            ValueLayout.ADDRESS.withName("name"),         // const char*
            ValueLayout.ADDRESS.withName("metadata"),     // const char*
            ValueLayout.JAVA_LONG.withName("flags"),
            ValueLayout.JAVA_LONG.withName("n_children"),
            ValueLayout.ADDRESS.withName("children"),     // struct ArrowSchema**
            ValueLayout.ADDRESS.withName("dictionary"),   // struct ArrowSchema*
            ValueLayout.ADDRESS.withName("release"),      // void (*release)(struct ArrowSchema*)
            ValueLayout.ADDRESS.withName("private_data")  // void*
    ).withName("ArrowSchema");

    // Pre-computed byte offsets for ArrowArray fields (for fast writing)
    public static final long ARRAY_OFFSET_LENGTH = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("length"));
    public static final long ARRAY_OFFSET_NULL_COUNT = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("null_count"));
    public static final long ARRAY_OFFSET_OFFSET = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("offset"));
    public static final long ARRAY_OFFSET_N_BUFFERS = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("n_buffers"));
    public static final long ARRAY_OFFSET_N_CHILDREN = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("n_children"));
    public static final long ARRAY_OFFSET_BUFFERS = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("buffers"));
    public static final long ARRAY_OFFSET_CHILDREN = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("children"));
    public static final long ARRAY_OFFSET_DICTIONARY = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("dictionary"));
    public static final long ARRAY_OFFSET_RELEASE = ARROW_ARRAY_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("release"));

    // Pre-computed byte offsets for ArrowSchema fields
    public static final long SCHEMA_OFFSET_FORMAT = ARROW_SCHEMA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("format"));
    public static final long SCHEMA_OFFSET_NAME = ARROW_SCHEMA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("name"));
    public static final long SCHEMA_OFFSET_FLAGS = ARROW_SCHEMA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("flags"));
    public static final long SCHEMA_OFFSET_N_CHILDREN = ARROW_SCHEMA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("n_children"));
    public static final long SCHEMA_OFFSET_CHILDREN = ARROW_SCHEMA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("children"));
    public static final long SCHEMA_OFFSET_DICTIONARY = ARROW_SCHEMA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("dictionary"));
    public static final long SCHEMA_OFFSET_RELEASE = ARROW_SCHEMA_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("release"));
}