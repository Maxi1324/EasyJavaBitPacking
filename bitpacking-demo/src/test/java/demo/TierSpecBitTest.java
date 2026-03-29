package demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierSpecBitTest {

    @Test
    void packAndRawOperationsWorkForOffsetsAndWrapping() {
        short packed = TierSpecBit.pack(-2, 7, false);

        assertEquals(-2, TierSpecBit.getMood(packed));
        assertEquals(7, TierSpecBit.getAge(packed));
        assertFalse(TierSpecBit.getWild(packed));

        short friendly = TierSpecBit.setMood(packed, 4);
        assertEquals(4, TierSpecBit.getMood(friendly));

        short rawPacked = TierSpecBit.rawPack(20, 40, true);
        assertEquals(4, TierSpecBit.getMood(rawPacked));
        assertEquals(8, TierSpecBit.getAge(rawPacked));
        assertTrue(TierSpecBit.getWild(rawPacked));

        short older = TierSpecBit.rawAddAge(rawPacked, 30);
        assertEquals(6, TierSpecBit.getAge(older));
    }

    @Test
    void packRejectsValuesOutsideConfiguredRanges() {
        assertThrows(IllegalArgumentException.class, () -> TierSpecBit.pack(-4, 0, false));
        assertThrows(IllegalArgumentException.class, () -> TierSpecBit.pack(0, 32, false));
    }
}
