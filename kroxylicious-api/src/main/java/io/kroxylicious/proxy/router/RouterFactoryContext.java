/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.router;

import io.kroxylicious.proxy.filter.FilterDispatchExecutor;

import edu.umd.cs.findbugs.annotations.NonNull;

public interface RouterFactoryContext {

    FilterDispatchExecutor dispatchExecutor();

    <P> @NonNull P pluginInstance(@NonNull Class<P> pluginClass, @NonNull String instanceName);
}
