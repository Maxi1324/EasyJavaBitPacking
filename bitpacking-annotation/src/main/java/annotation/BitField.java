package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes one field inside a {@link BitPacked} specification.
 *
 * <p>{@link #bits()} defines how many bits are reserved for this field.
 * {@link #valueOffset()} shifts the logical value range before packing, which
 * allows small negative ranges to be stored in an unsigned bit field.</p>
 *
 * generates
 * {@code getRooms(packed)} to read the field
 * {@code setRooms(packed, value)} to write the field with validation
 * {@code rawSetRooms(packed, value)} to write the field without validation
 * {@code validateRooms(value)} as an internal range check
 * {@code encodeRooms(value)} and {@code decodeRooms(encoded)} when an explicit
 * encode/decode step is needed
 * {@code addRooms(packed, delta)} and {@code rawAddRooms(packed, delta)} for
 * numeric field types such as {@code byte}, {@code short}, {@code int}, and {@code long}</p>
 *
 * raw is always faster but unsafer
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface BitField {

    int bits();

    int valueOffset() default 0;
}
