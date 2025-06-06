/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.routing;

import io.kroxylicious.proxy.config.NamedFilterDefinition;
import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;

record InitializedFilterFactory<I>(
        NamedFilterDefinition filterDefinition,
        FilterFactory<?, I> filterFactory,
        I initializationData
) {
    Filter create(FilterFactoryContext context) {
        return filterFactory.createFilter(context, initializationData);
    }

    // TODO arrange to close the filterFactory
}
