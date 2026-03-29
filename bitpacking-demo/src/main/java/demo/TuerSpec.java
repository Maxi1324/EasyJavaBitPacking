package demo;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

@BitPacked(storage = PackedStorage.INT, useValidBit = true)
class TuerSpec {

    @BitField(bits = 7)
    char zoneCode;

    @BitField(bits = 1)
    boolean locked;

    @BitField(bits = 8, valueOffset = 20)
    int temperature;
}
