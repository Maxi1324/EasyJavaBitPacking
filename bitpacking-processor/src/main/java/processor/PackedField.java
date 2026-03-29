package processor;

import javax.lang.model.type.TypeKind;

/// Stores all information needed for generation
///
/// fieldName           original field name example id
/// accessorSuffix      derived from fieldName, example Id for getId
/// constantBaseName    derived from fieldName, example ID
/// typeName            the type as string "int" "boolean"
/// typeKind            compiler type information
/// bits                how many bits are used
/// shift               offset from the start
/// valueOffset         move values around
public final class PackedField {

    private final String fieldName;
    private final String accessorSuffix;
    private final String constantBaseName;
    private final String typeName;
    private final TypeKind typeKind;
    private final int bits;
    private final int shift;
    private final int valueOffset;

    public PackedField(String fieldName,
                       String typeName,
                       TypeKind typeKind,
                       int bits,
                       int shift,
                       int valueOffset) {
        this.fieldName = fieldName;
        this.accessorSuffix = capitalize(fieldName);
        this.constantBaseName = constantName(fieldName);
        this.typeName = typeName;
        this.typeKind = typeKind;
        this.bits = bits;
        this.shift = shift;
        this.valueOffset = valueOffset;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String constantName(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (Character.isUpperCase(current) && index > 0) {
                builder.append('_');
            }

            if (Character.isLetterOrDigit(current)) {
                builder.append(Character.toUpperCase(current));
            } else {
                builder.append('_');
            }
        }

        return builder.toString();
    }


    public String fieldName() {
        return fieldName;
    }

    public String accessorSuffix() {
        return accessorSuffix;
    }

    public String constantBaseName() {
        return constantBaseName;
    }

    public String typeName() {
        return typeName;
    }

    public TypeKind typeKind() {
        return typeKind;
    }

    public int bits() {
        return bits;
    }

    public int shift() {
        return shift;
    }

    public int valueOffset() {
        return valueOffset;
    }

    public String bitsConstant() {
        return constantBaseName + "_BITS";
    }

    public String shiftConstant() {
        return constantBaseName + "_SHIFT";
    }

    public String maskConstant() {
        return constantBaseName + "_MASK";
    }

    public String valueOffsetConstant() {
        return constantBaseName + "_VALUE_OFFSET";
    }
}
