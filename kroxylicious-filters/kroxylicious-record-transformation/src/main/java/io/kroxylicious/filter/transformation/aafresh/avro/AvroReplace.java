/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation.aafresh.avro;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

import io.kroxylicious.filter.transformation.aafresh.DataMapping2;
import io.kroxylicious.filter.transformation.api.mapper.Context;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * <p>Replace a node in a "generic" Avro tree, identified by a given pointer.
 * The replacement is represented as the JSON-encoded Avro having the same schema
 * as the value it is replacing. This mapping is therefore schema preserving.
 * </p>
 */
public class AvroReplace implements DataMapping2<AvroRoot, AvroRoot> {

    private String field;
    private int index;

    // the bytes of the json encoding of the replacement json
    private byte[] replacement;

    @Nullable
    @Override
    public AvroRoot transform(@Nullable AvroRoot value, Context context) {
        // TODO schema could be a union
        if (value.root() instanceof GenericRecord gr) {
            gr.put(field, replacement(gr.getSchema().getField(field).schema()));
            return value;
        }
        else if (value.root() instanceof GenericData.AbstractArray<?> array) {
            ((List) array).set(index, replacement(array.getSchema().getElementType()));
            return value;
        }
        else if (value.root() instanceof Map<?, ?> map) {
            ((Map) map).put(field, replacement(value.schema().getValueType()));
            return value;
        }
        else if (value.root() instanceof GenericData.Fixed fixed) {
            return new AvroRoot(replacement(fixed.getSchema()), fixed.getSchema());
        }
        else if (value.root() instanceof GenericData.EnumSymbol symbol) {
            return new AvroRoot(replacement(symbol.getSchema()), symbol.getSchema());
        }
        return new AvroRoot(replacement(value.schema()), value.schema());
    }

    Object replacement(Schema replacementSchema) {
        //TODO cache the replacement using the schema as a key
        //  or just maybe
        //  long cacheKey = SchemaNormalization.parsingFingerprint64(replacementSchema);
        var reader = new GenericDatumReader<>(replacementSchema);
        DecoderFactory decoderFactory = DecoderFactory.get();
        Decoder decoder = decoderFactory.jsonDecoder(replacementSchema, new ByteArrayInputStream(replacement));

        Object read = reader.read(null, decoder);

        return read;
    }
}
