/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.net.URI;

import io.kroxylicious.tools.schema.model.SchemaObject;
import io.kroxylicious.tools.schema.model.SchemaVisitor;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A {@link SchemaVisitor} that resolves {@code $ref}s to external schemas.
 * External schemas are those which are not present any of the files found in the SchemaCompiler.srcPaths.
 * External schemas are {@code $ref}ed to using an absolute URI, but this is not treated
 * as a URL (nothing is loaded over the network).
 * Instead, these schemas are located via the classpath.
 *
 * Postcondition: After this phase is complete all external $ref nodes will have a type model
 */
public class ResolveVisitor extends SchemaVisitor {

    private final Diagnostics diagnostics;
    private final IdVisitor idVisitor;
    private final Catalog catalog;

    public ResolveVisitor(Diagnostics diagnostics,
                          IdVisitor idVisitor,
                          Catalog catalog) {
        this.diagnostics = diagnostics;
        this.idVisitor = idVisitor;
        this.catalog = catalog;
    }

    @Override
    public void enterSchema(
                            Context context,
                            @NonNull SchemaObject schema) {
        String ref = schema.getRef();
        if (ref != null) {
            // TODO validate that the other fields are not set

            // resolve
            URI resolvedRef = context.base().resolve(ref);
            // Try looking it up internally
            SchemaObject resolvedSchemaObject = idVisitor.resolve(resolvedRef);
            if (resolvedSchemaObject == null) {
                // TODO cope with not-yet-loaded refs

                var typeModel= catalog.lookup(resolvedRef);
                if (typeModel == null) {
                    diagnostics.reportError("{}: Unable to resolve $ref: {}", context.base(), ref);
                    schema.setUnknownProperty("$$model", TypeModel.UNKNOWN);
                }
                else {
                    // set the type model on the $ref schema object
                    schema.setUnknownProperty("$$model", typeModel);
                    diagnostics.debug("Setting model {} on {}", typeModel, context.fullPath());
                }
            }
            // TODO check for infinite recursion, both direct and indirect
        }
    }

}
