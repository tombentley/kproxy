/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.avro;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.ServiceLoaderPluginLookup;
import io.kroxylicious.filter.record.manipulation.format.jackson2.Use;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A demo of building an Avro mask/generator {@link AvroFunction} from a {@link Schema} tree, reusing the
 * Avro schema's own JSON syntax plus the non-standard {@code apply} keyword - the Avro equivalent of
 * {@link Use}.
 * <p>
 * Scoped to what {@link AvroFunction} currently supports: {@code record}/{@code array}/{@code string}/
 * {@code int}. In particular this drops the union/nullable fields the original sketch of this class
 * explored ({@code address}/{@code favorite_color} typed as {@code [..., "null"]}) down to plain required
 * types - see the module README's "Current state" section for why unions are a separate piece of work.
 */
@SuppressFBWarnings(value = "HARD_CODE_KEY", justification = "AvroUse is a main()-based demo, not production wiring - this module has no Filter "
        + "integration yet (see module README), so there's no real key-management path to source key material from. The literal here is an "
        + "illustrative placeholder standing in for a Context built from a real key at call sites that do exist.")
public class AvroUse {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvroUse.class);
    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };
    private static final PluginLookup LOOKUP = new ServiceLoaderPluginLookup();

    private AvroUse() {
    }

    /**
     * Runs the demo.
     * @param args unused
     */
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "See Use.main(), which carries the same justification for the same reason.")
    public static void main(String[] args) {
        // Basically, let's just reuse the Avro schema schema, but add our own keyword (apply): a field's
        // apply chain sits as a sibling of its own "type" (e.g. "firstName" below), while an apply chain
        // for an array's elements sits directly on the "items" schema (e.g. "aliases" below), since
        // Schema and Schema.Field both already preserve arbitrary extra JSON properties - see AvroSchemas.
        var maskSchemaJson = """
                {"type": "record", "name": "User", "namespace": "example.avro",
                    "fields": [
                        {"name": "firstName", "type": "string", "apply": [
                            {"op": "RandomString", "alphabet": "abcdefghijklmnopqrstuvwxyz", "minLengthInclusive": 3, "maxLengthExclusive": 5}
                        ]},
                        {"name": "surname", "type": "string", "apply": [
                            {"op": "ChooseString", "from": ["Smith", "Jones"]}
                        ]},
                        {"name": "ageYears", "type": "int", "apply": [
                            {"op": "RandomInt", "minInclusive": 18, "maxExclusive": 100}
                        ]},
                        {"name": "aliases", "type": {"type": "array", "items": {"type": "string", "apply": [
                            {"op": "RandomString", "alphabet": "abcdefghijklmnopqrstuvwxyz", "minLengthInclusive": 3, "maxLengthExclusive": 15}
                        ]}}},
                        {"name": "address", "type": {"type": "record", "name": "Address", "fields": [
                            {"name": "streetAddress", "type": "string", "apply": [
                                {"op": "HmacString", "keyId": "FOO"}
                            ]},
                            {"name": "city", "type": "string", "apply": [
                                {"op": "EncryptString", "keyId": "FOO"}
                            ]}
                        ]}}
                    ]
                }
                """;

        Schema maskSchema = new Schema.Parser().parse(maskSchemaJson);
        Schema unmaskSchema = new Schema.Parser().parse(maskSchemaJson.replace("EncryptString", "DecryptString"));

        GenericRecord address = new GenericData.Record(maskSchema.getField("address").schema());
        address.put("streetAddress", "Hogwarts");
        address.put("city", "Hogsmead");
        GenericRecord user = new GenericData.Record(maskSchema);
        user.put("firstName", "Harry");
        user.put("surname", "Potter");
        user.put("ageYears", 17);
        user.put("aliases", List.of("Vernon Dudley", "Barny Weasley"));
        user.put("address", address);

        Function<ByteBuffer, Object> deserializer = new AvroBinaryDeserializer(maskSchema);
        Function<Object, ByteBuffer> serializer = new AvroBinarySerializer(maskSchema);
        ByteBuffer data = serializer.apply(user);

        // OpContext maskOpContext = new OpContext(new Random(), KEY);
        // Pipeline maskPipeline = new Pipeline(List.of(deserializer, AvroFunction.buildMask(maskSchema, LOOKUP).bindRecord(maskOpContext), serializer));
        // ByteBuffer masked = maskPipeline.apply(data.duplicate());
        // LOGGER.atInfo().addKeyValue("masked", deserializer.apply(masked.duplicate())).log("applied mask");
        //
        // OpContext unmaskOpContext = new OpContext(new Random(), KEY);
        // Pipeline unmaskPipeline = new Pipeline(List.of(deserializer, AvroFunction.buildMask(unmaskSchema, LOOKUP).bindRecord(unmaskOpContext), serializer));
        // ByteBuffer unmasked = unmaskPipeline.apply(masked.duplicate());
        // LOGGER.atInfo().addKeyValue("unmasked", deserializer.apply(unmasked.duplicate())).log("applied unmask");
        //
        // // The same mask, applied via Avro's JSON encoding rather than its binary encoding.
        // Function<ByteBuffer, GenericRecord> jsonDeserializer = new AvroJsonDeserializer(maskSchema);
        // Function<GenericRecord, ByteBuffer> jsonSerializer = new AvroJsonSerializer(maskSchema);
        // Pipeline jsonMaskPipeline = new Pipeline(
        // List.of(jsonDeserializer, AvroFunction.buildMask(maskSchema, LOOKUP).bindRecord(new OpContext(new Random(), KEY)), jsonSerializer));
        // ByteBuffer jsonMasked = jsonMaskPipeline.apply(jsonSerializer.apply(user));
        // LOGGER.atInfo().addKeyValue("jsonMasked", jsonDeserializer.apply(jsonMasked.duplicate())).log("applied mask via Avro JSON encoding");
    }
}
