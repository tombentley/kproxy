/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.transforms;

import java.util.function.Consumer;
import java.util.stream.Stream;

import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.transform.service.HeadersTransform;

public class RemoveFirstHeader implements HeadersTransform {

    private final String key;

    RemoveFirstHeader(String key) {
        this.key = key;
    }

    @Override
    public Stream<Header> transform(Stream<Header> headers) {
        Stream.Builder<Header> builder = Stream.builder();
        headers.forEach(
                new Consumer<>() {
                    boolean found = false;

                    @Override
                    public void accept(Header header) {
                        if (!header.key().equals(key)
                                || !found) {
                            found = true;
                            builder.add(header);
                        }
                    }
                });
        return builder.build();
    }
}
