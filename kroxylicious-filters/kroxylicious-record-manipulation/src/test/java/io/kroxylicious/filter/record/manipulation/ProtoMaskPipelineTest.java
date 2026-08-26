/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;

import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.EncryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.HmacStringFunction;
import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.ServiceLoaderPluginLookup;
import io.kroxylicious.filter.record.manipulation.protobuf.ParsedProtoSchema;
import io.kroxylicious.filter.record.manipulation.protobuf.ProtoBinaryDeserializer;
import io.kroxylicious.filter.record.manipulation.protobuf.ProtoBinarySerializer;
import io.kroxylicious.filter.record.manipulation.protobuf.ProtoFunction;
import io.kroxylicious.filter.record.manipulation.protobuf.ProtoSchemaParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Composes deserialize/mask/serialize stages built from {@link ProtoSchemaParser}/{@link ProtoFunction}
 * into {@link Pipeline}s and asserts on the result - the Protobuf equivalent of
 * {@link io.kroxylicious.filter.record.manipulation.AvroMaskPipelineTest}.
 * <p>
 * Each schema below is deliberately scoped to as few fields as the test needs, rather than one big shared
 * schema, for the same reason {@code AvroMaskPipelineTest} gives: keeping each PRNG-consuming field
 * isolated in its own minimal schema lets every expected value be computed independently.
 */
class ProtoMaskPipelineTest {

    private static final String MASK_SCHEMA_PROTO = """
            syntax = "proto3";
            message User {
                string first_name = 1 [(apply) = { op: "ValueString", value: "REDACTED" }];
                string surname = 2;
                Address address = 3;
                message Address {
                    string street_address = 1 [(apply) = { op: "HmacString", keyId: "FOO" }];
                    string city = 2 [(apply) = { op: "EncryptString", keyId: "FOO" }];
                }
            }
            """;

    private static final String ENCRYPT_THEN_HMAC_CITY = """
            syntax = "proto3";
            message User {
                Address address = 1;
                message Address {
                    string city = 1 [(apply) = { op: "EncryptString", keyId: "FOO" }, (apply) = { op: "HmacString", keyId: "FOO" }];
                }
            }
            """;

    private static final String HMAC_THEN_ENCRYPT_CITY = """
            syntax = "proto3";
            message User {
                Address address = 1;
                message Address {
                    string city = 1 [(apply) = { op: "HmacString", keyId: "FOO" }, (apply) = { op: "EncryptString", keyId: "FOO" }];
                }
            }
            """;

    private static final String RANDOM_INT_SCHEMA_PROTO = """
            syntax = "proto3";
            message User {
                int32 age_years = 1 [(apply) = { op: "RandomInt", minInclusive: 18, maxExclusive: 100 }];
            }
            """;

    private static final String RANDOM_STRING_REPEATED_SCHEMA_PROTO = """
            syntax = "proto3";
            message User {
                repeated string aliases = 1 [(apply) = { op: "RandomString", alphabet: "abcdefghijklmnopqrstuvwxyz", minLengthInclusive: 3, maxLengthExclusive: 15 }];
            }
            """;

    private static final String DELETE_SCHEMA_PROTO = """
            syntax = "proto3";
            message User {
                string first_name = 1 [(apply) = { op: "Delete" }];
            }
            """;

    private static final String UNSUPPORTED_TYPE_SCHEMA_PROTO = """
            syntax = "proto3";
            message User {
                int64 id = 1;
            }
            """;

    /** A fixed seed, chosen arbitrarily, that pins every "random" mask and every encryption IV drawn below. */
    private static final long SEED = 42L;

    /** Matches the (currently hard-coded) key {@link io.kroxylicious.filter.record.manipulation.protobuf.ProtoUse} uses for hmac/encrypt/decrypt. */
    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

