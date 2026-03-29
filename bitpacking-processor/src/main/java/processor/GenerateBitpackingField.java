package processor;

import annotation.PackedStorage;

import javax.lang.model.type.TypeKind;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Generates the field-specific methods for one packed field.
 *
 * <p>Generated methods:
 * {@code getX(...)}         reads the logical field value from the packed value
 * {@code setX(...)}         writes the field with validation
 * {@code rawSetX(...)}      writes the field without validation
 * {@code addX(...)}         adds a delta with overflow and range checks
 * {@code rawAddX(...)}      adds a delta without validation
 * {@code validateX(...)}    checks whether a value fits into the configured bit range
 * {@code encodeX(...)}      shifts the logical value into its stored representation
 * {@code decodeX(...)}      converts the stored representation back to the logical value
 */
public class GenerateBitpackingField {
    private final PackedStorage storage;
    private final boolean useValidBit;
    private final PackedField field;

    public GenerateBitpackingField(PackedStorage storage, boolean useValidBit, PackedField field) {
        this.storage = storage;
        this.useValidBit = useValidBit;
        this.field = field;
    }

    public String generateGetMethod() {
        return """
                public static %s get%s(%s packed) {
                    long encoded = (%s >>> %s) & %s;
                    return %s;
                }
                """.formatted(
                field.typeName(),
                field.accessorSuffix(),
                packedType(),
                packedAsLong("packed"),
                field.shiftConstant(),
                field.maskConstant(),
                decodeUsage("encoded")
        );
    }

    public String generateSetMethod() {
        return """
                public static %s set%s(%s packed, %s value) {
                    validate%s(value);
                    return rawSet%s(packed, value);
                }
                """.formatted(
                packedType(),
                field.accessorSuffix(),
                packedType(),
                field.typeName(),
                field.accessorSuffix(),
                field.accessorSuffix()
        );
    }

    public String generateRawSetMethod() {
        return """
                public static %s rawSet%s(%s packed, %s value) {
                    long cleared = %s & ~(%s << %s);
                    long encoded = ((%s) & %s) << %s;
                    return %s;
                }
                """.formatted(
                packedType(),
                field.accessorSuffix(),
                packedType(),
                field.typeName(),
                packedAsLong("packed"),
                field.maskConstant(),
                field.shiftConstant(),
                encodeUsage("value"),
                field.maskConstant(),
                field.shiftConstant(),
                castPacked(withValidBit("cleared | encoded"))
        );
    }

    public String generateAddMethod() {
        if (!supportsAdd()) {
            return "";
        }

        return """
                public static %s add%s(%s packed, %s delta) {
                %s
                }
                """.formatted(
                packedType(),
                field.accessorSuffix(),
                packedType(),
                field.typeName(),
                indent(addBody())
        );
    }

    public String generateRawAddMethod() {
        if (!supportsAdd()) {
            return "";
        }

        return """
                public static %s rawAdd%s(%s packed, %s delta) {
                %s
                }
                """.formatted(
                packedType(),
                field.accessorSuffix(),
                packedType(),
                field.typeName(),
                indent(rawAddBody())
        );
    }

    public String generateValidateMethod() {
        return """
                private static void validate%s(%s value) {
                %s
                }
                """.formatted(
                field.accessorSuffix(),
                field.typeName(),
                indent(validationBody())
        );
    }

    public String generateEncodeMethod() {
        if (usesDirectCodec()) {
            return "";
        }

        return """
                private static long encode%s(%s value) {
                    return %s;
                }
                """.formatted(
                field.accessorSuffix(),
                field.typeName(),
                encodeExpression("value")
        );
    }

    public String generateDecodeMethod() {
        if (usesDirectCodec()) {
            return "";
        }

        return """
                private static %s decode%s(long encoded) {
                    return %s;
                }
                """.formatted(
                field.typeName(),
                field.accessorSuffix(),
                decodeExpression("encoded")
        );
    }

    public String generate() {
        return joinSections(
                generateGetMethod(),
                generateSetMethod(),
                generateRawSetMethod(),
                generateAddMethod(),
                generateRawAddMethod(),
                generateValidateMethod(),
                generateEncodeMethod(),
                generateDecodeMethod()
        );
    }

    private String addBody() {
        return switch (field.typeKind()) {
            case BYTE -> """
                    int result = Math.addExact((int) get%s(packed), (int) delta);
                    if (result < Byte.MIN_VALUE || result > Byte.MAX_VALUE) {
                        throw new ArithmeticException("Field '%s' overflow while adding delta.");
                    }
                    return rawSet%s(packed, (byte) result);
                    """.formatted(field.accessorSuffix(), field.fieldName(), field.accessorSuffix());
            case SHORT -> """
                    int result = Math.addExact((int) get%s(packed), (int) delta);
                    if (result < Short.MIN_VALUE || result > Short.MAX_VALUE) {
                        throw new ArithmeticException("Field '%s' overflow while adding delta.");
                    }
                    return rawSet%s(packed, (short) result);
                    """.formatted(field.accessorSuffix(), field.fieldName(), field.accessorSuffix());
            case INT, LONG -> """
                    return rawSet%s(packed, Math.addExact(get%s(packed), delta));
                    """.formatted(field.accessorSuffix(), field.accessorSuffix());
            default -> throw new IllegalStateException("Unsupported add type: " + field.typeKind());
        };
    }

