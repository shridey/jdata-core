package io.github.shridey.jdata.core.column;

import org.junit.jupiter.api.Test;
import java.lang.foreign.Arena;
import static org.junit.jupiter.api.Assertions.*;

class UTF8ColumnTest {

    @Test
    void testVariableLengthStringOffsets() {
        try (Arena arena = Arena.ofConfined()) {
            String[] input = {"Apple", null, "", "Banana"};
            UTF8Column col = UTF8Column.allocate(arena, "fruits", input);

            assertEquals(4, col.length());
            assertEquals(1, col.nullCount());

            // 1. Standard string extraction
            assertEquals("Apple", col.getString(0));

            // 2. Null extraction (should bypass offset math)
            assertTrue(col.isNull(1));
            assertNull(col.getString(1));

            // 3. Empty string extraction (start offset == end offset)
            assertFalse(col.isNull(2));
            assertEquals("", col.getString(2));

            // 4. Final string extraction
            assertEquals("Banana", col.getString(3));
        }
    }
}