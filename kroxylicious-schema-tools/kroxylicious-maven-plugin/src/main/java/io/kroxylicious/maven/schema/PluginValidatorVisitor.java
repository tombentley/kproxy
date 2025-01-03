/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.maven.schema;

import java.util.List;

import io.kroxylicious.tools.schema.model.SchemaObject;
import io.kroxylicious.tools.schema.model.SchemaType;
import io.kroxylicious.tools.schema.model.SchemaValue;
import io.kroxylicious.tools.schema.model.SchemaVisitor;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Visitor which {@linkplain SchemaVisitor.Context#reportWarning(String, Object...) warns} about invalid input schemas.
 * The rules
 */
public class PluginValidatorVisitor extends SchemaVisitor {

    PluginValidatorVisitor() {

    }

    /**
     * @param context The context for a schema
     * @return the containing junctor keyword if the schema for this context is within a junctor, else returns null
     */
    @Nullable
    String withinJunctor(Context context) {
        return switch (context.keyword()) {
            case "allOf", "anyOf", "oneOf", "none" -> context.keyword();
            default -> null;
        };
    }

    @Override
    public VisitAction enterSchema(
                                   Context context,
                                   @NonNull SchemaObject schema) {
        if (schema.getItems() != null && schema.getItems().size() > 1) {
            context.reportError("`items` at path '{}' must not be an array", context.fullPath());
        }
        if (List.of(SchemaType.ARRAY).equals(schema.getType())
                && schema.getItems() == null) {
            context.reportError("`items` at path '{}' must be present if `type` is `array`", context.fullPath());
        }
        if (schema.getUniqueItems() != null && schema.getUniqueItems()) {
            context.reportError("`uniqueItems` at path '{}' must not be `true`", context.fullPath());
        }
        if (schema.getPatternProperties() != null && !schema.getPatternProperties().isEmpty()) {
            context.reportError("`patternProperties` at path '{}' must not be used", context.fullPath());
        }

        SchemaValue additionalProperties = schema.getAdditionalProperties();
        if (additionalProperties != null
                && Boolean.FALSE.equals(additionalProperties.getBooleanValue())) {
            context.reportError("`additionalProperties` at path '{}' must not be false", context.fullPath());
        }

        if (additionalProperties != null
                && (additionalProperties.getBooleanValue() != null
                        || additionalProperties.getSchemaObject() != null)
                && schema.getProperties() != null
                && !schema.getProperties().isEmpty()) {
            context.reportError("`additionalProperties` at path '{}' is mutually exclusive with `properties`", context.fullPath());
        }
        String containingJunctor = withinJunctor(context);
        if (containingJunctor != null) {

            checkWithinJunctor(context, schema, containingJunctor, additionalProperties);
        }
        else {
            // not within junctor
            if (schema.getType() == null || schema.getType().size() > 1) {
                context.reportError("`type` at path '{}' must be a `string`", context.fullPath());
            }
        }

        if (schema.getUnknownProperties().containsKey("dependencies")) {
            context.reportError("`dependencies` at path '{}' must not be used", context.fullPath());
        }

        // TODO additionalProperties cannot be false, mutually exclusive with properties, not in a junctor
        // TODO dependencies is not allowed

        // TODO type not in a junctor
        // TODO type not required if x-kube-int-or-string
        // TODO allOf
        // TODO anyOf
        // TODO oneOf
        // TODO not
        // TODO default
        // TODO format
        // TODO nullable
        return VisitAction.CONTINUE;
    }

    private static void checkWithinJunctor(Context context,
                                           SchemaObject schema,
                                           String containingJunctor,
                                           SchemaValue additionalProperties) {
        if (schema.getType() != null) {
            context.reportError("`type` at path '{}' must not be used within {}", context.fullPath(), containingJunctor);
        }

        if (schema.getProperties() != null) {
            context.reportError("`properties` at path '{}' within junctor {} were not declared outside that junctor", context.fullPath(), containingJunctor);
        }

        if (additionalProperties != null
                && (additionalProperties.getBooleanValue() != null
                        || additionalProperties.getSchemaObject() != null)) {
            context.reportError("`additionalProperties` at path '{}' must not be used within {}", context.fullPath(), containingJunctor);
        }

        if (schema.getDescription() != null) {
            context.reportError("`description` at path '{}' must not be used within {}", context.fullPath(), containingJunctor);
        }

        if (schema.getDefault() != null) {
            context.reportError("`default` at path '{}' must not be used within {}", context.fullPath(), containingJunctor);
        }

        // format

        // TODO disallow nullable

        // TODO disallow discriminator, readonly, writeonly, xml, deprecated
    }
}
