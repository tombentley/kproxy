/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogTest {

    @Test
    void shouldReadWhatWeWrote() throws IOException {
        // Given
        var mapper = new JsonMapper();
        var classpath = Files.createTempDirectory(CatalogTest.class.getName());
        var catalog = new Catalog(mapper, List.of(classpath));
        URI exampleCom = URI.create("https://example.com/schema");
        URI exampleComRoot = exampleCom.resolve("#");
        URI exampleComFoo = exampleCom.resolve("#/definitions/Foo");
        URI exampleComBar = exampleCom.resolve("#/definitions/Bar");
        URI exampleOrg = URI.create("https://example.org/schema");
        URI exampleOrgRoot = exampleOrg.resolve("#");
        URI exampleOrgFoo = exampleOrg.resolve("#/definitions/Foo");
        URI exampleOrgBar = exampleOrg.resolve("#/definitions/Bar");

        // When
        catalog.writeTypeDecls(exampleCom,
                List.of(new TypeModel("", "com.example.pkg.Root", List.of()),
                        new TypeModel("/definitions/Foo", "com.example.pkg.Foo", List.of()),
                        new TypeModel("/definitions/Bar", "com.example.pkg.Bar", List.of())),
                classpath);
        catalog.writeTypeDecls(exampleOrg,
                List.of(new TypeModel("", "org.example.whatever.Root", List.of()),
                        new TypeModel("/definitions/Foo", "org.example.whatever.Foo", List.of()),
                        new TypeModel("/definitions/Bar", "org.example.whatever.Bar", List.of())),
                classpath);

        // Then
        var readCatalog = new Catalog(mapper, List.of(classpath));
        assertThat(readCatalog.lookup(exampleComRoot).classname())
                .isEqualTo("com.example.pkg.Root");
        assertThat(readCatalog.lookup(exampleComFoo).classname())
                .isEqualTo("com.example.pkg.Foo");
        assertThat(readCatalog.lookup(exampleComBar).classname())
                .isEqualTo("com.example.pkg.Bar");

        assertThat(readCatalog.lookup(exampleOrgRoot).classname())
                .isEqualTo("org.example.whatever.Root");
        assertThat(readCatalog.lookup(exampleOrgFoo).classname())
                .isEqualTo("org.example.whatever.Foo");
        assertThat(readCatalog.lookup(exampleOrgBar).classname())
                .isEqualTo("org.example.whatever.Bar");
    }

    @Test void shouldRejectRelativeUri() throws IOException {
        // Given
        var mapper = new JsonMapper();
        var classpath = Files.createTempDirectory(CatalogTest.class.getName());
        var catalog = new Catalog(mapper, List.of(classpath));
        URI exampleCom = URI.create("schema");

        // When
        assertThatThrownBy(() ->
                catalog.writeTypeDecls(exampleCom,
                        List.of(),
                        classpath))
                .isInstanceOf(IllegalArgumentException.class);
    }

}