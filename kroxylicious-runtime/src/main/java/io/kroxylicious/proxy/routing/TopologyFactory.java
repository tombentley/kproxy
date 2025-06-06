/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.routing;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.kroxylicious.proxy.config.ClusterDefinition;
import io.kroxylicious.proxy.config.Configuration;
import io.kroxylicious.proxy.config.NamedFilterDefinition;
import io.kroxylicious.proxy.config.PluginFactory;
import io.kroxylicious.proxy.config.PluginFactoryRegistry;
import io.kroxylicious.proxy.config.RouteDefinition;
import io.kroxylicious.proxy.config.RouterDefinition;
import io.kroxylicious.proxy.config.VirtualCluster;
import io.kroxylicious.proxy.filter.FilterDispatchExecutor;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;
import io.kroxylicious.proxy.router.RouterFactory;
import io.kroxylicious.proxy.router.RouterFactoryContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class TopologyFactory {

    private final Map<String, InitializedFilterFactory<?>> filterFactoriesByName;
    private final Map<String, ClusterDefinition> clusterDefinitionsByName;
    private final Map<String, InitializedRouterFactory<?>> routerFactoriesByName;

    @NonNull
    static <T>  Stream<T> stream(@Nullable Collection<T> collection) {
        return Optional.ofNullable(collection).stream().flatMap(Collection::stream);
    }

    TopologyFactory(PluginFactoryRegistry pfr, Configuration configuration) {
        PluginFactory<RouterFactory<?, ?>> routerPluginFactory = (PluginFactory) pfr.pluginFactory(RouterFactory.class);
        PluginFactory<FilterFactory<?, ?>> filterPluginFactory = (PluginFactory) pfr.pluginFactory(FilterFactory.class);
        FilterDispatchExecutor executor = null; // TODO

        RouterFactoryContextImpl ctx = new RouterFactoryContextImpl(pfr, executor);
        FilterFactoryContext ffc = new FilterFactoryContext() {
            @Override
            public FilterDispatchExecutor filterDispatchExecutor() {
                return executor;
            }

            @Override
            public <P> @NonNull P pluginInstance(@NonNull Class<P> pluginClass, @NonNull String instanceName) {
                return pfr.pluginFactory(pluginClass).pluginInstance(instanceName);
            }
        };
        this.routerFactoriesByName = stream(configuration.routers())
                .collect(Collectors.toMap(
                        RouterDefinition::routerName,
                        rd -> {
                            RouterFactory routerFactory = routerFactory(routerPluginFactory, rd);
                            return initialize(routerFactory, rd, ctx);
                        }));
        this.filterFactoriesByName = stream(configuration.filterDefinitions())
                .collect(Collectors.toMap(
                        NamedFilterDefinition::name,
                        fd -> {
                            FilterFactory<?, ?> filterFactory = filterFactory(filterPluginFactory, fd);
                            return initialize(filterFactory, fd, ffc);
                        }));
        this.clusterDefinitionsByName = stream(configuration.clusters())
                .collect(Collectors.toMap(
                        ClusterDefinition::clusterName,
                        Function.identity()));

    }

    private static @NonNull FilterFactory<?, ?> filterFactory(PluginFactory<FilterFactory<?, ?>> filterPluginFactory,
                                                              NamedFilterDefinition filterDefinition) {
        var configType = filterPluginFactory.configType(filterDefinition.type());
        if (filterDefinition.config() == null || configType.isInstance(filterDefinition.config())) {
            return filterPluginFactory.pluginInstance(filterDefinition.type());
        }
        throw new PluginConfigurationException("Filter '" + filterDefinition.name() + "' accepts config of type '" +
                configType.getName() + "' but provided with config of type '" + filterDefinition.config().getClass().getName() + "'");
    }

    private static @NonNull RouterFactory<?, ?> routerFactory(PluginFactory<RouterFactory<?, ?>> routerPluginFactory,
                                                              RouterDefinition routerDefinition) {
        var configType = routerPluginFactory.configType(routerDefinition.type());
        if (routerDefinition.config() == null || configType.isInstance(routerDefinition.config())) {
            RouterFactory<?, ?> routerFactory = routerPluginFactory.pluginInstance(routerDefinition.type());
            return routerFactory;
        }
        throw new PluginConfigurationException("Router '" + routerDefinition.routerName() + "' accepts config of type '" +
                configType.getName() + "' but provided with config of type '" + routerDefinition.config().getClass().getName() + "'");
    }

    static <C, I> InitializedRouterFactory<I> initialize(RouterFactory<C, I> routerFactory,
                                                         RouterDefinition routerDefinition,
                                                         RouterFactoryContext context) {
        I initializationData = routerFactory.initialize(context, (C) routerDefinition.config(),
                stream(routerDefinition.routes()).map(RouteDefinition::routeName).collect(Collectors.toSet()));
        return new InitializedRouterFactory(routerDefinition, context, routerFactory, initializationData);
    }

    static <C, I> InitializedFilterFactory<I> initialize(FilterFactory<C, I> filterFactory,
                                                         NamedFilterDefinition filterDefinition,
                                                         FilterFactoryContext context) {
        I initializationData = filterFactory.initialize(context, (C) filterDefinition.config());
        return new InitializedFilterFactory<>(filterDefinition, filterFactory, initializationData);
    }

    public TopologyHandler createHandler(VirtualCluster vc) {
        return new TopologyHandler(vc, this.routerFactoriesByName, this.filterFactoriesByName, this.clusterDefinitionsByName);
    }
}
