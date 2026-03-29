package processor;

import annotation.PackedStorage;

import java.util.List;
import java.util.stream.Collectors;

/// get a list of bitFields
/// checks if everything is setup correctly
/// generate mapping to the specified fields
public class GenerateBitpackingClass {
    private final GenerateBitpackingConstants constantsGen;
    private final GenerateBitpackingPack packGen;
    private final List<GenerateBitpackingField> fields;
    private final String classname;
    private final String packageName;

    public GenerateBitpackingClass(String classname, String packageName, PackedStorage storage, boolean useValidBit, List<PackedField> fields) {
        this.constantsGen = new GenerateBitpackingConstants(storage, useValidBit, fields);
        this.packGen = new GenerateBitpackingPack(storage, useValidBit, fields);
        this.fields = fields.stream().map(field -> new GenerateBitpackingField(storage, useValidBit, field)).toList();
        this.classname = classname;
        this.packageName = packageName;
    }

    public String generatedClass() {
        String packageLine = packageName.isBlank() ? "" : "package " + packageName + ";\n\n";
        return """
                %s
                
                public final class %s {
                    %s

                    %s
                
                    %s
                }
                """.formatted(packageLine, classname, constantsGen.generate(), packGen.generate(),
                fields.stream().map(GenerateBitpackingField::generate).collect(Collectors.joining("\n\n")));
    }
}
