package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a bitpacking specification for the annotation processor.
 *
 * <p>Usage:
 * Place this annotation on a class whose fields are annotated with {@link BitField}.
 * Each annotated field becomes part of the packed layout in declaration order.</p>
 *
 * <p>Generated class:
 * For a specification class for example{@code HausSpec}, the processor generates
 * {@code HausSpecBit} in the same package.</p>
 *
 * <p>The generated class contains:
 * {@code pack(...)} and {@code rawPack(...)} for packing all fields
 * {@code getX(...)} and {@code setX(...)} for every annotated field
 * {@code rawSetX(...)} for writing a field without validation
 * {@code addX(...)} and {@code rawAddX(...)} for numeric fields
 * shared constants such as storage size, total used bits, masks, and shifts</p>
 *
 * <p>{@link #storage()} selects the primitive type used for the packed value.
 * {@link #useValidBit()} reserves one extra bit that is always set to {@code 1}
 * whenever a value is packed or updated through generated raw methods.</p>
 *
 * valid bit if activa can be used to check != 0 for valid or notnull
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BitPacked {
    PackedStorage storage() default PackedStorage.LONG;

    boolean useValidBit() default false;
}
