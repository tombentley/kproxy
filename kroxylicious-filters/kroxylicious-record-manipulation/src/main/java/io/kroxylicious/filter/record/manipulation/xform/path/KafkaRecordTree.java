/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.xform.path;

import java.util.List;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.record.internal.Record;

public class KafkaRecordTree implements TreeAdapter<Object> {

    @Override
    public boolean isObject(Object node) {
        return node instanceof Record || node instanceof Header;
    }

    @Override
    public boolean isArray(Object node) {
        return node instanceof Header[];
    }

    @Override
    public Iterable<? extends Prop<Object>> objectProperties(Object node) {
        if (node instanceof Record record) {
            return List.of(
                    new Prop<>("timestamp", record.timestamp()),
                    new Prop<>("sequence", record.sequence()),
                    new Prop<>("headers", record.headers()),
                    new Prop<>("offset", record.offset()),
                    new Prop<>("key", record.key()),
                    new Prop<>("value", record.value()));
        }
        else if (node instanceof Header header) {
            return List.of(new Prop<>("key", header.key()),
                    new Prop<>("value", header.value()));
        }
        throw new IllegalArgumentException("Unknown node type: " + node.getClass());
    }

    @Override
    public int arrayLength(Object node) {
        return ((Header[]) node).length;
    }

    @Override
    public Object arrayItem(Object node, int index) {
        return ((Header[]) node)[index];
    }
}
