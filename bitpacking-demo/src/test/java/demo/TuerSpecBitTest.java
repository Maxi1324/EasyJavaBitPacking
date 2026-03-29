package demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TuerSpecBitTest {

    @Test
    void charBooleanAndTemperatureFieldsWorkTogether() {
        int packed = TuerSpecBit.pack('A', true, -5);

        assertEquals('A', TuerSpecBit.getZoneCode(packed));
        assertEquals(-5, TuerSpecBit.getTemperature(packed));
        assertNotEquals(0, packed & (1 << 16));

        int unlocked = TuerSpecBit.setLocked(packed, false);
        assertFalse(TuerSpecBit.getLocked(unlocked));
        assertNotEquals(0, unlocked & (1 << 16));

        int warmer = TuerSpecBit.addTemperature(unlocked, 10);
        assertEquals(5, TuerSpecBit.getTemperature(warmer));

        int wrapped = TuerSpecBit.rawSetTemperature(warmer, 300);
        assertEquals(44, TuerSpecBit.getTemperature(wrapped));
        assertNotEquals(0, wrapped & (1 << 16));
    }
}
