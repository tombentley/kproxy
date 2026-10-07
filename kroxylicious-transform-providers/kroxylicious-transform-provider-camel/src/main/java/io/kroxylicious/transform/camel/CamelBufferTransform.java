/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.camel;

import java.nio.ByteBuffer;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;

import io.kroxylicious.transform.records.RecordView;
import io.kroxylicious.transform.service.BufferTransform;

public class CamelBufferTransform implements BufferTransform {

    private final CamelContext context;
    private final ProducerTemplate template;

    public CamelBufferTransform(CamelContext context) {
        this.context = context;
        this.template = context.createProducerTemplate();
    }

    @Override
    public ByteBuffer apply(RecordView record) {
        Exchange exchange = ExchangeBuilder.anExchange(context)
                .withBody(record.key())
//                .withHeader("Tenant-ID", record.)
//                .withHeader("Schema-Version", "v2")
//                .withProperty("TraceId", "tx-12345")
                .withPattern(ExchangePattern.InOut) // Request-Reply
                .build();
        Exchange send = template.send("direct:transformApp", exchange);
        return send.getMessage().getBody(ByteBuffer.class);
    }
}
