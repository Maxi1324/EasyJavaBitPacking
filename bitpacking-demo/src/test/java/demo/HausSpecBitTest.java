package demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HausSpecBitTest {

    @Test
    void packSetAndAddKeepValuesAndValidBit() {
        short packed = HausSpecBit.pack(12, 3, true);

        assertEquals(12, HausSpecBit.getRooms(packed));
        assertEquals(3, HausSpecBit.getFloors(packed));
        assertTrue(HausSpecBit.getGarage(packed));
        assertNotEquals(0, packed & (1 << 9));

        short updated = HausSpecBit.setFloors(packed, 5);
        assertEquals(5, HausSpecBit.getFloors(updated));
        assertNotEquals(0, updated & (1 << 9));

        short expanded = HausSpecBit.addRooms(updated, 2);
        assertEquals(14, HausSpecBit.getRooms(expanded));
        assertNotEquals(0, expanded & (1 << 9));
    }
}
