/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;

import io.kroxylicious.tools.schema.model.SchemaKeyword;
import io.kroxylicious.tools.schema.model.SchemaObject;
import io.kroxylicious.tools.schema.model.VisitorException;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A transpiler which accepts JSON Schemas (Wright draft 4) and
 * generates {@code .java} source code for Jackson-annotated POJOs
 * that can represent instance values that conform to that schema.
 */
public class SchemaCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaCompiler.class);

    private final List<Path> srcPaths;
    private final Path dst;
    private final CodeGen codeGen;
    private final YAMLMapper mapper;
    private final IdVisitor idVisitor;
    private final @Nullable String header;
    public final Diagnostics diagnostics;
    private final List<String> packages;
    private final Catalog catalog;
    private final Map<String, String> existingClasses;

    public SchemaCompiler(List<Path> srcPaths,
                          Path dst,
                          List<Path> classpath,
                          @Nullable List<String> packages,
                          @Nullable String header,
                          Map<String, String> existingClasses,
                          List<TypeAnnotator> typeAnnotators,
                          PropertyStrategy propertyStrategy,
                          boolean optAccessor,
                          List<PropertyAnnotator> propertyAnnotators) {
        this.diagnostics = new Diagnostics();
        this.srcPaths = Objects.requireNonNull(srcPaths);
        this.dst = Objects.requireNonNull(dst);
        this.mapper = new YAMLMapper();
        this.catalog = new Catalog(this.mapper, Objects.requireNonNull(classpath));
        this.packages = packages;
        if (header != null) {
            header = maybeWrapInComment(header);
        }
        this.header = header;
        this.existingClasses = existingClasses;

        this.idVisitor = new IdVisitor();
        this.codeGen = new CodeGen(diagnostics,
                idVisitor,
                catalog,
                existingClasses,
                "edu.umd.cs.findbugs.annotations.Nullable",
                "edu.umd.cs.findbugs.annotations.NonNull",
                typeAnnotators,
                propertyStrategy,
                optAccessor,
                propertyAnnotators);
    }

    public Path dst() {
        return dst;
    }

    @NonNull
    private static String maybeWrapInComment(@NonNull String header) {
        // Is the header already a comment? Let's parse it as java and find out
        try {
            var cu = StaticJavaParser.parse(header);
            if (cu.getChildNodes().stream().anyMatch(node -> !(node instanceof Comment))) {
                // The header wasn't (just) a comment, and was valid java (!) => we should turn it into a comment
                throw new ParseProblemException(new RuntimeException());
            }
        }
        catch (ParseProblemException e) {
            // The header is not already a comment
            var nl = System.lineSeparator();
            header = "/*" + nl + header.lines()
                    .map(line -> " * " + line.replace("*/", "* /"))
                    .collect(Collectors.joining(nl)) + nl + " */" + nl;
        }
        return header;
    }

    public List<SchemaInput> parse() {

        return srcPaths.stream().flatMap(srcPath -> {
            LOGGER.debug("Parsing {}", srcPath.toAbsolutePath());
            try (Stream<Path> walkPaths = Files.walk(srcPath)) {
                return walkPaths.flatMap(file -> {
                    String string = file.getFileName().toString();
                    if (Files.isRegularFile(file)
                            && (string.endsWith(".yaml")
                                    || string.endsWith(".yml")
                                    || string.endsWith(".json"))
                            && (packages == null
                                    || packages.contains(srcPath.relativize(file).getParent().toString().replace("/", ".")))) {
                        return parseSchema(srcPath, file);
                    }
                    return Stream.empty();
                }).toList().stream(); // Need to materialise this list else the try(walkPaths) will close Stream before anything pulled through the stream
            }
            catch (IOException e) {
                throw new UncheckedIOException("Unable to walk source directory " + srcPath, e);
            }
        })
                .flatMap(this::resolve)
                .map(input -> {
                    diagnostics.debug("ID index {}", idVisitor.toString());
                    return input;
                })
                .flatMap(this::type)
                .toList();
    }

    private Stream<SchemaInput> parseSchema(Path srcPath, Path schemaFile) {
        try {

            LOGGER.debug("Parsing {}", schemaFile);
            var tree = mapper.readTree(schemaFile.toFile());

            JsonNode schemaKeywordNode = tree.path(SchemaKeyword.SCHEMA);
            if (schemaKeywordNode.isMissingNode()) {
                diagnostics.reportWarning("Ignoring non-schema file: {}", schemaFile);
                return Stream.empty();
            }
            if (!"http://json-schema.org/draft-04/schema#".equals(schemaKeywordNode.asText("http://json-schema.org/draft-04/schema#"))) {
                diagnostics.reportWarning("Ignoring non-schema file: {}", schemaFile);
                return Stream.empty();
            }

            var relPath = srcPath.relativize(schemaFile);

            String pkg = StreamSupport.stream(relPath.getParent().spliterator(), false)
                    .map(Path::toString)
                    .collect(Collectors.joining("."));

            var rootSchema = mapper.convertValue(tree, SchemaObject.class);

            SchemaInput schemaInput = new SchemaInput(schemaFile, pkg, rootSchema);

            // Build the map of absolute URI identifiers to schema
            schemaInput.visitSchemas(diagnostics, idVisitor);

            return Stream.of(schemaInput);
        }
        catch (IOException | IllegalArgumentException | VisitorException e) {
            diagnostics.reportError("Unable to read source file {}: {}", schemaFile, e.getMessage());
            return Stream.empty();
        }
    }

    /**
     * Execute the resolve phase.
     *
     * <p>Precondition: the id phase has executed on all the inputs</p>
     * <p>Postcondition: After this phase is complete all external $ref nodes will have a type model</p>
     */
    private Stream<SchemaInput> resolve(SchemaInput input) {
        var resolveVisitor = new ResolveVisitor(diagnostics, idVisitor, catalog);
        input.visitSchemas(diagnostics, resolveVisitor);
        return Stream.of(input);
    }

    /**
     * Execute the typing phase
     *
     * <p>Precondition: the resolve phase has executed on all the inputs</p>
     * <p>Postcondition: After this phase is complete all non-$ref nodes that will have a class declaration generated
     * will have a type model</p>
     */
    private Stream<SchemaInput> type(SchemaInput input) {
        // We should now be able to resolve local $ref
        String rootClass = input.schemaPath().getFileName().toString().replaceAll("\\.yaml$", "");
        var typeNameVisitor = new TypeNameVisitor1(diagnostics, input.pkg(), rootClass, existingClasses, catalog);
        input.visitSchemas(diagnostics, typeNameVisitor);

        if (input.pkg().isEmpty()) {
            diagnostics.reportError("Schema file '{}' would be in the root package: move it to a subdirectory", input.schemaPath());
            return Stream.of();
        }
        return Stream.of(input);
    }

    /**
     * Execute the code generation phase.
     *
     * <p>Precondition: the typing has executed on all the inputs</p>
     */
    public Stream<CodeGen.Unit> gen(List<SchemaInput> inputs) {

        return inputs.stream()
                .flatMap(input -> {
                    try {
                        return codeGen.genDecls(input).stream();
                    }
                    catch (VisitorException e) {
                        diagnostics.reportError("Error: {}", e.getMessage(), e);
                        return Stream.empty();
                    }
                });

    }

    /**
     * Write the generated code to files, assuming the gen phase has executed
     */
    public void write(List<CodeGen.Unit> units) {

        units.forEach(unit -> {
            writeCompilationUnit(unit.compilationUnit());

        });
        Map<URI, List<CodeGen.Unit>> collect = units.stream().collect(Collectors.groupingBy(CodeGen.Unit::schemaUri));
        collect.forEach((schemaUri, typeModels) -> {
            try {
                diagnostics.debug("Writing schema models for {} to {}", schemaUri, dst);
                catalog.writeTypeDecls(schemaUri,
                        typeModels.stream().map(CodeGen.Unit::typeModel).toList(),
                        dst);
            }
            catch (IOException e) {
                diagnostics.reportError("Error writing models", e);
            }
        });

    }

    private void writeCompilationUnit(CompilationUnit compilationUnit) {
        String pkg = packageName(compilationUnit);
        String dirname = pkg.replace(".", File.separator);
        Path parent = dst.resolve(dirname);
        String javaFileName = javaFileName(compilationUnit);
        Path javaFile = parent.resolve(javaFileName);

        try {
            Files.createDirectories(parent);
        }
        catch (IOException e) {
            diagnostics.reportFatal("Unable to create dst directory {}", parent, e);
        }

        try {
            if (header != null) {
                Files.writeString(javaFile, header);
                Files.writeString(javaFile, compilationUnit.toString(), StandardOpenOption.APPEND);
            }
            else {
                Files.writeString(javaFile, compilationUnit.toString());
            }
        }
        catch (IOException e) {
            diagnostics.reportFatal("Unable to write output java file {}", javaFile, e);
        }
    }

    public int numFatals() {
        return diagnostics.getNumFatals();
    }

    public int numErrors() {
        return diagnostics.getNumErrors();
    }

    public int numWarnings() {
        return diagnostics.getNumWarnings();
    }

    @NonNull
    static String javaFileName(CompilationUnit compilationUnit) {
        return compilationUnit.getTypes().stream()
                .filter(t -> t.isPublic() && t.isTopLevelType())
                .findFirst()
                .map(NodeWithSimpleName::getNameAsString)
                .orElseThrow() + ".java";
    }

    private static String packageName(CompilationUnit compilationUnit) {
        return compilationUnit.getPackageDeclaration().orElseThrow().getNameAsString();
    }

}