    private String rawAddBody() {
        return """
                long encoded = ((%s >>> %s) & %s) + %s;
                long cleared = %s & ~(%s << %s);
                return %s;
                """.formatted(
                packedAsLong("packed"),
                field.shiftConstant(),
                field.maskConstant(),
                deltaAsLongExpression(),
                packedAsLong("packed"),
                field.maskConstant(),
                field.shiftConstant(),
                castPacked(withValidBit("cleared | ((encoded & %s) << %s)".formatted(field.maskConstant(), field.shiftConstant())))
        );
    }

    private String validationBody() {
        if (field.typeKind() == TypeKind.BOOLEAN) {
            return "// boolean is already restricted to one bit.";
        }

        if (field.typeKind() == TypeKind.LONG && field.bits() == 64) {
            return "// a full-width long needs no additional range check.";
        }

        if (field.typeKind() == TypeKind.CHAR) {
            return """
                    long encoded = (long) value;
                    if (encoded < 0L || encoded > %s) {
                        throw new IllegalArgumentException("Field '%s' is out of range for %d bits: " + (int) value);
                    }
                    """.formatted(field.maskConstant(), field.fieldName(), field.bits());
        }

        return """
                long encoded;
                try {
                    encoded = Math.addExact((long) value, %s);
                } catch (ArithmeticException exception) {
                    throw new IllegalArgumentException("Field '%s' overflow while applying valueOffset.", exception);
                }
                if (encoded < 0L || encoded > %s) {
                    throw new IllegalArgumentException("Field '%s' is out of range for %d bits: " + value);
                }
                """.formatted(
                field.valueOffsetConstant(),
                field.fieldName(),
                field.maskConstant(),
                field.fieldName(),
                field.bits()
        );
    }

    private String encodeExpression(String valueExpression) {
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

    private String encodeUsage(String valueExpression) {
        return usesDirectCodec()
                ? encodeExpression(valueExpression)
                : "encode%s(%s)".formatted(field.accessorSuffix(), valueExpression);
    }

    private String decodeExpression(String encodedExpression) {
        return switch (field.typeKind()) {
            case BOOLEAN -> "%s != 0L".formatted(encodedExpression);
            case BYTE -> "(byte) (%s - %s)".formatted(encodedExpression, field.valueOffsetConstant());
            case SHORT -> "(short) (%s - %s)".formatted(encodedExpression, field.valueOffsetConstant());
            case INT -> "(int) (%s - %s)".formatted(encodedExpression, field.valueOffsetConstant());
            case LONG -> field.valueOffset() == 0
                    ? encodedExpression
                    : "%s - %s".formatted(encodedExpression, field.valueOffsetConstant());
            case CHAR -> "(char) %s".formatted(encodedExpression);
            default -> throw new IllegalStateException("Unsupported decode type: " + field.typeKind());
        };
    }

    private String decodeUsage(String encodedExpression) {
        return usesDirectCodec()
                ? decodeExpression(encodedExpression)
                : "decode%s(%s)".formatted(field.accessorSuffix(), encodedExpression);
    }

    private boolean supportsAdd() {
        return switch (field.typeKind()) {
            case BYTE, SHORT, INT, LONG -> true;
            default -> false;
        };
    }

    private boolean usesDirectCodec() {
        return field.valueOffset() == 0;
    }

    private String packedType() {
        return storage.typeName();
    }

    private String packedAsLong(String packedExpression) {
        return switch (storage) {
            case BYTE, SHORT, INT -> "(((long) %s) & STORAGE_MASK)".formatted(packedExpression);
            case LONG -> packedExpression;
        };
    }

    private String castPacked(String expression) {
        return switch (storage) {
            case BYTE -> "(byte) (%s)".formatted(expression);
            case SHORT -> "(short) (%s)".formatted(expression);
            case INT -> "(int) (%s)".formatted(expression);
            case LONG -> expression;
        };
    }

    private String deltaAsLongExpression() {
        return switch (field.typeKind()) {
            case BYTE, SHORT, INT -> "((long) delta)";
            case LONG -> "delta";
            default -> throw new IllegalStateException("Unsupported delta type: " + field.typeKind());
        };
    }

    private String withValidBit(String expression) {
        if (!useValidBit) {
            return expression;
        }
        return "(" + expression + ") | VALID_MASK";
    }

    private String indent(String block) {
        return block.lines()
                .map(line -> line.isEmpty() ? "" : "    " + line)
                .collect(Collectors.joining("\n"));
    }

    private String joinSections(String... sections) {
        return Arrays.stream(sections)
                .filter(section -> section != null && !section.isBlank())
                .collect(Collectors.joining("\n\n"));
    }
}
