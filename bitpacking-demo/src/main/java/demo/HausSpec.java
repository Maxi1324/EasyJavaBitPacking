package demo;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

@BitPacked(storage = PackedStorage.SHORT, useValidBit = true)
class HausSpec {

    @BitField(bits = 5)
    int rooms;

    @BitField(bits = 3)
    int floors;

    @BitField(bits = 1)
    boolean garage;
}
