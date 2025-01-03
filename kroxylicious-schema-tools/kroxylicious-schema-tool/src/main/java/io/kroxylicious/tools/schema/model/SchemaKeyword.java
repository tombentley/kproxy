/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.tools.schema.model;

public class SchemaKeyword {
    public static final String ITEMS = "items";
    public static final String PROPERTIES = "properties";
    public static final String ONE_OF = "oneOf";
    public static final String ALL_OF = "allOf";
    public static final String ANY_OF = "anyOf";
    public static final String NOT = "not";
    public static final String LIST_MAP_KEYS = "x-kubernetes-list-map-keys";
    public static final String LIST_TYPE = "x-kubernetes-list-type";
    public static final String REF = "$ref";
    public static final String DEFINITIONS = "definitions";
    public static final String ID = "id";
    public static final String SCHEMA = "$schema";

    private SchemaKeyword() {
    }
}
