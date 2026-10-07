/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.transform.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;

import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.PluginLookup;

public class CamelBufferTransportService implements BufferTransformService<CamelBufferTransportService.Config, CamelBufferTransportService.Init> {
    @Override
    public Init configure(PluginLookup pluginLookup, Config config) {
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:transformApp")
                        .filter("", """)
                        .to("dataformat:jackson:unmarshal")
                        //.unmarshal().json(JsonLibrary.Jackson)
                        .to("jslt:my-transform.jslt")
                        .to("dataformat:jackson:marshal")
                        .marshal().json(JsonLibrary.Jackson);
            }
        });
        context.start();
        return new Init(context);
    }

    @Override
    public BufferTransform create(Init initializationData) {
        return new CamelBufferTransform(initializationData.context());
    }

    @Override
    public void close(Init initializationData) {
        initializationData.context().stop();
    }

    public record Config() {}
    public record Init(CamelContext context) {}

}
