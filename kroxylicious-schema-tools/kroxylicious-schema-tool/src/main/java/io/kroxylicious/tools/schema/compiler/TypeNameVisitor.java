/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import io.kroxylicious.tools.schema.model.SchemaObject;
import io.kroxylicious.tools.schema.model.SchemaVisitor;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A {@link SchemaVisitor} that assigns a (hopefully, but not necessarily)
 * unique {@link SchemaObject#getJavaType()} name to each (sub-)schema which has
 * been loaded without any explicit {@link SchemaObject#getJavaType()}.
 *
 * We don't aim for uniqueness on the basis that the user can always override by setting $javaType, and we'd prefer to
 * generate "nice" names than "ugly" names which are guaranteed to be unique.
 */
public class TypeNameVisitor extends SchemaVisitor {

    private final Diagnostics diagnostics;
    private final String rootClass;

    public TypeNameVisitor(Diagnostics diagnostics,
                           String rootClass) {
        this.diagnostics = diagnostics;
        this.rootClass = rootClass;
    }

    @Override
    public void enterSchema(
                           SchemaVisitor.Context context,
                           @NonNull SchemaObject schema) {
        if (CodeGen.isTypeGenerated(schema)) {
            TypeModel model = null;
            if (schema.getJavaType() != null) {
                model = new TypeModel(context.fullPath(), schema.getJavaType(), List.of());
            }
            else if (context.isRootSchema()) {
                model = new TypeModel(context.fullPath(), rootClass, List.of());
            }
            else {
                final String name = generateTypeName(context);
                model = new TypeModel(context.fullPath(), name, List.of());
            }
            schema.setUnknownProperty("$$model", model);
        }
    }

    private String generateTypeName(SchemaVisitor.Context context) {
        var path = context.fullPath();
        final String name;
        if ("definitions".equals(context.keyword())) {
            name = generateTypeNameForDefinition(context);
        }
        else {
            var nameParts = new ArrayList<String>();
            var c= context;
            boolean singularize = false;
            while (c != null && c.parentSchema() != null) {
                String keyword = c.keyword();
                if ("properties".equals(keyword)) {
                    var d = initialCaps(c.fullPath().substring(c.fullPath().lastIndexOf("/") + 1));
                    if (singularize) {
                        d = singularize(d);
                    }
                    nameParts.add(d);
                    var s = c.parentSchema();
                    var ancestorModel = (TypeModel) s.getUnknownProperties().get("$$model");
                    if (ancestorModel != null) {
                        nameParts.add(ancestorModel.classname());
                        break;
                    }
                }
                else if ("items".equals(keyword)) {
                    singularize = true;
                }
                c = c.parent();
            }
            Collections.reverse(nameParts);
            String computedName = String.join("", nameParts);
            assert (!computedName.isEmpty());
            if (!computedName.equals(rootClass)) {
                name = computedName;
            }
            else {
                diagnostics.reportError("Could not compute a java class name for the schema at " + path);
                name = "*ERROR*";
            }
        }
        return name;
    }

    private String generateTypeNameForDefinition(SchemaVisitor.Context context) {
        var path = context.fullPath();
        final String name;
        var definitionsMatcher = DEFINITIONS_PATTERN.matcher(path);
        if (definitionsMatcher.matches()) {
            name = Objects.requireNonNull(definitionsMatcher.group("defName"));
        }
        else {
            diagnostics.reportError("Could not compute a java class name for the schema at " + path);
            name = "*ERROR*";
        }
        return name;
    }

    @NonNull
    private static String initialCaps(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase(Locale.ROOT) + propertyName.substring(1);
    }

    @NonNull
    private static String singularize(String propertyName) {
        if (propertyName.endsWith("ies")) {
            propertyName = propertyName.substring(0, propertyName.length() - 3);
        }
        else if (propertyName.endsWith("es")) {
            propertyName = propertyName.substring(0, propertyName.length() - 2);
        }
        else if (propertyName.endsWith("s")) {
            propertyName = propertyName.substring(0, propertyName.length() - 1);
        }
        return propertyName;
    }

    @SuppressWarnings("java:S5860") // sonar fails to detect use of the group name
    private static final Pattern PROPS_PATH = Pattern.compile("/(?<keyword>properties|items)/(?<nameOrIndex>[a-zA-Z0-9_-]+)");
    @SuppressWarnings("java:S5860") // sonar fails to detect use of the group name
    private static final Pattern DEFINITIONS_PATTERN = Pattern.compile(".*/definitions/(?<defName>[a-zA-Z0-9_-]+)");
}
