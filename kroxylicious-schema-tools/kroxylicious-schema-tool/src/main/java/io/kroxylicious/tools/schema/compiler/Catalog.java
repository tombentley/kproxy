/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Management of files recording which Java classes correspond to which schemas.
 * These correspondences are written during code generation to files under {@code META-INF}, one for each root schema.
 * When generating code for schemas which {@code $ref} to previously compiled schemas the files are read so that
 * we know the fully qualified class names of referents.
 *
 * In other words, .jar files include catalogs of previously compile schemas which are used in subsequent compiles, allowing the schema
 * compiler to be incremental
 */
public class Catalog {

    private final Map<URI, TypeModel> cache = new HashMap<>();
    private final JsonMapper mapper;
    private final List<Path> classpath;

    public Catalog(
            ObjectMapper mapper,
            List<Path> classpath
    ) {
        // We want to guarantee that we write JSON (not YAML)
        this.mapper = new JsonMapper(mapper.getFactory());
        this.classpath = classpath;
    }

    public void writeTypeDecls(URI schemaUri, List<TypeModel> typeModels, Path dir) throws IOException {
        if (schemaUri.getFragment() != null) {
            throw new IllegalArgumentException("URI fragment is not supported");
        }
        if (!schemaUri.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        }
        // TODO validation: bijective

        var output = dir.resolve(entryName(schemaUri));
        Files.createDirectories(output.getParent());
        mapper.writeValue(output.toFile(), new CompilerInfo(1, typeModels));
    }

    public @Nullable TypeModel lookup(URI schemaUri) {

        var hit = cache.get(schemaUri);
        if (hit != null) {
            return hit;
        }

        URI withoutFragment = withoutFragment(schemaUri);

        String entryName = entryName(withoutFragment);
        // classpath items may be jars, or directories
        for (Path p : classpath) {
            if (Files.exists(p)) {
                TypeModel typeModel = null;
                if (Files.isDirectory(p)) {
                    typeModel = findInFile(mapper, schemaUri, p, entryName, withoutFragment);
                }
                else { // assume it's readable
                    typeModel = findInJar(mapper, schemaUri, p, entryName, withoutFragment);
                }
                if (typeModel != null) {
                    return typeModel;
                }
            }
        }
        return null;
    }

    @Nullable
    private TypeModel findInFile(JsonMapper mapper, URI uri, Path p, String name, URI withoutFragment) {
        TypeModel typeModel = null;
        Path file = p.resolve(name);
        if (Files.isReadable(file)) {
            try (var is = Files.newInputStream(file)) {
                typeModel = getAndMaybeCacheMappings(mapper, uri, withoutFragment, is);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return typeModel;
    }

    @Nullable
    private TypeModel findInJar(JsonMapper mapper, URI uri, Path p, String name, URI withoutFragment) {
        try (JarFile jarFile = new JarFile(p.toFile())) {
            var entry = jarFile.getJarEntry(name);
            try (var is = jarFile.getInputStream(entry)) {
                var typeDecl = getAndMaybeCacheMappings(mapper, uri, withoutFragment, is);
                if (typeDecl != null) {
                    return typeDecl;
                }
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    @Nullable
    private TypeModel getAndMaybeCacheMappings(JsonMapper mapper, URI uri, URI withoutFragment, InputStream is) throws IOException {
        var decls = mapper.readValue(is, CompilerInfo.class);
        if (decls.version() != 1) {
            throw new IllegalArgumentException("Unsupported version " + decls.version());
        }
        // cache the mappings
        decls.typeModels().forEach(typeModel -> {
            var u = uri.resolve("#" + typeModel.pointer());
            cache.put(u, typeModel);
        });
        return decls.typeModels().stream()
                .filter(td -> td.pointer().equals(uri.getFragment()))
                .findFirst()
                .orElse(null);
    }

    @NonNull
    private static String entryName(URI withoutFragment) {
        var encoded = withoutFragment.toASCIIString();
        return "META-INF/schema/" + URLEncoder.encode(encoded, StandardCharsets.US_ASCII) + ".json";
    }

    @NonNull
    private static URI withoutFragment(URI uri) {
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery());
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * A structure written to a file kept in a {@linkplain #entryName(URI)} specific file} under META-INF
     * @param version The version of the format that was written
     * @param typeModels The decls
     */
    record CompilerInfo(
            int version,
            List<TypeModel> typeModels
    ) {
    }
}
