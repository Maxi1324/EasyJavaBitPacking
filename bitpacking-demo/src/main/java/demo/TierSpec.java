package demo;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

@BitPacked(storage = PackedStorage.SHORT)
class TierSpec {

    @BitField(bits = 4, valueOffset = 3)
    int mood;

    @BitField(bits = 5)
    int age;

    @BitField(bits = 1)
    boolean wild;
}
