/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.transformation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.Record;
import org.apache.kafka.common.record.RecordBatch;

import io.kroxylicious.filter.transformation.api.RecordDataLocation;
import io.kroxylicious.filter.transformation.api.mapper.Context;
import io.kroxylicious.filter.transformation.api.schema.identification.NoSchemaId;
import io.kroxylicious.filter.transformation.api.schema.identification.SchemaIdDeserializer;
import io.kroxylicious.filter.transformation.api.schema.identification.WireSchemaId;
import io.kroxylicious.kafka.transform.RecordConsumer;
import io.kroxylicious.kafka.transform.RecordStream;

/**
 * A {@link RecordConsumer} that builds up a Set of the {@linkplain WireSchemaId schema ids} in a bunch of records.
 */
@SuppressWarnings("java:S6213") // `record` is a perfectly acceptable identifier
class SchemaIdConsumer implements RecordConsumer<Void> {

    private final Map<RecordDataLocation, SchemaIdDeserializer<?>> recordTransform;
    private final String topicName;
    private final Set<WireSchemaId> schemas = new HashSet<>();

    private SchemaIdConsumer(String topicName,
                             Map<RecordDataLocation, SchemaIdDeserializer<?>> recordTransform) {
        this.topicName = topicName;
        this.recordTransform = recordTransform;
    }

    static Set<WireSchemaId> schemaIds(String topicName,
                                       Map<RecordDataLocation, SchemaIdDeserializer<?>> extractors,
                                       MemoryRecords records) {
        SchemaIdConsumer mapper = new SchemaIdConsumer(topicName, extractors);
        RecordStream.ofRecords(records).forEachRecord(mapper);
        return mapper.schemas();
    }

    public Set<WireSchemaId> schemas() {
        return Collections.unmodifiableSet(schemas);
    }

    @Override
    public void accept(RecordBatch batch, Record record, Void state) {
        try {
            maybeAddSchemaId(RecordDataLocation.KEY, record);
            maybeAddSchemaId(RecordDataLocation.VALUE, record);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void maybeAddSchemaId(RecordDataLocation site,
                                  Record record) throws IOException {
        SchemaIdDeserializer<?> schemaIdDeserializer = recordTransform.get(site);
        Context context = new Context(topicName, List.of(record.headers()), site);
        WireSchemaId wireSchemaId = schemaId(record, context, schemaIdDeserializer);
        if (wireSchemaId != null && !(wireSchemaId instanceof NoSchemaId)) {
            this.schemas.add(wireSchemaId);
        }
    }

    public static WireSchemaId schemaId(Record record,
                                        Context context,
                                        SchemaIdDeserializer<?> schemaIdDeserializer)
            throws IOException {
        ByteBuffer buffer = context.location().buffer(record);
        TransformationInputStream in = new TransformationInputStream(buffer != null ? buffer : ByteBuffer.wrap(new byte[0]));
        var schemaAndValue = schemaIdDeserializer.deserialize(in, context);
        return schemaAndValue.schemaId();
    }
}