    private static final PluginLookup LOOKUP = new ServiceLoaderPluginLookup();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), KEY);
    }

    private static ParsedProtoSchema schema(String proto) {
        return ProtoSchemaParser.parse(proto, "User");
    }

    private static DynamicMessage mask(DynamicMessage message, ParsedProtoSchema schema, Context context) {
        Function<ByteBuffer, DynamicMessage> deserializer = new ProtoBinaryDeserializer(schema.descriptor());
        Function<DynamicMessage, ByteBuffer> serializer = new ProtoBinarySerializer();
        Pipeline pipeline = new Pipeline(List.of(deserializer, ProtoFunction.buildMask(schema, LOOKUP).bindRecord(context), serializer));
        return deserializer.apply(pipeline.apply(serializer.apply(message)));
    }

    private static String hmacOf(String plaintext) {
        return new HmacStringFunction().apply(plaintext, contextWithSeed(0));
    }

    private static DynamicMessage userWithAddress(ParsedProtoSchema schema, String streetAddress, String city) {
        Descriptors.Descriptor addressDescriptor = schema.descriptor().findNestedTypeByName("Address");
        DynamicMessage address = DynamicMessage.newBuilder(addressDescriptor)
                .setField(addressDescriptor.findFieldByName("street_address"), streetAddress)
                .setField(addressDescriptor.findFieldByName("city"), city)
                .build();
        return DynamicMessage.newBuilder(schema.descriptor())
                .setField(schema.descriptor().findFieldByName("first_name"), "Harry")
                .setField(schema.descriptor().findFieldByName("surname"), "Potter")
                .setField(schema.descriptor().findFieldByName("address"), address)
                .build();
    }

    @Test
    void pipelineDeserializesMasksAndReserializesARecordDeterministically() {
        // Given
        ParsedProtoSchema schema = schema(MASK_SCHEMA_PROTO);
        DynamicMessage data = userWithAddress(schema, "Hogwarts", "Hogsmead");

        // When
        DynamicMessage masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        assertThat(masked.getField(schema.descriptor().findFieldByName("first_name"))).isEqualTo("REDACTED");
        assertThat(masked.getField(schema.descriptor().findFieldByName("surname"))).isEqualTo("Potter");
        DynamicMessage maskedAddress = (DynamicMessage) masked.getField(schema.descriptor().findFieldByName("address"));
        Descriptors.Descriptor addressDescriptor = schema.descriptor().findNestedTypeByName("Address");
        assertThat(maskedAddress.getField(addressDescriptor.findFieldByName("street_address"))).isEqualTo(hmacOf("Hogwarts"));
        assertThat(maskedAddress.getField(addressDescriptor.findFieldByName("city")))
                .isEqualTo(new EncryptStringFunction().apply("Hogsmead", contextWithSeed(SEED)));
    }

    @Test
    void maskingWithTheSameSeedIsRepeatable() {
        // Given
        ParsedProtoSchema schema = schema(MASK_SCHEMA_PROTO);
        DynamicMessage data = userWithAddress(schema, "Hogwarts", "Hogsmead");

        // When
        DynamicMessage first = mask(data, schema, contextWithSeed(SEED));
        DynamicMessage second = mask(data, schema, contextWithSeed(SEED));

        // Then
        assertThat(second).isEqualTo(first);
    }

    @Test
    void maskThenUnmaskPipelineRoundTripsTheEncryptedFieldButNotTheHmacedField() {
        // Given
        ParsedProtoSchema maskSchema = schema(MASK_SCHEMA_PROTO);
        ParsedProtoSchema unmaskSchema = schema(MASK_SCHEMA_PROTO.replace("EncryptString", "DecryptString"));
        DynamicMessage data = userWithAddress(maskSchema, "Hogwarts", "Hogsmead");

        // When
        DynamicMessage masked = mask(data, maskSchema, contextWithSeed(SEED));
        DynamicMessage unmasked = mask(masked, unmaskSchema, contextWithSeed(SEED));

        // Then
        Descriptors.Descriptor addressDescriptor = unmaskSchema.descriptor().findNestedTypeByName("Address");
        DynamicMessage unmaskedAddress = (DynamicMessage) unmasked.getField(unmaskSchema.descriptor().findFieldByName("address"));
        assertThat(unmaskedAddress.getField(addressDescriptor.findFieldByName("city"))).isEqualTo("Hogsmead");
        // hmac has no inverse, so the unmask pass re-hmacs the already-masked value rather than recovering "Hogwarts".
        assertThat(unmaskedAddress.getField(addressDescriptor.findFieldByName("street_address"))).isEqualTo(hmacOf(hmacOf("Hogwarts")));
    }

    @Test
    void composedApplyChainAppliesOperationsInDeclaredOrder() {
        // Given
        ParsedProtoSchema schema = schema(ENCRYPT_THEN_HMAC_CITY);
        Descriptors.Descriptor addressDescriptor = schema.descriptor().findNestedTypeByName("Address");
        DynamicMessage address = DynamicMessage.newBuilder(addressDescriptor).setField(addressDescriptor.findFieldByName("city"), "Hogsmead").build();
        DynamicMessage data = DynamicMessage.newBuilder(schema.descriptor()).setField(schema.descriptor().findFieldByName("address"), address).build();

        // When
        DynamicMessage masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        String encryptedFirst = new EncryptStringFunction().apply("Hogsmead", contextWithSeed(SEED));
        DynamicMessage maskedAddress = (DynamicMessage) masked.getField(schema.descriptor().findFieldByName("address"));
        assertThat(maskedAddress.getField(addressDescriptor.findFieldByName("city"))).isEqualTo(hmacOf(encryptedFirst));
    }

    @Test
    void composedApplyChainIsOrderSensitive() {
        // Given
        ParsedProtoSchema encryptThenHmac = schema(ENCRYPT_THEN_HMAC_CITY);
        ParsedProtoSchema hmacThenEncrypt = schema(HMAC_THEN_ENCRYPT_CITY);
        Descriptors.Descriptor encryptThenHmacAddressDescriptor = encryptThenHmac.descriptor().findNestedTypeByName("Address");
        DynamicMessage encryptThenHmacAddress = DynamicMessage.newBuilder(encryptThenHmacAddressDescriptor)
                .setField(encryptThenHmacAddressDescriptor.findFieldByName("city"), "Hogsmead").build();
        DynamicMessage encryptThenHmacData = DynamicMessage.newBuilder(encryptThenHmac.descriptor())
                .setField(encryptThenHmac.descriptor().findFieldByName("address"), encryptThenHmacAddress).build();
        Descriptors.Descriptor hmacThenEncryptAddressDescriptor = hmacThenEncrypt.descriptor().findNestedTypeByName("Address");
        DynamicMessage hmacThenEncryptAddress = DynamicMessage.newBuilder(hmacThenEncryptAddressDescriptor)
                .setField(hmacThenEncryptAddressDescriptor.findFieldByName("city"), "Hogsmead").build();
        DynamicMessage hmacThenEncryptData = DynamicMessage.newBuilder(hmacThenEncrypt.descriptor())
                .setField(hmacThenEncrypt.descriptor().findFieldByName("address"), hmacThenEncryptAddress).build();

        // When
        DynamicMessage encryptFirstResult = mask(encryptThenHmacData, encryptThenHmac, contextWithSeed(SEED));
        DynamicMessage hmacFirstResult = mask(hmacThenEncryptData, hmacThenEncrypt, contextWithSeed(SEED));

        // Then
        DynamicMessage encryptFirstAddress = (DynamicMessage) encryptFirstResult.getField(encryptThenHmac.descriptor().findFieldByName("address"));
        DynamicMessage hmacFirstAddress = (DynamicMessage) hmacFirstResult.getField(hmacThenEncrypt.descriptor().findFieldByName("address"));
        assertThat(encryptFirstAddress.getField(encryptThenHmacAddressDescriptor.findFieldByName("city")))
                .isNotEqualTo(hmacFirstAddress.getField(hmacThenEncryptAddressDescriptor.findFieldByName("city")));
    }

    @Test
    void applyRandomGeneratesADeterministicIntWithASeededContext() {
        // Given
        ParsedProtoSchema schema = schema(RANDOM_INT_SCHEMA_PROTO);
        DynamicMessage data = DynamicMessage.newBuilder(schema.descriptor()).setField(schema.descriptor().findFieldByName("age_years"), 17).build();

        // When
        DynamicMessage masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        assertThat(masked.getField(schema.descriptor().findFieldByName("age_years")))
                .isEqualTo(new RandomIntSupplier(18, 100).applyAsInt(contextWithSeed(SEED)));
    }

    @Test
    void applyRandomGeneratesADeterministicStringPerRepeatedElementWithASeededContext() {
        // Given
        ParsedProtoSchema schema = schema(RANDOM_STRING_REPEATED_SCHEMA_PROTO);
        DynamicMessage data = DynamicMessage.newBuilder(schema.descriptor())
                .addRepeatedField(schema.descriptor().findFieldByName("aliases"), "Vernon Dudley")
                .build();

        // When
        DynamicMessage masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        String expected = new RandomStringSupplier("abcdefghijklmnopqrstuvwxyz", 3, 15).apply(contextWithSeed(SEED));
        @SuppressWarnings("unchecked")
        List<Object> aliases = (List<Object>) masked.getField(schema.descriptor().findFieldByName("aliases"));
        assertThat(aliases).containsExactly(expected);
    }

    @Test
    void applyDeleteIsNotYetSupportedForProtoFields() {
        // Given
        ParsedProtoSchema schema = schema(DELETE_SCHEMA_PROTO);

        // When/Then
        assertThatThrownBy(() -> ProtoFunction.buildMask(schema, LOOKUP)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildMaskThrowsForAFieldTypeNotYetSupported() {
        // Given
        ParsedProtoSchema schema = schema(UNSUPPORTED_TYPE_SCHEMA_PROTO);

        // When/Then
        assertThatThrownBy(() -> ProtoFunction.buildMask(schema, LOOKUP)).isInstanceOf(IllegalArgumentException.class);
    }

}
