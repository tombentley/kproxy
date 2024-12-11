/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import io.kroxylicious.tools.schema.model.SchemaObject;
import io.kroxylicious.tools.schema.model.SchemaObjectBuilder;
import io.kroxylicious.tools.schema.model.SchemaType;
import io.kroxylicious.tools.schema.model.XKubeListType;

import edu.umd.cs.findbugs.annotations.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

class CodeGenTest {

    CodeGen codeGen;

    {
        Diagnostics diagnostics = new Diagnostics();
        codeGen = new CodeGen(diagnostics,
                new IdVisitor(),
                Map.of(),
                "edu.umd.cs.findbugs.annotations.Nullable",
                "edu.umd.cs.findbugs.annotations.NonNull",
                List.of(),
                new BeanPropertyStrategy(),
                false,
                List.of());
    }

    SchemaObject emptyTypes = new SchemaObjectBuilder().withType().build();
    SchemaObject nullSchema = new SchemaObjectBuilder().withType(SchemaType.NULL).build();
    SchemaObject booleanSchema = new SchemaObjectBuilder().withType(SchemaType.BOOLEAN).build();
    SchemaObject stringSchema = new SchemaObjectBuilder().withDescription("A string").withType(SchemaType.STRING).build();
    SchemaObject integerSchema = new SchemaObjectBuilder().withType(SchemaType.INTEGER).build();
    SchemaObject numberSchema = new SchemaObjectBuilder().withType(SchemaType.NUMBER).build();
    SchemaObject emptyObjectSchema = new SchemaObjectBuilder().withType(SchemaType.OBJECT).withJavaType("EmptyObject").build();
    SchemaObject stringArrayListSchema = new SchemaObjectBuilder().withType(SchemaType.ARRAY).withItems(stringSchema).build();
    SchemaObject integerArrayListSchema = new SchemaObjectBuilder().withType(SchemaType.ARRAY).withItems(integerSchema).build();
    SchemaObject stringArraySetSchema = new SchemaObjectBuilder(stringArrayListSchema).withXKubernetesListType(XKubeListType.SET).build();

    @Test
    void genTypeName() {
        String pkg = "foo";
        assertThat(codeGen.genTypeName(pkg, null, emptyTypes)).hasToString("java.lang.Object");
        assertThat(codeGen.genTypeName(pkg, null, nullSchema)).hasToString("java.lang.Object");
        assertThat(codeGen.genTypeName(pkg, null, booleanSchema)).hasToString("java.lang.Boolean");
        assertThat(codeGen.genTypeName(pkg, null, stringSchema)).hasToString("java.lang.String");
        assertThat(codeGen.genTypeName(pkg, null, integerSchema)).hasToString("java.lang.Long");
        assertThat(codeGen.genTypeName(pkg, null, numberSchema)).hasToString("java.lang.Double");
        assertThat(codeGen.genTypeName(pkg, null, stringArrayListSchema)).hasToString("java.util.List<java.lang.String>");
        assertThat(codeGen.genTypeName(pkg, null, integerArrayListSchema)).hasToString("java.util.List<java.lang.Long>");
        assertThat(codeGen.genTypeName(pkg, null, stringArraySetSchema)).hasToString("java.util.Set<java.lang.String>");
        assertThat(codeGen.genTypeName(pkg, null, emptyObjectSchema)).hasToString("foo.EmptyObject");
    }

    private static final String HEADER = """
            /*
             * Copyright Kroxylicious Authors.
             *
             * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
             */

            """;

