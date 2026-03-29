# EasyJavaBitPacking

### Context

Bit packing stores multiple logical values inside one primitive such as
`byte`, `short`, `int`, or `long`.

This is useful when:

- the full range of a primitive is not needed for every value
- heap allocations should be avoided
- compact data layouts matter

The downside is that handwritten bitpacking code is repetitive, hard to read,
and easy to break with small masking or shifting mistakes.

### Goal

This project provides an annotation-based Java annotation processor that
generates bitpacking helper classes automatically.

You describe a packed type with a normal Java class and annotate its fields.
The processor then generates a utility class with packing, unpacking, and
update methods.

### Modules

- `bitpacking-annotation`: contains `@BitPacked`, `@BitField`, and `PackedStorage`
- `bitpacking-processor`: contains the annotation processor and code generator
- `bitpacking-demo`: contains example specs and tests

### How To Use

### Maven Setup

In a consumer project, add the annotation module as a normal dependency and the
processor module as a provided dependency:

```xml
<dependencies>
    <dependency>
        <groupId>org.maxi.bitpacking</groupId>
        <artifactId>bitpacking-annotation</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

    <dependency>
        <groupId>org.maxi.bitpacking</groupId>
        <artifactId>bitpacking-processor</artifactId>
        <version>1.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Also enable the annotation processor in the Maven compiler plugin:

```xml
<build>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <annotationProcessors>
                    <annotationProcessor>processor.BitPackingProcessor</annotationProcessor>
                </annotationProcessors>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### IntelliJ IDEA

For Maven projects, IntelliJ IDEA should usually detect annotation processors
from the `pom.xml` automatically after importing the project.

If the generated `*Bit` classes are still marked as missing in the IDE:

1. Reimport the Maven project.
2. Open `Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors`.
3. Make sure `Enable annotation processing` is enabled.
4. Keep `Obtain processors from project classpath` enabled.
5. Rebuild the project.

If Maven builds work but IntelliJ's internal build still complains, a practical
fallback is to delegate build and run actions to Maven.

### Current Behavior

- storage type can be selected with `PackedStorage.BYTE`, `SHORT`, `INT`, or `LONG`
- generated methods use the selected storage type as parameter and return type
- `pack(...)` performs range checks
- `rawPack(...)` skips validation and truncates to the configured field widths
- `get`, `set`, `rawSet`, `add`, and `rawAdd` are generated per field where applicable

### How It Works

1. Find every class annotated with `@BitPacked`
2. Collect all fields annotated with `@BitField`
3. Compute bit positions from declaration order
4. Validate that the layout fits into the selected storage type
5. Generate one `*Bit` helper class per specification

### Example

Specification:

```java
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
```

Generated class:

- `HausSpecBit` is generated in the same package
- `rooms`, `floors`, and `garage` are packed in declaration order
- one extra valid bit is reserved and always set to `1`

Usage:

```java
short packed = HausSpecBit.pack(12, 3, true);
int rooms = HausSpecBit.getRooms(packed);
short updated = HausSpecBit.addRooms(packed, 2);
```
