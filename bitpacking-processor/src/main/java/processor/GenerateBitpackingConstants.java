package processor;

import annotation.PackedStorage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the shared constants for the packed class.
 *
 * <p>Generated constants:
 * {@code STORAGE_BITS}          number of bits provided by the selected storage type
 * {@code STORAGE_MASK}          mask covering the full storage width
 * {@code VALID_SHIFT}           bit position of the valid marker when enabled
 * {@code VALID_MASK}            mask for the valid marker when enabled
 * {@code <FIELD>_BITS}          number of bits used by one field
 * {@code <FIELD>_SHIFT}         bit position of one field inside the packed value
 * {@code <FIELD>_MASK}          mask for the field width
 * {@code <FIELD>_VALUE_OFFSET}  logical value offset used during encode/decode
 */
public class GenerateBitpackingConstants {
    private final PackedStorage storage;
    private final boolean useValidBit;
    private final List<PackedField> fields;

    public GenerateBitpackingConstants(PackedStorage storage, boolean useValidBit, List<PackedField> fields) {
        this.storage = storage;
        this.useValidBit = useValidBit;
        this.fields = fields;
    }

    public String generate() {
        String storageConstants = """
                public static final int STORAGE_BITS = %d;
                private static final long STORAGE_MASK = %s;
                """.formatted(storage.bitCount(), storageMaskLiteral());

        String validBitConstants = useValidBit ? """
                private static final int VALID_SHIFT = %d;
                private static final long VALID_MASK = 1L << VALID_SHIFT;
                """.formatted(validShift()) : "";

        String fieldConstants = fields.stream()
                .map(this::generateFieldConstants)
                .collect(Collectors.joining("\n\n"));

        String allConstants = joinSections(storageConstants, validBitConstants, fieldConstants);
        if (allConstants.isBlank()) {
            return storageConstants;
        }

        return allConstants;
    }

    private String generateFieldConstants(PackedField field) {
        return """
                private static final int %s = %d;
                private static final int %s = %d;
                private static final long %s = %s;
                private static final long %s = %s;
                """.formatted(
                field.bitsConstant(),
                field.bits(),
                field.shiftConstant(),
                field.shift(),
                field.maskConstant(),
                maskLiteral(field),
                field.valueOffsetConstant(),
                longLiteral(field.valueOffset())
        );
    }

    private String maskLiteral(PackedField field) {
        if (field.bits() == 64) {
            return "-1L";
        }
        return longLiteral((1L << field.bits()) - 1L);
    }

    private String longLiteral(long value) {
        if (value == Long.MIN_VALUE) {
            return "Long.MIN_VALUE";
        }
        return value + "L";
    }

    private String storageMaskLiteral() {
        if (storage == PackedStorage.LONG) {
            return "-1L";
        }
        return longLiteral((1L << storage.bitCount()) - 1L);
    }

    private int validShift() {
        if (fields.isEmpty()) {
            return 0;
        }
        PackedField lastField = fields.get(fields.size() - 1);
        return lastField.shift() + lastField.bits();
    }

    private String joinSections(String... sections) {
        return java.util.Arrays.stream(sections)
                .filter(section -> section != null && !section.isBlank())
                .collect(Collectors.joining("\n\n"));
    }
}
