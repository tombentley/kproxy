/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.avro.AvroBinaryDeserializer;
import io.kroxylicious.filter.record.manipulation.avro.AvroBinarySerializer;
import io.kroxylicious.filter.record.manipulation.avro.AvroFunction;
import io.kroxylicious.filter.record.manipulation.common.Context;
import io.kroxylicious.filter.record.manipulation.common.EncryptStringFunction;
import io.kroxylicious.filter.record.manipulation.common.HmacStringFunction;
import io.kroxylicious.filter.record.manipulation.common.Pipeline;
import io.kroxylicious.filter.record.manipulation.common.PluginLookup;
import io.kroxylicious.filter.record.manipulation.common.RandomBytesSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomIntSupplier;
import io.kroxylicious.filter.record.manipulation.common.RandomStringSupplier;
import io.kroxylicious.filter.record.manipulation.common.ServiceLoaderPluginLookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Composes the same deserialize/mask/serialize stages as {@link io.kroxylicious.filter.record.manipulation.avro.AvroUse}'s
 * demo into {@link Pipeline}s and asserts on the result - the Avro equivalent of {@link MaskPipelineTest}.
 * <p>
 * Each schema below is deliberately scoped to as few fields as the test needs, rather than one big shared
 * schema: unlike JSON object properties, Avro record fields are processed in schema-declared order with no
 * separate "input order" to lean on, so keeping each PRNG-consuming field isolated in its own minimal schema
 * lets every expected value be computed independently (a single fresh seeded {@link Context} draws exactly
 * the value under test), rather than needing to replay a multi-field draw sequence by hand.
 */
@SuppressWarnings("java:S6213")
class AvroMaskPipelineTest {

