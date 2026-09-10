/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.format.protobuf;

import java.nio.ByteBuffer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.ServiceLoaderPluginLookup;
import io.kroxylicious.filter.record.manipulation.format.jackson2.Use;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A demo of building a Protobuf mask {@link ProtobufFunction} from raw {@code .proto} IDL text, reusing the
 * schema's own option syntax plus the non-standard {@code apply} option - the Protobuf equivalent of
 * {@link Use}.
 * <p>
 * Scoped to what {@link ProtobufFunction} currently supports: {@code message}/{@code repeated}/{@code string}/
 * {@code int32}. See the module README's "Current state" section for what's still open ({@code oneof},
 * {@code map}, other scalar types, ...).
 */
@SuppressFBWarnings(value = "HARD_CODE_KEY", justification = "ProtoUse is a main()-based demo, not production wiring - this module has no Filter "
        + "integration yet (see module README), so there's no real key-management path to source key material from. The literal here is an "
        + "illustrative placeholder standing in for a Context built from a real key at call sites that do exist.")
public class ProtoUse {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtoUse.class);
    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };
    private static final PluginLookup LOOKUP = new ServiceLoaderPluginLookup();

    private ProtoUse() {
    }

    /**
     * Runs the demo.
     * @param args unused
     */
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "See Use.main(), which carries the same justification for the same reason.")
    public static void main(String[] args) {
        // A field's apply chain attaches via the custom "apply" option, a sibling of its own declaration
        // (e.g. "first_name" below) - the Protobuf equivalent of Avro's non-standard "apply" schema
        // keyword, but read off the schema's option syntax rather than round-tripped through the schema
        // object itself (see ProtoSchemaParser for why).
        var maskSchemaProto = """
                syntax = "proto3";
                message User {
                    string first_name = 1 [(apply) = { op: "RandomString", alphabet: "abcdefghijklmnopqrstuvwxyz", minLengthInclusive: 3, maxLengthExclusive: 5 }];
                    string surname = 2 [(apply) = { op: "ChooseString", from: ["Smith", "Jones"] }];
                    int32 age_years = 3 [(apply) = { op: "RandomInt", minInclusive: 18, maxExclusive: 100 }];
                    repeated string aliases = 4 [(apply) = { op: "RandomString", alphabet: "abcdefghijklmnopqrstuvwxyz", minLengthInclusive: 3, maxLengthExclusive: 15 }];
                    Address address = 5;

                    message Address {
                        string street_address = 1 [(apply) = { op: "HmacString", keyId: "FOO" }];
                        string city = 2 [(apply) = { op: "EncryptString", keyId: "FOO" }];
                    }
                }
                """;

        ParsedProtoSchema maskSchema = ProtobufSchemaParser.parse(maskSchemaProto, "User");
        ParsedProtoSchema unmaskSchema = ProtobufSchemaParser.parse(maskSchemaProto.replace("EncryptString", "DecryptString"), "User");

        DynamicMessage address = DynamicMessage.newBuilder(maskSchema.descriptor().findNestedTypeByName("Address"))
                .setField(maskSchema.descriptor().findNestedTypeByName("Address").findFieldByName("street_address"), "Hogwarts")
                .setField(maskSchema.descriptor().findNestedTypeByName("Address").findFieldByName("city"), "Hogsmead")
                .build();
        DynamicMessage user = DynamicMessage.newBuilder(maskSchema.descriptor())
                .setField(maskSchema.descriptor().findFieldByName("first_name"), "Harry")
                .setField(maskSchema.descriptor().findFieldByName("surname"), "Potter")
                .setField(maskSchema.descriptor().findFieldByName("age_years"), 17)
                .addRepeatedField(maskSchema.descriptor().findFieldByName("aliases"), "Vernon Dudley")
                .addRepeatedField(maskSchema.descriptor().findFieldByName("aliases"), "Barny Weasley")
                .setField(maskSchema.descriptor().findFieldByName("address"), address)
                .build();

        // Unlike Avro's GenericRecord.get(String), DynamicMessage.getField(FieldDescriptor) is checked
        // against the exact Descriptor build a FieldDescriptor came from - a deserializer and the mask
        // function it feeds must be built from the very same ParsedProtoSchema, even if two schemas are
        // structurally identical, or every field access throws "FieldDescriptor does not match message
        // type." So mask and unmask each get their own deserializer, unlike AvroUse, which reuses one.
        Function<ByteBuffer, DynamicMessage> maskDeserializer = new ProtobufBinaryDeserializer(maskSchema.descriptor());
        Function<ByteBuffer, DynamicMessage> unmaskDeserializer = new ProtobufBinaryDeserializer(unmaskSchema.descriptor());
        Function<DynamicMessage, ByteBuffer> serializer = new ProtobufBinarySerializer();
        ByteBuffer data = serializer.apply(user);

        // OpContext maskOpContext = new OpContext(new Random(), KEY);
        // Pipeline maskPipeline = new Pipeline(List.of(maskDeserializer, ProtobufFunction.buildMask(maskSchema, LOOKUP).bindRecord(maskOpContext), serializer));
        // ByteBuffer masked = maskPipeline.apply(data.duplicate());
        // LOGGER.atInfo().addKeyValue("masked", maskDeserializer.apply(masked.duplicate())).log("applied mask");
        //
        // OpContext unmaskOpContext = new OpContext(new Random(), KEY);
        // Pipeline unmaskPipeline = new Pipeline(List.of(unmaskDeserializer, ProtobufFunction.buildMask(unmaskSchema, LOOKUP).bindRecord(unmaskOpContext), serializer));
        // ByteBuffer unmasked = unmaskPipeline.apply(masked.duplicate());
        // LOGGER.atInfo().addKeyValue("unmasked", unmaskDeserializer.apply(unmasked.duplicate())).log("applied unmask");
    }
}
