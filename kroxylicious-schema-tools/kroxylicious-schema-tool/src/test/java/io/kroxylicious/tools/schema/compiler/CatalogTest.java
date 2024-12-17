/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.json.JsonMapper;

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
                List.of(new TypeModel("", "com.example.pkg", "Root", List.of()),
                        new TypeModel("/definitions/Foo", "com.example.pkg", "Foo", List.of()),
                        new TypeModel("/definitions/Bar", "com.example.pkg", "Bar", List.of())),
                classpath);
        catalog.writeTypeDecls(exampleOrg,
                List.of(new TypeModel("", "org.example.whatever", "Root", List.of()),
                        new TypeModel("/definitions/Foo", "org.example.whatever", "Foo", List.of()),
                        new TypeModel("/definitions/Bar", "org.example.whatever", "Bar", List.of())),
                classpath);

        // Then
        var readCatalog = new Catalog(mapper, List.of(classpath));
        assertThat(readCatalog.lookup(exampleComRoot).classname())
                .isEqualTo("Root");
        assertThat(readCatalog.lookup(exampleComRoot).pkg())
                .isEqualTo("com.example.pkg");
        assertThat(readCatalog.lookup(exampleComFoo).classname())
                .isEqualTo("Foo");
        assertThat(readCatalog.lookup(exampleComFoo).pkg())
                .isEqualTo("com.example.pkg");
        assertThat(readCatalog.lookup(exampleComBar).classname())
                .isEqualTo("Bar");
        assertThat(readCatalog.lookup(exampleComBar).pkg())
                .isEqualTo("com.example.pkg");

        assertThat(readCatalog.lookup(exampleOrgRoot).classname())
                .isEqualTo("Root");
        assertThat(readCatalog.lookup(exampleOrgRoot).pkg())
                .isEqualTo("org.example.whatever");
        assertThat(readCatalog.lookup(exampleOrgFoo).classname())
                .isEqualTo("Foo");
        assertThat(readCatalog.lookup(exampleOrgFoo).pkg())
                .isEqualTo("org.example.whatever");
        assertThat(readCatalog.lookup(exampleOrgBar).classname())
                .isEqualTo("Bar");
        assertThat(readCatalog.lookup(exampleOrgBar).pkg())
                .isEqualTo("org.example.whatever");
    }

    @Test
    void shouldRejectRelativeUri() throws IOException {
        // Given
        var mapper = new JsonMapper();
        var classpath = Files.createTempDirectory(CatalogTest.class.getName());
        var catalog = new Catalog(mapper, List.of(classpath));
        URI exampleCom = URI.create("schema");

        // When
        assertThatThrownBy(() -> catalog.writeTypeDecls(exampleCom,
                List.of(),
                classpath))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