    private static final String MASK_SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "firstName", "type": "string", "apply": [
                    {"op": "ValueString", "value": "REDACTED"}
                ]},
                {"name": "surname", "type": "string"},
                {"name": "address", "type": {"type": "record", "name": "Address", "fields": [
                    {"name": "streetAddress", "type": "string", "apply": [
                        {"op": "HmacString", "keyId": "FOO"}
                    ]},
                    {"name": "city", "type": "string", "apply": [
                        {"op": "EncryptString", "keyId": "FOO"}
                    ]}
                ]}}
            ]}
            """;

    private static final String ENCRYPT_THEN_HMAC_CITY = """
            {"type": "record", "name": "User", "fields": [
                {"name": "address", "type": {"type": "record", "name": "Address", "fields": [
                    {"name": "city", "type": "string", "apply": [
                        {"op": "EncryptString", "keyId": "FOO"},
                        {"op": "HmacString", "keyId": "FOO"}
                    ]}
                ]}}
            ]}
            """;

    private static final String HMAC_THEN_ENCRYPT_CITY = """
            {"type": "record", "name": "User", "fields": [
                {"name": "address", "type": {"type": "record", "name": "Address", "fields": [
                    {"name": "city", "type": "string", "apply": [
                        {"op": "HmacString", "keyId": "FOO"},
                        {"op": "EncryptString", "keyId": "FOO"}
                    ]}
                ]}}
            ]}
            """;

    private static final String RANDOM_INT_SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "ageYears", "type": "int", "apply": [
                    {"op": "RandomInt", "minInclusive": 18, "maxExclusive": 100}
                ]}
            ]}
            """;

    private static final String RANDOM_STRING_ARRAY_SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "aliases", "type": {"type": "array", "items": {"type": "string", "apply": [
                    {"op": "RandomString", "alphabet": "abcdefghijklmnopqrstuvwxyz", "minLengthInclusive": 3, "maxLengthExclusive": 15}
                ]}}}
            ]}
            """;

    private static final String VALUE_BYTES_SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "token", "type": "bytes", "apply": [
                    {"op": "ValueBytes", "value": "aGVsbG8="}
                ]}
            ]}
            """;

    private static final String RANDOM_BYTES_SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "token", "type": "bytes", "apply": [
                    {"op": "RandomBytes", "minLengthInclusive": 3, "maxLengthExclusive": 15}
                ]}
            ]}
            """;

    private static final String DELETE_SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "firstName", "type": "string", "apply": [
                    {"op": "Delete"}
                ]}
            ]}
            """;

    private static final String UNSUPPORTED_TYPE_SCHEMA_JSON = """
            {"type": "record", "name": "User", "fields": [
                {"name": "id", "type": {"type": "map", "values": "string"}}
            ]}
            """;

    /** A fixed seed, chosen arbitrarily, that pins every "random" mask and every encryption IV drawn below. */
    private static final long SEED = 42L;

    /** Matches the (currently hard-coded) key {@code AvroUse} uses for hmac/encrypt/decrypt. */
    private static final byte[] KEY = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };

    private static final PluginLookup LOOKUP = new ServiceLoaderPluginLookup();

    private static Context contextWithSeed(long seed) {
        return new Context(new Random(seed), KEY);
    }

    private static Schema schema(String json) {
        return new Schema.Parser().parse(json);
    }

    private static GenericRecord mask(GenericRecord record, Schema schema, Context context) {
        Function<ByteBuffer, GenericRecord> deserializer = new AvroBinaryDeserializer(schema);
        Function<GenericRecord, ByteBuffer> serializer = new AvroBinarySerializer(schema);
        Pipeline pipeline = new Pipeline(List.of(deserializer, AvroFunction.buildMask(schema, LOOKUP).bindRecord(context), serializer));
        return deserializer.apply(pipeline.apply(serializer.apply(record)));
    }

    private static String hmacOf(String plaintext) {
        return new HmacStringFunction().apply(plaintext, contextWithSeed(0));
    }

    private static GenericRecord userWithAddress(Schema schema, String streetAddress, String city) {
        GenericRecord address = new GenericData.Record(schema.getField("address").schema());
        address.put("streetAddress", streetAddress);
        address.put("city", city);
        GenericRecord user = new GenericData.Record(schema);
        user.put("firstName", "Harry");
        user.put("surname", "Potter");
        user.put("address", address);
        return user;
    }

    @Test
    void pipelineDeserializesMasksAndReserializesARecordDeterministically() {
        // Given
        Schema schema = schema(MASK_SCHEMA_JSON);
        GenericRecord data = userWithAddress(schema, "Hogwarts", "Hogsmead");

        // When
        GenericRecord masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        assertThat(masked.get("firstName")).isInstanceOf(Utf8.class).hasToString("REDACTED");
        assertThat(masked.get("surname")).isInstanceOf(Utf8.class).hasToString("Potter");
        GenericRecord maskedAddress = (GenericRecord) masked.get("address");
        assertThat(maskedAddress.get("streetAddress")).isInstanceOf(Utf8.class).hasToString(hmacOf("Hogwarts"));
        assertThat(maskedAddress.get("city")).isInstanceOf(Utf8.class).hasToString(new EncryptStringFunction().apply("Hogsmead", contextWithSeed(SEED)));
    }

    @Test
    void maskingWithTheSameSeedIsRepeatable() {
        // Given
        Schema schema = schema(MASK_SCHEMA_JSON);
        GenericRecord data = userWithAddress(schema, "Hogwarts", "Hogsmead");

        // When
        GenericRecord first = mask(data, schema, contextWithSeed(SEED));
        GenericRecord second = mask(data, schema, contextWithSeed(SEED));

        // Then
        assertThat(second).isEqualTo(first);
    }

    @Test
    void maskThenUnmaskPipelineRoundTripsTheEncryptedFieldButNotTheHmacedField() {
        // Given
        Schema maskSchema = schema(MASK_SCHEMA_JSON);
        Schema unmaskSchema = schema(MASK_SCHEMA_JSON.replace("EncryptString", "DecryptString"));
        GenericRecord data = userWithAddress(maskSchema, "Hogwarts", "Hogsmead");

        // When
        GenericRecord masked = mask(data, maskSchema, contextWithSeed(SEED));
        GenericRecord unmasked = mask(masked, unmaskSchema, contextWithSeed(SEED));

        // Then
        GenericRecord unmaskedAddress = (GenericRecord) unmasked.get("address");
        assertThat(unmaskedAddress.get("city")).isInstanceOf(Utf8.class).hasToString("Hogsmead");
        // hmac has no inverse, so the unmask pass re-hmacs the already-masked value rather than recovering "Hogwarts".
        assertThat(unmaskedAddress.get("streetAddress")).isInstanceOf(Utf8.class).hasToString(hmacOf(hmacOf("Hogwarts")));
    }

    @Test
    void composedApplyChainAppliesOperationsInDeclaredOrder() {
        // Given
        Schema schema = schema(ENCRYPT_THEN_HMAC_CITY);
        GenericRecord address = new GenericData.Record(schema.getField("address").schema());
        address.put("city", "Hogsmead");
        GenericRecord data = new GenericData.Record(schema);
        data.put("address", address);

        // When
        GenericRecord masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        String encryptedFirst = new EncryptStringFunction().apply("Hogsmead", contextWithSeed(SEED));
        GenericRecord maskedAddress = (GenericRecord) masked.get("address");
        assertThat(maskedAddress.get("city")).isInstanceOf(Utf8.class).hasToString(hmacOf(encryptedFirst));
    }

    @Test
    void composedApplyChainIsOrderSensitive() {
        // Given
        Schema encryptThenHmac = schema(ENCRYPT_THEN_HMAC_CITY);
        Schema hmacThenEncrypt = schema(HMAC_THEN_ENCRYPT_CITY);
        GenericRecord encryptThenHmacAddress = new GenericData.Record(encryptThenHmac.getField("address").schema());
        encryptThenHmacAddress.put("city", "Hogsmead");
        GenericRecord encryptThenHmacData = new GenericData.Record(encryptThenHmac);
        encryptThenHmacData.put("address", encryptThenHmacAddress);
        GenericRecord hmacThenEncryptAddress = new GenericData.Record(hmacThenEncrypt.getField("address").schema());
        hmacThenEncryptAddress.put("city", "Hogsmead");
        GenericRecord hmacThenEncryptData = new GenericData.Record(hmacThenEncrypt);
        hmacThenEncryptData.put("address", hmacThenEncryptAddress);

        // When
        GenericRecord encryptFirstResult = mask(encryptThenHmacData, encryptThenHmac, contextWithSeed(SEED));
        GenericRecord hmacFirstResult = mask(hmacThenEncryptData, hmacThenEncrypt, contextWithSeed(SEED));

        // Then
        GenericRecord encryptFirstAddress = (GenericRecord) encryptFirstResult.get("address");
        GenericRecord hmacFirstAddress = (GenericRecord) hmacFirstResult.get("address");
        assertThat(encryptFirstAddress.get("city").toString()).isNotEqualTo(hmacFirstAddress.get("city").toString());
    }

    @Test
    void applyRandomGeneratesADeterministicIntWithASeededContext() {
        // Given
        Schema schema = schema(RANDOM_INT_SCHEMA_JSON);
        GenericRecord data = new GenericData.Record(schema);
        data.put("ageYears", 17);

        // When
        GenericRecord masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        assertThat(masked.get("ageYears")).isEqualTo(new RandomIntSupplier(18, 100).applyAsInt(contextWithSeed(SEED)));
    }

    @Test
    void applyRandomGeneratesADeterministicStringPerArrayElementWithASeededContext() {
        // Given
        Schema schema = schema(RANDOM_STRING_ARRAY_SCHEMA_JSON);
        GenericRecord data = new GenericData.Record(schema);
        data.put("aliases", List.of("Vernon Dudley"));

        // When
        GenericRecord masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        String expected = new RandomStringSupplier("abcdefghijklmnopqrstuvwxyz", 3, 15).apply(contextWithSeed(SEED));
        assertThat(((List<?>) masked.get("aliases")).get(0)).isInstanceOf(Utf8.class).hasToString(expected);
    }

    @Test
    void applyValueReplacesABytesFieldWithAFixedValue() {
        // Given
        Schema schema = schema(VALUE_BYTES_SCHEMA_JSON);
        GenericRecord data = new GenericData.Record(schema);
        data.put("token", ByteBuffer.wrap("world".getBytes(StandardCharsets.UTF_8)));

        // When
        GenericRecord masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        assertThat(masked.get("token")).isEqualTo(ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void applyRandomGeneratesADeterministicBytesArrayWithASeededContext() {
        // Given
        Schema schema = schema(RANDOM_BYTES_SCHEMA_JSON);
        GenericRecord data = new GenericData.Record(schema);
        data.put("token", ByteBuffer.wrap(new byte[0]));

        // When
        GenericRecord masked = mask(data, schema, contextWithSeed(SEED));

        // Then
        byte[] expected = new RandomBytesSupplier(3, 15).apply(contextWithSeed(SEED));
        assertThat(masked.get("token")).isEqualTo(ByteBuffer.wrap(expected));
    }

    @Test
    void applyDeleteIsNotYetSupportedForAvroFields() {
        // Given
        Schema schema = schema(DELETE_SCHEMA_JSON);

        // When/Then
        assertThatThrownBy(() -> AvroFunction.buildMask(schema, LOOKUP)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildMaskThrowsForASchemaTypeNotYetSupported() {
        // Given
        Schema schema = schema(UNSUPPORTED_TYPE_SCHEMA_JSON);

        // When/Then
        assertThatThrownBy(() -> AvroFunction.buildMask(schema, LOOKUP)).isInstanceOf(IllegalArgumentException.class);
    }

}
