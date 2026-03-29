package processor;

import annotation.BitField;
import annotation.BitPacked;
import annotation.PackedStorage;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

///  find all classes
///  generate for each class

@SupportedAnnotationTypes("annotation.BitPacked")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class BitPackingProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        // iterate over all bitpacked classes
        for (Element element : roundEnvironment.getElementsAnnotatedWith(BitPacked.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(element, "Can only use Bitpacked on Class");
            } else {
                TypeElement typeElement = (TypeElement) element;
                BitPacked bitPacked = typeElement.getAnnotation(BitPacked.class);
                PackedStorage storage = bitPacked.storage();
                boolean useValidBit = bitPacked.useValidBit();
                // get all information needed for generation and place in record
                List<PackedField> fields = collectPackedFields(typeElement);

                if (!fields.isEmpty()) {
                    int usedBits = fields.get(fields.size() - 1).shift() + fields.get(fields.size() - 1).bits();
                    if (useValidBit) {
                        usedBits += 1;
                    }

                    if (usedBits > storage.bitCount()) {
                        error(typeElement, "Bit layout needs " + usedBits + " bits but storage type " + storage.typeName() + " only provides " + storage.bitCount() + ".");
                        continue;
                    }

                    // generate the code and add new class file
                    PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);
                    String packageName = packageElement.isUnnamed() ? "" : packageElement.getQualifiedName().toString();
                    String generatedSimpleName = typeElement.getSimpleName().toString() + "Bit";
                    var generateClass = new GenerateBitpackingClass(generatedSimpleName, packageName, storage, useValidBit, fields);
                    try {
                        JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(qualifiedGeneratedName(element), typeElement);
                        try (Writer writer = sourceFile.openWriter()) {
                            writer.write(generateClass.generatedClass());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }

        return true;
    }

    /// get from a bitpacked class all bitfields
    /// and extracts all information needed and finds a mapping
    private List<PackedField> collectPackedFields(TypeElement typeElement) {
        List<PackedField> fields = new ArrayList<>();
        int nextShift = 0;
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement fieldElement = (VariableElement) enclosedElement;
                BitField bitField = fieldElement.getAnnotation(BitField.class);

                if (bitField != null) {
                    String fieldName = fieldElement.getSimpleName().toString();
                    int bits = bitField.bits();
                    // extract information use last nextShift
                    fields.add(new PackedField(fieldName, fieldElement.asType().toString(),
                            fieldElement.asType().getKind(), bits, nextShift, bitField.valueOffset()
                    ));
                    // move next shift by current bits
                    nextShift += bits;
                }
            }
        }
        return fields;
    }

    private String qualifiedGeneratedName(Element element) {
        TypeElement typeElement = (TypeElement) element;
        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);
        String packageName;
        if (packageElement.isUnnamed()) {
            packageName = "";
        } else {
            packageName = packageElement.getQualifiedName().toString();
        }
        String generatedSimpleName = typeElement.getSimpleName().toString() + "Bit";
        if (packageName.isEmpty()) {
            return generatedSimpleName;
        }
        return packageName + "." + generatedSimpleName;
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
