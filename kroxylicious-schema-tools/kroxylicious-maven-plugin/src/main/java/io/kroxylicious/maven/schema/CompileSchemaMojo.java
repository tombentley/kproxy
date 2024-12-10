/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.maven.schema;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import io.kroxylicious.tools.schema.compiler.Diagnostics;
import io.kroxylicious.tools.schema.compiler.RecordPropertyStrategy;
import io.kroxylicious.tools.schema.compiler.SchemaCompiler;
import io.kroxylicious.tools.schema.model.SchemaObject;

@Mojo(name = "compile-schema", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CompileSchemaMojo extends AbstractCompileSchemaMojo {

    @Override
    protected SchemaCompiler schemaCompiler() throws MojoExecutionException {
        SchemaCompiler schemaCompiler = new SchemaCompiler(List.of(source.toPath()),
                null,
                readHeaderFile(),
                existingClasses != null ? existingClasses : Map.of(),
                List.of(),
                new RecordPropertyStrategy(),
                false,
                List.of());
        return schemaCompiler;
    }

    @Override
    protected void validate(Diagnostics diagnostics, URI uri, SchemaObject schemaObject) {
        // TODO Wright-00 validation
    }
}
