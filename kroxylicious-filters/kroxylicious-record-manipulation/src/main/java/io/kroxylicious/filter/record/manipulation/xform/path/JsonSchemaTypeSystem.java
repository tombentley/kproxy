/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;

import edu.umd.cs.findbugs.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.NumericFPNode;
import tools.jackson.databind.node.NumericIntNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

public class JsonSchemaTypeSystem implements TypeSystem<JsonSchemaTypeSystem.JsonSchema> {

    record JsonSchema (JsonNode schemaNode) {

    }

    @Override
    public Type topType() {
        return null;
    }

    @Override
    public Type unionType(List<Type> caseTypes) {
        return Union.of(caseTypes);
    }

    @Override
    public Set<Type> caseTypes(JsonSchema unionType) {
        if (unionType.schemaNode.get("type") instanceof ArrayNode types) {
            return types.valueStream().map(this::typeOf).collect(Collectors.toSet());
        }
        else {
            return Collections.emptySet();
        }
    }

    @Override
    public boolean isUnionType(JsonSchema unionType) {
        return unionType.schemaNode.get("type") instanceof ArrayNode;
    }

    @Override
    public boolean isObjectType(JsonSchema sch) {
        var type = sch.schemaNode.get("type");
        if (type instanceof ArrayNode a) {
            for (JsonNode n : a) {
                if (n instanceof StringNode s
                        && s.asString().equals("object")) {
                    return true;
                }
            }
        } else if (type instanceof StringNode s) {
            return s.asString().equals("object");
        }
        return false;
    }

    @Override
    public boolean isObjectOpen(JsonSchema sch) {
        JsonNode additionalProperties = sch.schemaNode.get("additionalProperties");
        if (additionalProperties.isMissingNode()) {
            return true;
        }
        else if (additionalProperties.isBoolean()) {
            return additionalProperties.asBoolean();
        }
        else {
            return true; // assume it's a satisfiable schema
        }
    }

    @Override
    public List<String> objectProperties(JsonSchema sch) {
        JsonNode properties = sch.schemaNode.get("properties");
        if (properties.isMissingNode()) {
            return List.of();
        }
        else if (properties.isObject()) {
            return List.copyOf(properties.propertyNames());
        }
        return List.of();
    }

    @Override
    public JsonSchema objectPropertySchema(JsonSchema sch, String propertyName) {
        JsonNode properties = sch.schemaNode.get("properties");
        if (properties.isObject()) {
            return new JsonSchema(properties.get(propertyName));
        }
        // TODO pattern properties
        return objectPropertySchema(sch);
    }

    @Override
    public JsonSchema objectPropertySchema(JsonSchema objectType) {
        JsonNode additionalProperties = objectType.schemaNode.get("additionalProperties");
        if (additionalProperties.isObject()) {
            return new JsonSchema(additionalProperties);
        }
        return new JsonSchema(new ObjectNode(null));
    }

    @Override
    public boolean isArrayType(JsonSchema sch) {
        var type = sch.schemaNode.get("type");
        if (type instanceof ArrayNode a) {
            for (JsonNode n : a) {
                if (n instanceof StringNode s
                        && s.asString().equals("array")) {
                    return true;
                }
            }
        } else if (type instanceof StringNode s) {
            return s.asString().equals("array");
        }
        return false;
    }

    @Override
    public boolean isArrayOpen(JsonSchema arrayType) {
        return false;
    }

    @Override
    public int[] arrayIndexes(JsonSchema arrayType) {
        return new int[0];
    }

    @Override
    public JsonSchema arrayItemSchema(JsonSchema arrayType, int index) {
        return null;
    }

    @Override
    public JsonSchema arrayItemSchema(JsonSchema arrayType) {
        return null;
    }

    @Override
    public Type bottomType() {
        return MissingNode.class;
    }

    @Override
    public Type typeOf(JsonSchema schema) {
        return typeOf(schema.schemaNode);
    }

    private Type typeOf(JsonNode schema) {
        var type = schema.get("type");
        if (type instanceof ArrayNode typesArray) {
            List<Type> types = new ArrayList<>(typesArray.size());
            for (JsonNode n : typesArray) {
                if (n instanceof StringNode typeNode) {
                    types.add(getType(typeNode));
                }
            }
            return Union.of(types);
        } else if (type instanceof StringNode typeNode) {
            return getType(typeNode);
        }
        return MissingNode.class;
    }

    @NonNull
    private static Type getType(StringNode s) {
        return switch (s.asString()) {
            case "null" -> NullNode.class;
            case "boolean" -> BooleanNode.class;
            case "integer" -> NumericIntNode.class;
            case "number" -> NumericFPNode.class;
            case "string" -> StringNode.class;
            case "array" -> ArrayNode.class;
            case "object" -> ObjectNode.class;
            default -> MissingNode.class;
        };
    }
}
