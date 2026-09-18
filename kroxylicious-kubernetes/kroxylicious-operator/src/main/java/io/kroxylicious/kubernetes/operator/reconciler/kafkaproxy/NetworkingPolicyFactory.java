/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator.reconciler.kafkaproxy;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;

import io.kroxylicious.proxy.config.NetworkRequirements;

/**
 * A factory for resources representing any kind of network policy/firewall rules or similar.
 * @param <R> The type of resources built
 */
interface NetworkingPolicyFactory<R extends HasMetadata> {

    R buildPolicy(Map<String, String> proxyPodSelector,
                  BiFunction<String, String, Service> proxyServices,
                  NetworkRequirements proxyNetworkRequirements);

}

