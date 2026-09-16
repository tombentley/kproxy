/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import edu.umd.cs.findbugs.annotations.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.NumericFPNode;
import tools.jackson.databind.node.NumericIntNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

public class JsonSchemaTypeSystem implements TypeSystem<JsonNode> {

    private final JsonNodeFactory jsonNodeFactory;

    JsonSchemaTypeSystem(JsonNodeFactory jsonNodeFactory) {
        this.jsonNodeFactory = jsonNodeFactory;
    }

    @Override
    public Type topType() {
        return null;
    }

//    @Override
//    public Type unionType(List<Type> caseTypes) {
//        return Union.of(caseTypes);
//    }

//    @Override
//    public Set<Type> caseTypes(JsonNode unionSchema) {
//        if (unionSchema.get("type") instanceof ArrayNode types) {
//            return types.valueStream().map(this::typeOf).collect(Collectors.toSet());
//        }
//        else {
//            return Collections.emptySet();
//        }
//    }

    @Override
    public boolean isUnionType(JsonNode schema) {
        return schema.get("type") instanceof ArrayNode;
    }

    @Override
    public boolean isObjectType(JsonNode schema) {
        var type = schema.get("type");
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
    public boolean isObjectOpen(JsonNode objectSchema) {
        JsonNode additionalProperties = objectSchema.get("additionalProperties");
        if (additionalProperties == null) {
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
    public List<String> objectProperties(JsonNode objectSchema) {
        JsonNode properties = objectSchema.get("properties");
        if (properties == null) {
            return List.of();
        }
        else if (properties.isObject()) {
            return List.copyOf(properties.propertyNames());
        }
        return List.of();
    }

    @Override
    public JsonNode objectPropertySchema(JsonNode objectSchema, String propertyName) {
        JsonNode properties = objectSchema.get("properties");
        if (properties.isObject() && properties.has(propertyName)) {
            return properties.get(propertyName);
        }
        // TODO patternProperties
        // TODO note that the contract for this method is not really compatible with patternProperties
        //   because this method can test the given propertyName against the patterns and get schema S1
        //   but when objectPropertySchema(JsonNode objectType) is called directly it would have to take
        //   the union of all the patternProperty schemas, which is actually wider/more general that this
        //   objectPropertySchema(JsonNode sch, String propertyName) case
        return objectPropertySchema(objectSchema);
    }

    @Override
    public JsonNode objectPropertySchema(JsonNode objectSchema) {
        JsonNode additionalProperties = objectSchema.get("additionalProperties");
        if (additionalProperties != null && additionalProperties.isObject()) {
            return additionalProperties;
        }
        if (isObjectOpen(objectSchema)) {
            ObjectNode jsonNodes = new ObjectNode(jsonNodeFactory);
            jsonNodes.putArray("type")
                    .add("null")
                    .add("boolean")
                    .add("number")
                    .add("integer")
                    .add("string")
                    .add("array")
                    .add("object");
            return jsonNodes;
        }
        else {
            return new ObjectNode(null);
        }
    }

    @Override
    public boolean isArrayType(JsonNode schema) {
        var type = schema.get("type");
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
    public boolean isArrayOpen(JsonNode arraySchema) {
        return false;
    }

    @Override
    public int[] arrayIndexes(JsonNode arraySchema) {
        JsonNode itemsSchema = arraySchema.get("items");
        if (itemsSchema == null) {
            return null;
        }
        else if (itemsSchema.isObject()) {
            return new int[0];
        }
        else if (itemsSchema.isArray()) {
            return IntStream.range(0, itemsSchema.size()).toArray();
        }
        // TODO additionalItems
        return null;
    }

    @Override
    public JsonNode arrayItemSchema(JsonNode arraySchema, int index) {
        JsonNode prefixItemsSchema = arraySchema.get("prefixItems");
        if (prefixItemsSchema != null && index < prefixItemsSchema.size()) {
            return prefixItemsSchema.get(index);
        }
        JsonNode itemsSchema = arraySchema.get("items");
        return itemsSchema;
    }

    @Override
    public JsonNode arrayItemSchema(JsonNode arraySchema) {
        return null;
    }

    @Override
    public Type bottomType() {
        return MissingNode.class;
    }

    @Override
    public Type typeOf(JsonNode schema) {
        var typeNode = schema.get("type");
        if (typeNode instanceof ArrayNode typeArrayNode) {
            List<Type> types = new ArrayList<>(typeArrayNode.size());
            Set<String> seenTypes = new HashSet<>();
            for (JsonNode n : typeArrayNode) {
                if (n instanceof StringNode stringNode) {
                    String type = stringNode.asString();
                    seenTypes.add(type);
                    types.add(jsonNodeType(type));
                }
            }
            if (seenTypes.equals(Set.of("null", "boolean", "number", "integer", "string", "array", "object"))) {
                return JsonNode.class;
            }
            return Union.of(types);
        } else if (typeNode instanceof StringNode typeStringNode) {
            return jsonNodeType(typeStringNode.asString());
        }
        return MissingNode.class;
    }

    @NonNull
    private static Type jsonNodeType(String type) {
        return switch (type) {
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

    @NonNull
    private static Type mapListType(String type) {
        return switch (type) {
            case "null" -> Void.class;
            case "boolean" -> Boolean.class;
            case "integer" -> Number.class;
            case "number" -> Number.class;
            case "string" -> String.class;
            case "array" -> List.class;
            case "object" -> Map.class;
            default -> Void.class; // ??? WTF
        };
    }
}
