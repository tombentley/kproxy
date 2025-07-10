/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.filter;

import io.kroxylicious.proxy.authentication.ClientPrincipalProducer;
import io.kroxylicious.proxy.authentication.SaslPrincipal;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;

@ClientPrincipalProducer(SaslPrincipal.class)
@Plugin(configType = Void.class)
public class SaslPlainTermination
        implements FilterFactory<Void, Void> {

    @Override
    public Void initialize(FilterFactoryContext context, Void config) throws PluginConfigurationException {
        return null;
    }

    @Override
    public Filter createFilter(FilterFactoryContext context, Void initializationData) {
        return new SaslPlainTerminationFilter();
    }
}
