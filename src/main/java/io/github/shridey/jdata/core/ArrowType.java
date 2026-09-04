package io.github.shridey.jdata.core;

/**
 * Defines the comprehensive set of data types supported by the Apache Arrow specification.
 * Each enum value is bound to its exact C-Data Interface format string, ensuring
 * seamless interoperability with C++, Rust, and Python without runtime translation.
 *
 * @see <a href="https://arrow.apache.org/docs/format/CDataInterface.html">Arrow C Data Interface</a>
 * @version 1.0
 * @since 1.0
 */
public enum ArrowType {

    // --- Null & Boolean ---
    NULL("n"),
    BOOLEAN("b"),

    // --- Signed Integers ---
    INT8("c"),
    INT16("s"),
    INT32("i"),
    INT64("l"),

    // --- Unsigned Integers ---
    UINT8("C"),
    UINT16("S"),
    UINT32("I"),
    UINT64("L"),

    // --- Floating Point ---
    FLOAT16("e"),
    FLOAT32("f"),
    FLOAT64("g"),

    // --- Variable-Length Binary and Strings ---
    UTF8("u"),           // 32-bit offsets
    LARGE_UTF8("U"),     // 64-bit offsets (for strings > 2GB)
    BINARY("z"),         // 32-bit offsets (raw bytes)
    LARGE_BINARY("Z"),   // 64-bit offsets

    // --- Date & Time ---
    DATE32("tdD"),       // Days since UNIX epoch (32-bit int)
    DATE64("tdm"),       // Milliseconds since UNIX epoch (64-bit int)

    // Note: Time and Timestamp types have varying precisions (s, ms, us, ns).
    // We define the base millisecond and microsecond variants used most often by databases.
    TIME32_MS("ttm"),    // Milliseconds since midnight
    TIME64_US("ttu"),    // Microseconds since midnight
    TIMESTAMP_MS("tsm:"), // Milliseconds since epoch (no timezone)
    TIMESTAMP_US("tsu:"), // Microseconds since epoch (no timezone)

    // --- Complex Types ---
    LIST("+l"),          // Standard List with 32-bit offsets
    LARGE_LIST("+L"),    // Large List with 64-bit offsets
    STRUCT("+s"),        // Struct (nested types)
    DICTIONARY("DICTIONARY"); // Dictionary encoding relies on a nested schema, handled dynamically

    private final String cDataFormat;

    /**
     * Constructs the ArrowType with its corresponding C-Data Interface format string.
     *
     * @param cDataFormat The exact character sequence expected by the native C ABI.
     */
    ArrowType(String cDataFormat) {
        this.cDataFormat = cDataFormat;
    }

    /**
     * Retrieves the C-Data Interface format string for this memory layout.
     *
     * @return The format string (e.g., "g" for Float64).
     */
    public String getFormat() {
        return cDataFormat;
    }

    /**
     * Reverse lookup to map a native C-format string back to a Java ArrowType.
     *
     * @param format The C-Data format string provided by a foreign language.
     * @return The corresponding {@link ArrowType}.
     * @throws IllegalArgumentException if the format string is unknown to jdata.
     */
    public static ArrowType fromFormat(String format) {
        for (ArrowType type : values()) {
            // Timestamp and FixedSize types can have appended metadata,
            // so we check if the format starts with the base identifier.
            if (format.startsWith(type.cDataFormat) && type != DICTIONARY) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported Arrow C-Data format: " + format);
    }
}