/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.api.mapper;

import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;

/**
 * <p>A general mapping/transformation of Kafka Records.
 * Such a transformation can make arbitrary changes to a record's key, value and/or headers.</p>
 *
 * <p>See also the specialised subinterfaces {@link RecordHeaderMapping},
 * {@link RecordKeyMapping} and
 * {@link RecordValueMapping}.</p>
 *
 * @param <K1> The type of the input record key
 * @param <K1S> The type of the input record key schema
 * @param <K1I> The type of the input record key schema identifier
 * @param <V1> The type of the input record value
 * @param <V1S> The type of the input record value schema
 * @param <V1I> The type of the input record value schema identifier
 * @param <K2> The type of the output record key
 * @param <K2S> The type of the output record key schema
 * @param <K2I> The type of the output record key schema identifier
 * @param <V2> The type of the output record value
 * @param <V2S> The type of the output record value schema
 * @param <V2I> The type of the output record value identifier
 */
@SuppressWarnings("java:S6213")
public interface RecordMapping<
        K1, K1S, K1I extends WireSchemaId,
        V1, V1S, V1I extends WireSchemaId,
        K2, K2S, K2I extends WireSchemaId,
        V2, V2S, V2I extends WireSchemaId> {
    MappingRecord<K2, K2S, K2I, V2, V2S, V2I> transform(MappingRecord<K1, K1S, K1I, V1, V1S, V1I> record, Context context);

//    static void pipeline(List<RecordMapping> mappings) {
//        Type keyType, valueType;
//        for (int i = 0; i < mappings.size(); i++) {
//            var mapping = mappings.get(i);
//            if (i == 0) {
//                keyType = mapping.acceptsKeyType();
//                valueType = mapping.acceptsValueType();
//                // TODO Lookup a deserializer for the key type and value type
//                keyType = mapping.returnsKeyType();
//                valueType = mapping.returnsValueType();
//            }
//            else {
//                var keyType1 = mapping.acceptsKeyType();
//                var valueType1 = mapping.acceptsValueType();
//                if (!keyType.isAssignableTo(keyType1)) {
//                    // TODO not java compatible. But we could serialize and deserialize
//                    //  if we're able to figure out a common byte format
//                }
//                if (!valueType.isAssignableTo(valueType1)) {
//
//                }
//                keyType = mapping.returnsKeyType();
//                valueType = mapping.returnsValueType();
//            }
//
//
//        }
//    }
}

/*
// KeyOnlyRecordMapping
// ValueOnlyRecordMapping
// HeaderOnlyRecordMapping
// KeyOnlyInPlaceRecordMapping
// ValueOnlyInRecordMapping

class AvroValueRemove<K> implements RecordMapping<K, Object, K, Object> {

    static <K> AvroValueRemove<K> fromRecord(MappingRecord<K, Schema, WireSchemaId> record, String replacementAsJson) {
        // The late binding case
        return fromSchema((Schema) record.valueSchema(), replacementAsJson);
    }

    static <K> AvroValueRemove<K> fromConfig(Schema replacementSchema, String replacementAsJson) {
        // The early binding case
        return fromSchema(replacementSchema, replacementAsJson);
    }

    static <K> AvroValueRemove<K> fromSchema(Schema replacementSchema, String replacementAsJson) {
        // The late binding case
        var reader = new GenericDatumReader<>(replacementSchema);
        DecoderFactory decoderFactory = DecoderFactory.get();
        Decoder decoder = decoderFactory.jsonDecoder(replacementSchema, replacementAsJson);

        Object read = reader.read(null, decoder);
        return new AvroValueRemove<K>(replacementSchema, read);
    }

    String fieldName;
    int index;
    private final Schema replacementSchema;
    private final Object replacement; // TODO where is value going to come from?
                  //   It's configuration data, so we can use the Avro JSON encoding
                  //   But we don't necessarily know the schema until transform time

    AvroValueRemove(Schema replacementSchema, Object replacement) {
        this.replacementSchema = replacementSchema;
        this.replacement = replacement;
    }

    @Override
    public MappingRecord<K, Object> transform(MappingRecord<K, Object> record, Context context) {
        Schema schema = (Schema) record.valueSchema();
        switch (schema.getType()) {
            case RECORD -> {
                ((GenericRecord) record).put(fieldName, replacement);
            }
            case ARRAY -> {
                ((List) record).add(index, replacement);
            }
            case MAP -> {
                ((Map) record).put(fieldName, replacement);
            }
            default ->  {
                record.withValue(replacement, null, replacementSchema);
                return record;
            }
        }
        return record;
    }
}


class JsonReplace<K, V> implements RecordMapping<K, V, K, V> {



    // TODO Remove
    //   Add
    //   Move
    //   Copy
    //   Test
    static <V> RecordMapping<JsonNode, V, JsonNode, V> key(String jsonPointer, JsonNode replacement) {
        return new JsonReplace<>(MappingRecord::key, jsonPointer, replacement);
    }

    static <K> RecordMapping<K, JsonNode, K, JsonNode> value(String jsonPointer, JsonNode replacement) {
        return new JsonReplace<>(MappingRecord::value, jsonPointer, replacement);
    }

    private final Function<MappingRecord<K, V>, JsonNode> transformer;
    private final JsonPointer head;
    private final JsonPointer last;
    private final JsonNode replacement;

    JsonReplace(Function<MappingRecord<K, V>, JsonNode> transformer, String path, JsonNode replacement) {
        this.transformer = transformer;
        JsonPointer compile = JsonPointer.compile(path);
        this.head = compile.head();
        this.last = compile.last();
        this.replacement = replacement;
    }

    @Override
    public MappingRecord<K, V> transform(MappingRecord<K, V> record, Context context) {
        JsonNode at = transformer.apply(record).at(head);
        if (at.isArray()) {
            int index = last.getMatchingIndex();
            ((ArrayNode) at).remove(index);
            ((ArrayNode) at).insert(index, replacement);
        }
        else if (at.isObject()) {
            ((ObjectNode) at).put(last.getMatchingProperty(), replacement);
        }
        else {
            record.withValue(replacement, null, null);
        }
        return record;
    }
}
*/