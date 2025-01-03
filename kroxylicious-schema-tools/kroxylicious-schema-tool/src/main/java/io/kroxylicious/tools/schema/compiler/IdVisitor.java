/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.kroxylicious.tools.schema.model.SchemaKeyword;
import io.kroxylicious.tools.schema.model.SchemaObject;
import io.kroxylicious.tools.schema.model.SchemaVisitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A {@link SchemaVisitor} which tracks the URIs (multiple!) which can be used
 * to refer to an input schema.
 *
 * The same IdVisitor instance is used for all the input schemas, which allows them to cross-reference each other.
 * This is the first phase of complication, and the index built is used by later phases when resolving {@code $ref}s.
 */
public class IdVisitor extends SchemaVisitor {

    private static final Pattern SUBSCHEMA_ID_PATTERN = Pattern.compile("^#[A-Za-z][A-Za-z0-9_:.-]*$");

    // The "id" keyword defines a URI for the schema, and the base URI that
    // other URI references within the schema are resolved against.
    // The "id" keyword itself is resolved against the base URI that the object
    // as a whole appears in.

    private final Map<URI, SchemaObject> idIndex = new TreeMap<>();
    private @Nullable URI rootId;

    public IdVisitor() {
    }

    public @Nullable SchemaObject resolve(URI uri) {
        return idIndex.get(uri);
    }

    // The value of the $ref is a URI
    // Reference. Resolved against the current URI base, it identifies the
    // URI of a schema to use.

    @Override
    public VisitAction enterSchema(
                                   Context context,
                                   @NonNull SchemaObject schema) {
        if (context.isRootSchema()) {
            indexRootSchema(context, schema);
        }
        else {
            // a subschema
            indexSubschema(context, schema);
        }
        return VisitAction.CONTINUE;
    }

    @Override
    public void exitSchema(
                           Context context,
                           @NonNull SchemaObject schema) {
        if (context.isRootSchema()) {
            this.rootId = null;
        }
    }

    private void indexRootSchema(Context context, SchemaObject rootSchema) {
        final URI base = context.base();
        index(context, base, rootSchema);
        index(context, resolve(base, "#"), rootSchema);
        // Wright 00:
        // The root schema of a JSON Schema document SHOULD contain an "id"
        // keyword with an absolute-URI (containing a scheme, but no fragment).
        var rootId = rootSchema.getId() != null ? URI.create(rootSchema.getId()) : null;
        if (rootId == null) {
            context.reportWarning(
                    "Root schema of a document should contain an '{}' with an absolute URI, but '{}' is absent: {}",
                    SchemaKeyword.ID,
                    SchemaKeyword.ID,
                    base);
        }
        else {
            if (rootId.getFragment() != null) {
                context.reportError(
                        "Root schema of a document loaded from {} should contain an '{}' with an absolute URI without a fragment, but '{}' has a fragment",
                        base,
                        SchemaKeyword.ID,
                        rootId);
            }
            else if (!rootId.isAbsolute()) {
                context.reportError(
                        "Root schema of a document loaded from {} should contain an '{}' with an absolute URI without a fragment, but '{}' is not absolute.",
                        base,
                        SchemaKeyword.ID,
                        rootId);
            }
            else if (!rootId.equals(base)) {
                index(context, rootId, rootSchema);
                index(context, rootId.resolve("#"), rootSchema);
                this.rootId = rootId;
            }
        }
    }

    private void indexSubschema(Context context, SchemaObject subSchema) {
        final URI base = context.base();
        // the subschema's explicit id
        String id = subSchema.getId();
        if (id != null) {
            // Wright 00:
            // To name subschemas in a JSON Schema document, subschemas can use "id"
            // to give themselves a document-local identifier. This form of "id"
            // keyword MUST begin with a hash ("#") to identify it as a fragment URI
            // reference, followed by a letter ([A-Za-z]), followed by any number of
            // letters, digits ([0-9]), hyphens ("-"), underscores ("_"), colons
            // (":"), or periods (".").
            if (!SUBSCHEMA_ID_PATTERN.matcher(id).matches()) {
                context.reportError("Invalid subschema '{}', must match {}: {}",
                        SchemaKeyword.ID,
                        SUBSCHEMA_ID_PATTERN.pattern(),
                        id);
            }
            else {
                index(context, resolve(base, id), subSchema);
                if (this.rootId != null) {
                    index(context, resolve(this.rootId, id), subSchema);
                }
            }
        }

        // the subschema's #pointer
        // Pointer id. This cannot collide with the 'id' property because it always begins with /
        // which id is not allowed to contain
        if (context.fullPath().indexOf('/') == -1) {
            // Should never happen
            throw new IllegalStateException();
        }
        index(context, resolve(base, "#" + context.fullPath()), subSchema);
        if (this.rootId != null) {
            index(context, resolve(this.rootId, "#" + context.fullPath()), subSchema);
        }
    }

    private void index(SchemaVisitor.Context context, URI base, @NonNull SchemaObject schema) {
        SchemaObject old = idIndex.put(base, schema);
        if (old != null) {
            context.reportError("Attempt to identify two schemas from same URI {}", base);
        }
    }

    private static URI resolve(URI base, String pathId) {
        return base.resolve(pathId);
    }

    @Override
    public String toString() {
        return "IdVisitor{" + System.lineSeparator() + "  " +
                idIndex.entrySet().stream().map(entry -> entry.getKey() + " --> " + entry.getValue())
                        .collect(Collectors.joining(System.lineSeparator() + "  ", "", System.lineSeparator()))
                +
                '}';
    }
}
