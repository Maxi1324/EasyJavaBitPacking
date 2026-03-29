package processor;

import annotation.PackedStorage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the class-level packing methods.
 *
 * <p>Generated methods:
 * {@code pack(...)}         packs all fields with validation
 * {@code rawPack(...)}      packs all fields without validation
 */
public class GenerateBitpackingPack {
    private final PackedStorage storage;
    private final boolean useValidBit;
    private final List<PackedField> fields;

    public GenerateBitpackingPack(PackedStorage storage, boolean useValidBit, List<PackedField> fields) {
        this.storage = storage;
        this.useValidBit = useValidBit;
        this.fields = fields;
    }

    public String generate() {
        return joinSections(
                generateTotalBitsConstant(),
                generatePackMethod(),
                generateRawPackMethod()
        );
    }

    private String generateTotalBitsConstant() {
        int totalBits = fields.isEmpty() ? 0 : fields.get(fields.size() - 1).shift() + fields.get(fields.size() - 1).bits();
        if (useValidBit) {
            totalBits += 1;
        }
        return "public static final int TOTAL_BITS = %d;".formatted(totalBits);
    }

    private String generatePackMethod() {
        return """
                public static %s pack(%s) {
                %s
                    return rawPack(%s);
                }
                """.formatted(
                packedType(),
                parameterList(),
                indent(validationLines()),
                argumentList()
        );
    }

    private String generateRawPackMethod() {
        return """
                public static %s rawPack(%s) {
                    long packed = 0L;
                %s
                %s
                    return %s;
                }
                """.formatted(
                packedType(),
                parameterList(),
                indent(rawPackLines()),
                validBitLine(),
                castPacked("packed")
        );
    }

    private String parameterList() {
        return fields.stream()
                .map(field -> "%s %s".formatted(field.typeName(), field.fieldName()))
                .collect(Collectors.joining(", "));
    }

    private String validationLines() {
        return fields.stream()
                .map(field -> "validate%s(%s);".formatted(field.accessorSuffix(), field.fieldName()))
                .collect(Collectors.joining("\n"));
    }

    private String argumentList() {
        return fields.stream()
                .map(PackedField::fieldName)
                .collect(Collectors.joining(", "));
    }

    private String rawPackLines() {
        return fields.stream()
                .map(field -> "packed |= ((%s) & %s) << %s;".formatted(
                        encodeUsage(field, field.fieldName()),
                        field.maskConstant(),
                        field.shiftConstant()
                ))
                .collect(Collectors.joining("\n"));
    }

    private String encodeUsage(PackedField field, String valueExpression) {
        return usesDirectCodec(field)
                ? encodeExpression(field, valueExpression)
                : "encode%s(%s)".formatted(field.accessorSuffix(), valueExpression);
    }

    private String encodeExpression(PackedField field, String valueExpression) {
        return switch (field.typeKind()) {
            case BOOLEAN -> "%s ? 1L : 0L".formatted(valueExpression);
            case CHAR -> "(long) %s".formatted(valueExpression);
            case LONG -> field.valueOffset() == 0
                    ? valueExpression
                    : "%s + %s".formatted(valueExpression, field.valueOffsetConstant());
            case BYTE, SHORT, INT -> "((long) %s) + %s".formatted(valueExpression, field.valueOffsetConstant());
            default -> throw new IllegalStateException("Unsupported encode type: " + field.typeKind());
        };
    }

    private boolean usesDirectCodec(PackedField field) {
        return field.valueOffset() == 0;
    }

    private String packedType() {
        return storage.typeName();
    }

    private String castPacked(String expression) {
        return switch (storage) {
            case BYTE -> "(byte) (%s)".formatted(expression);
            case SHORT -> "(short) (%s)".formatted(expression);
            case INT -> "(int) (%s)".formatted(expression);
            case LONG -> expression;
        };
    }

    private String indent(String block) {
        return block.lines()
                .map(line -> line.isEmpty() ? "" : "    " + line)
                .collect(Collectors.joining("\n"));
    }

    private String joinSections(String... sections) {
        return java.util.Arrays.stream(sections)
                .filter(section -> section != null && !section.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    private String validBitLine() {
        if (!useValidBit) {
            return "";
        }
        return "    packed |= VALID_MASK;\n";
    }
}
