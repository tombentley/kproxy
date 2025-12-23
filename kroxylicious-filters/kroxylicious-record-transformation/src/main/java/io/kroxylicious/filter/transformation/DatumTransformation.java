/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.apache.kafka.common.header.Header;

/**
 * <p>A transformation pipeline for {@link Datum}s which start, and end, as byte sequences with associated schema metadata.</p>
 *
 * <p>Deserialization yields a schema identifier, but mapping of datums do not preserve schemas.
 * This is an intentional design decision because it's often not possible to for a transformation developer to
 * define a generic transformation which is statically known to be schema preserving,
 * nor is it always feasible for them to derive a schema in tandem with transforming the data.
 * For example, with JsonPatch, it's impossible to know whether a given patch is schema preserving without
 * also knowing the json document it will be applied to and the schema of that document.
 * The approach taken is to allow arbitrary transformation of the data
 * without preserving the schema, and to allow the end user to define a result schema at the end of the
 * pipeline (in addition to how it gets serialized).</p>
 *
 * <p>The composed transformation must follow Java's usual type assignability rules.
 * A simple check is made at runtime using {@link Class#isAssignableFrom(Class)}, but
 * this is susceptible to false positives because of type erasure.
 * For example trying to compose a {@code DatumDeserializer<Integer>} with a {@code DatumSerializer<String>}
 * should be detected early, but
 * composing a {@code DatumDeserializer<List<Integer>>} with a {@code DatumSerializer<List<String>>}
 * won't be detected immediately and could fail later at runtime.</p>
 *
 * @param deserializer The deserializer
 * @param mappers The mappers to sequentially apply
 * @param serializer The serializer
 */
record DatumTransformation(
        DatumDeserializer<?> deserializer,
        List<DatumMapper<?, ?>> mappers,
        DatumSerializer<?> serializer
) {
    DatumTransformation {
        Objects.requireNonNull(deserializer);
        Objects.requireNonNull(mappers);
        Objects.requireNonNull(serializer);
        var type = deserializer.returnedType();
        var typeSource = "the deserializer of type " + deserializer.getClass().getName();
        for (int i = 0; i < mappers.size(); i++) {
            DatumMapper<?, ?> mapper = mappers.get(i);
            if (!mapper.acceptedType().isAssignableFrom(type)) {
                throw new IllegalArgumentException("The mapper of type " + mapper.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from " + typeSource);
            }
            type = mapper.returnedType();
            typeSource = "the mapper of type " + mapper.getClass().getName();
        }
        if (!serializer.acceptedType().isAssignableFrom(type)) {
            throw new IllegalArgumentException("The serializer of type " + serializer.getClass().getName() + " cannot accept values of type " + type.getName() + " returned from " + typeSource);
        }
    }

    void apply(Header[] headers,
               TransformationInputStream in,
               TransformationOutputStream out) throws IOException {
        var datum = deserializer.deserialize(headers, in);
        var value = datum.datum();
        for (DatumMapper mapper : mappers) {
            value = mapper.transform(value);
        }
        ((DatumSerializer) serializer).serialize(new Datum(NoSchema.INSTANCE, Object.class, value), out);
    }
}
