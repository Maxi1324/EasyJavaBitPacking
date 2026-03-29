package annotation;

public enum PackedStorage {
    BYTE("byte", 8),
    SHORT("short", 16),
    INT("int", 32),
    LONG("long", 64);

    private final String typeName;
    private final int bitCount;

    PackedStorage(String typeName, int bitCount) {
        this.typeName = typeName;
        this.bitCount = bitCount;
    }

    public String typeName() {
        return typeName;
    }

    public int bitCount() {
        return bitCount;
    }
}