    private void assertGeneratedCode(Path srcdir,
                                     List<TypeAnnotator> typeAnnotators,
                                     List<PropertyAnnotator> propertyAnnotators)
            throws IOException {
        // First assert that the generate code matches the expected files
        assertGeneratedCodeMatches(srcdir, typeAnnotators, propertyAnnotators);
        // Then assert that the expected files can be compiled with a java compiler
        // Because `generated == expected` this means the generated must be legal java source code
        compileJavaFilesBeneath(srcdir.getParent());
        javadocJavaFilesBeneath(srcdir.getParent());

    }

    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private void f(Path classdir, Path instanceYaml, String className) throws IOException {
        // TODO read instance as JsonNode
        // TODO check JsonNode instance against schema (i.e. check that the instance we're testing with is actually valid)

        var cl = new URLClassLoader(new URL[]{ classdir.toUri().toURL() }, getClass().getClassLoader());
        try {
            var c = Class.forName(className, true, cl);
            // deserialize into POJOs (testing the Jackson annotations)
            var o = YAML_MAPPER.readValue(instanceYaml.toFile(), c);

            // serialize POJOs as JSONNode (testing the Jackson annotations)
            String s = YAML_MAPPER.writeValueAsString(o);
            // compare == (annotations provide roundtrip fidelity)
            var roundtripped = YAML_MAPPER.readTree(s);

            var instanceNodes = YAML_MAPPER.readTree(instanceYaml.toFile());

            assertThat(roundtripped)
                    .describedAs("Expect JSON roundtripped via POJO to be same as original instance")
                    .isEqualTo(instanceNodes);
            // assertThat(roundtripped.equals((x, y) -> {
            // if (x.equals(y)) {
            // return 0;
            // }
            // else {
            // return 1;
            // }
            // }, instanceNodes)).isTrue();

            // deserialize into POJOs 2nd time, compare .equals, .hashCode and .toString (testing those methods)
            var o2 = YAML_MAPPER.readValue(instanceYaml.toFile(), c);
            assertThat(o).isEqualTo(o2);
            assertThat(o).hasSameHashCodeAs(o2);
            assertThat(o).hasToString(o2.toString());
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static SchemaCompiler parseDiagnostics(String dir) {
        File src = new File(dir);
        Path path = new File("src/test/resources").toPath();
        SchemaCompiler schemaCompiler = new SchemaCompiler(
                List.of(path),
                List.of(path.relativize(src.toPath()).toString().replace("/", ".")),
                null,
                Map.of(),
                List.of(),
                new RecordPropertyStrategy(),
                false,
                List.of());
        schemaCompiler.parse();
        return schemaCompiler;
    }

    private static SchemaCompiler genDiagnostics(String dir) {
        File src = new File(dir);
        Path path = new File("src/test/resources").toPath();
        SchemaCompiler schemaCompiler = new SchemaCompiler(
                List.of(path),
                List.of(path.relativize(src.toPath()).toString().replace("/", ".")),
                null,
                Map.of(),
                List.of(),
                new RecordPropertyStrategy(),
                false,
                List.of());
        List<SchemaInput> parse = schemaCompiler.parse();
        assertThat(schemaCompiler.diagnostics.getNumFatals()).isZero();
        assertThat(schemaCompiler.diagnostics.getNumErrors()).isZero();
        assertThat(schemaCompiler.diagnostics.getNumWarnings()).isZero();
        try {
            schemaCompiler.gen(parse).toList();
        }
        catch (FatalException e) {
            // pass
        }
        return schemaCompiler;
    }

    private static void assertGeneratedCodeMatches(Path src,
                                                   List<TypeAnnotator> typeAnnotators,
                                                   List<PropertyAnnotator> propertyAnnotators) {
        var units = generate(src, typeAnnotators, propertyAnnotators);

        compare(src, units);

    }

    private static void compare(Path src, List<CompilationUnit> units) {
        Map<String, List<CompilationUnit>> collect = units.stream().collect(Collectors.groupingBy(SchemaCompiler::javaFileName));
        assertThat(collect).hasKeySatisfying(new Condition<>(
                filename -> filename.matches("[A-Z][a-zA-Z0-9_$]*\\.java"),
                "Valid .java filename"));
        assertThat(collect).hasValueSatisfying(new Condition<>(
                unitsForFile -> unitsForFile.size() == 1,
                "No colliding units"));
        collect.forEach((javaFilename, cus) -> {
            File expectedJavaFile = new File(src.toFile(), javaFilename);
            assertThat(expectedJavaFile)
                    .describedAs("Unexpected java source output (or expected output java file doesn't exist)")
                    .exists();
            try {
                // The following can be uncomments to bulk-update the expected java files
                // following a change to the code generator
                // USE WITH CAUTION ;-)
                // Files.writeString(expectedJavaFile.toPath(), HEADER + cus.get(0).toString());
                String javaSrc = Files.readString(expectedJavaFile.toPath()).trim();
                assertThat(cus).singleElement()
                        .isNotNull()
                        .extracting(compilationUnit -> HEADER + compilationUnit.toString().trim())
                        .describedAs("Java source output differs from expected output in " + expectedJavaFile)
                        .isEqualTo(javaSrc);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @NonNull
    private static List<CompilationUnit> generate(Path src, List<TypeAnnotator> typeAnnotators, List<PropertyAnnotator> propertyAnnotators) {
        Path path = new File("src/test/resources").toPath();
        SchemaCompiler schemaCompiler = new SchemaCompiler(
                List.of(path),
                List.of(path.relativize(src).toString().replace("/", ".")),
                null,
                Map.of(),
                typeAnnotators,
                new RecordPropertyStrategy(),
                false,
                propertyAnnotators);
        List<SchemaInput> parse = schemaCompiler.parse();
        var units = schemaCompiler.gen(parse).toList();

        assertThat(schemaCompiler.diagnostics.getNumFatals()).describedAs("Expect 0 fatal errors").isZero();
        assertThat(schemaCompiler.diagnostics.getNumErrors()).describedAs("Expect 0 errors").isZero();
        // TODO assertThat(schemaCompiler.diagnostics.getNumWarnings()).describedAs("Expect 0 warnings").isZero();
        return units;
    }

    /**
     * Compile the *.java files found beneath the given path.
     * Throw away the generated .class files
     * @param path
     * @throws IOException
     */
    private static Path compileJavaFilesBeneath(Path path) throws IOException {
        var outputDir = Files.createTempDirectory(CodeGenTest.class.getSimpleName());
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();
        try (var fileManager = compiler.getStandardFileManager(diagnosticListener, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromPaths(javaFilesBeneath(path));
            Boolean call = compiler.getTask(null,
                    fileManager,
                    diagnosticListener,
                    List.of("-d", outputDir.toString(), "-proc:none", "-Werror", "-Xlint:all"),
                    null,
                    compilationUnits1).call();
            assertThat(diagnosticListener.getDiagnostics())
                    .describedAs("The java source code should compile without errors")
                    .isEmpty();
            assertThat(call)
                    .describedAs("The compile task should return true")
                    .isTrue();
        }
        return outputDir;
    }

    /**
     * Compile the *.java files found beneath the given path.
     * Throw away the generated .class files
     * @param path
     * @throws IOException
     */
    private static void javadocJavaFilesBeneath(Path path) throws IOException {
        var outputDir = Files.createTempDirectory(CodeGenTest.class.getSimpleName());
        var docTool = ToolProvider.getSystemDocumentationTool();
        try (var fileManager = docTool.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromPaths(javaFilesBeneath(path));
            assertThat(
                    docTool.getTask(null, fileManager, null, null, List.of("-Xdoclint:all", "-Werror", "-public", "-d", outputDir.toString()), compilationUnits1).call())
                    .describedAs("The javadoc should be processed without errors")
                    .isTrue();
        }
    }

    @NonNull
    private static ArrayList<Path> javaFilesBeneath(Path start) throws IOException {
        var result = new ArrayList<Path>();
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(
                                             Path file,
                                             BasicFileAttributes attrs)
                    throws IOException {
                if (file.toString().endsWith(".java")) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    @TestFactory
    Stream<DynamicNode> valid() {
        return Stream.of("src/test/resources/empty",
                "src/test/resources/scalars",
                "src/test/resources/arrays",
                "src/test/resources/maps",
                "src/test/resources/anonymous",
                "src/test/resources/trickynaming",
                "src/test/resources/xref",
                "src/test/resources/junctor",
                "src/test/resources/open").map(Path::of).map(srcdir -> {

                    var result = Stream.<DynamicNode> builder();

                    List<CompilationUnit> compilationUnits = null;

                    try {
                        compilationUnits = generate(srcdir, List.of(), List.of());
                        // TODO DynamicTest.dynamicTest("generated code compiles with javac", () -> {}),
                        // DynamicTest.dynamicTest("generated code doc compiles with javadoc", () -> {}),
                        // // for each instance YAML
                        // DynamicTest.dynamicTest("generated code equals(), hashCode() and toString", () -> {}),
                        // DynamicTest.dynamicTest("generated code (de)serialization", () -> {})
                    }
                    catch (Exception | AssertionError e) {
                        result.add(DynamicTest.dynamicTest("generate(" + srcdir + ")", () -> {
                            throw e;
                        }));
                    }

                    if (compilationUnits != null) {
                        var finalCu = compilationUnits;
                        result.add(DynamicTest.dynamicTest("generate(" + srcdir + ")", () -> {
                        }));

                        result.add(DynamicTest.dynamicTest("compare(generate(" + srcdir + "))", () -> {
                            compare(srcdir, finalCu);
                        }));

                        Path tmpSrcDir = null;
                        try {
                            var dir = Files.createTempDirectory(CodeGenTest.class.getName());
                            for (var cu : finalCu) {
                                var srcFile = Files.createDirectories(dir.resolve(cu.getPackageDeclaration().get().getNameAsString().replace(".", "/")));
                                Path resolve = srcFile.resolve(cu.getTypes().get(0).getNameAsString() + ".java");
                                Files.writeString(resolve, cu.toString());
                            }
                            tmpSrcDir = dir;
                        }
                        catch (Exception | AssertionError e) {
                            result.add(DynamicTest.dynamicTest("write generated source", () -> {
                                throw e;
                            }));
                        }
                        final Path finalGeneratedSrcDir = tmpSrcDir;

                        if (tmpSrcDir != null) {
                            result.add(DynamicTest.dynamicTest("javadoc(generate(" + srcdir + "))", () -> {
                                javadocJavaFilesBeneath(finalGeneratedSrcDir);
                            }));

                            Path classdir = null;
                            try {
                                classdir = compileJavaFilesBeneath(finalGeneratedSrcDir);
                                result.add(DynamicTest.dynamicTest("javac(generate(" + srcdir + "))", () -> {
                                }));
                            }
                            catch (Exception | AssertionError e) {
                                result.add(DynamicTest.dynamicTest("javac(generate(" + srcdir + "))", () -> {
                                    throw e;
                                }));
                            }

                            if (classdir != null) {
                                var finalClassdir = classdir;
                                var pattern = Pattern.compile("instance-([A-Za-z0-9]*)\\.yaml");
                                // TODO walk srcdir parsing .yamls finding ones without a $schema that's Draft 4
                                try {
                                    Files.walkFileTree(srcdir,
                                            new SimpleFileVisitor<Path>() {
                                                @Override
                                                public FileVisitResult visitFile(
                                                                                 Path file,
                                                                                 BasicFileAttributes attrs)
                                                        throws IOException {
                                                    Matcher matcher = pattern.matcher(file.getFileName().toString());
                                                    if (matcher.matches()) {
                                                        try {
                                                            // TODO read as JsonNode and validate against the "root schema"
                                                            f(finalClassdir, file,
                                                                    finalCu.get(0).getPackageDeclaration().get().getName().asString() + "." + matcher.group(1));
                                                            result.add(DynamicTest.dynamicTest("generated code works for instance " + file, () -> {
                                                            }));
                                                        }
                                                        catch (Exception | AssertionError e) {
                                                            result.add(DynamicTest.dynamicTest("generated code works for instance " + file, () -> {
                                                                throw e;
                                                            }));
                                                        }
                                                    }
                                                    return FileVisitResult.CONTINUE;
                                                }
                                            });
                                }
                                catch (Exception | AssertionError e) {
                                    result.add(DynamicTest.dynamicTest("walk " + srcdir, () -> {
                                        throw e;
                                    }));
                                }

                            }
                        }
                    }
                    return DynamicContainer.dynamicContainer(srcdir.toString(), result.build());
                });

    }

    @Test
    void customAnnotations() throws IOException {
        assertGeneratedCode(Path.of("src/test/resources/customannotations"),
                List.of(new TypeAnnotator() {
                    @Override
                    public List<AnnotationExpr> annotateClass(Diagnostics diagnostics, SchemaObject typeSchema) {
                        return List.of(new SingleMemberAnnotationExpr(new Name("customannotations.Custom"), new StringLiteralExpr("class")));
                    }
                }),
                List.of(
                        new PropertyAnnotator() {
                            @Override
                            public List<AnnotationExpr> annotateField(
                                                                      Diagnostics diagnostics,
                                                                      String property,
                                                                      SchemaObject propertySchema) {
                                return List.of(new SingleMemberAnnotationExpr(new Name("customannotations.Custom"), new StringLiteralExpr("field")));
                            }

                            @Override
                            public List<AnnotationExpr> annotateConstructorParameter(
                                                                                     Diagnostics diagnostics,
                                                                                     String property,
                                                                                     SchemaObject propertySchema) {
                                return List.of(new SingleMemberAnnotationExpr(new Name("customannotations.Custom"), new StringLiteralExpr("ctorParameter")));
                            }

                            @Override
                            public List<AnnotationExpr> annotateAccessor(
                                                                         Diagnostics diagnostics,
                                                                         String property,
                                                                         SchemaObject propertySchema) {
                                return List.of(new SingleMemberAnnotationExpr(new Name("customannotations.Custom"), new StringLiteralExpr("accessor")));
                            }

                            @Override
                            public List<AnnotationExpr> annotateMutator(
                                                                        Diagnostics diagnostics,
                                                                        String property,
                                                                        SchemaObject propertySchema) {
                                return List.of(new SingleMemberAnnotationExpr(new Name("customannotations.Custom"), new StringLiteralExpr("mutator")));
                            }

                            @Override
                            public List<AnnotationExpr> annotateMutatorParameter(
                                                                                 Diagnostics diagnostics,
                                                                                 String property,
                                                                                 SchemaObject propertySchema) {
                                return List.of(new SingleMemberAnnotationExpr(new Name("customannotations.Custom"), new StringLiteralExpr("mutatorParameter")));
                            }
                        }));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "src/test/resources/nonschemafile",
            "src/test/resources/badschemaversion"
    })
    void warnings(String pathname) {
        assertThat(parseDiagnostics(pathname))
                .satisfies(schemaCompiler -> {
                    assertThat(schemaCompiler.numFatals()).isZero();
                    assertThat(schemaCompiler.numErrors()).isZero();
                    assertThat(schemaCompiler.numWarnings()).isEqualTo(1);
                });
    }

    @Test
    void unionTypeIsFatal() {
        String pathname = "src/test/resources/uniontype";
        assertThat(genDiagnostics(pathname))
                .satisfies(schemaCompiler -> {
                    assertThat(schemaCompiler.numFatals()).isEqualTo(1);
                    assertThat(schemaCompiler.numErrors()).isZero();
                    assertThat(schemaCompiler.numWarnings()).isZero();
                });
    }

}
