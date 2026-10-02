/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.resolve;

import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.List;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.transform.service.KafkaRecord;
import io.kroxylicious.transform.service.KafkaRecordHeader;

class RecordWrapper implements KafkaRecord {
    private final io.kroxylicious.kafka.common.record.internal.Record record;

    RecordWrapper(Record record) {
        this.record = record;
    }

    @Override
    public long timestamp() {
        return record.timestamp();
    }

    @Override
    public ByteBuffer key() {
        return record.key();
    }

    @Override
    public ByteBuffer value() {
        return record.value();
    }

    @Override
    public List<? extends KafkaRecordHeader> headers() {
        return new AbstractList<KafkaRecordHeader>() {
            @Override
            public KafkaRecordHeader get(int index) {
                Header[] headers = record.headers();
                if (headers == null || index >= headers.length) {
                    throw new IndexOutOfBoundsException();
                }
                return new KafkaRecordHeader() {
                    String key = headers[index].key();
                    ByteBuffer readOnlyBuffer = ByteBuffer.wrap(headers[index].value()).asReadOnlyBuffer();

                    @Override
                    public String key() {
                        return key;
                    }

                    @Override
                    public ByteBuffer value() {
                        return readOnlyBuffer;
                    }
                };
            }

            @Override
            public int size() {
                return record.headers().length;
            }
        };
    }
}
