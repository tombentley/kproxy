/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.internal.codec;

import org.apache.kafka.common.protocol.ApiKeys;

import io.kroxylicious.proxy.filter.KrpcFilter;

public interface DecodePredicate {
    public static DecodePredicate forFilters(KrpcFilter... filters) {
        return new DecodePredicate() {
            @Override
            public boolean shouldDecodeResponse(ApiKeys apiKey, short apiVersion) {
                for (var filter : filters) {
                    if (filter.shouldDeserializeResponse(apiKey, apiVersion)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean shouldDecodeRequest(ApiKeys apiKey, short apiVersion) {
                for (var filter : filters) {
                    if (filter.shouldDeserializeRequest(apiKey, apiVersion)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public boolean shouldDecodeRequest(ApiKeys apiKey, short apiVersion);

    public boolean shouldDecodeResponse(ApiKeys apiKey, short apiVersion);

}
