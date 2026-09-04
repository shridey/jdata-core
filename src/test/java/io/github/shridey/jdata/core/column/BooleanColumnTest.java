package io.github.shridey.jdata.core.column;

import org.junit.jupiter.api.Test;
import java.lang.foreign.Arena;
import static org.junit.jupiter.api.Assertions.*;

class BooleanColumnTest {

    @Test
    void testBooleanBitPacking() {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate 10 elements (requires 2 bytes of data buffer)
            BooleanColumn col = BooleanColumn.allocate(arena, "flags", 10);

            // Set two bits in the same byte
            col.set(0, true);
            col.set(1, false);
            col.set(2, true);

            assertTrue(col.get(0));
            assertFalse(col.get(1));
            assertTrue(col.get(2));

            // Ensure padding bits in the second byte aren't corrupted
            col.set(9, true);
            assertTrue(col.get(9));
            assertFalse(col.get(8)); // Should default to false (0x00 initialization)
        }
    }
}