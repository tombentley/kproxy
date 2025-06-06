/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.kroxylicious.proxy.config.tls.Tls;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;


/*
# This is the existing model
filterDefinitions:
  - name: my-filter
    type: MyFilter
    config:
      ...
virtualClusters:
  - name: my-cluster
    gateways:
      - name: my-gw
        portIdentifiesNode:
          bootstrapAddress: 0.0.0.0:9092
          advertisedBrokerAddressPattern: broker-$(nodeId).kafka.example.com
          nodeStartPort: 9092
          nodeIdRanges:
            name: brokers
            start: 9092
            end: 9094
    targetCluster:
      bootstrapServers: ...
      tls: ...
    filters:
      - my-filter
 */

/*
# This is a new model
filterDefinitions:
  - name: my-filter
    type: MyFilter
    config:
      ...
clusters:
  # A cluster is the same as a `virtualCluster.targetCluster`.
  # It gives a name to a combination of a Kafka cluster and the info needed to connect to it.
  - name: my-cluster
    bootstrapServers: ...
    tls: ...
  - name: my-old-cluster
    bootstrapServers: ...
    tls: ...

routers:
  # A router has a name, a type a config and a mapping of named outlets. The
  # type of the router defines the required and optional outlet names
  - name: disconnect
    type: Disconnector
  - name: my-authenticator
    type: Authenticator
    config: ...
    outlets: # routes?
      - name: on_success
        filters:
          - my-filter
        router: my-splicer
      - name: on_failure
        router: disconnect
      - name: on_timeout
        router: disconnect
  - name: my-splicer
    type: Splicer
    config: ...
    outlets:
      - name: old_cluster
        cluster: my-old_cluster
      - name: new_cluster
        cluster: my-cluster
virtualClusters:
  - name: my-ingress
    gateways:
      - name: my-gw
        portIdentifiesNode:
          bootstrapAddress: 0.0.0.0:9092
          advertisedBrokerAddressPattern: broker-$(nodeId).kafka.example.com
          nodeStartPort: 9092
          nodeIdRanges:
            name: brokers
            start: 9092
            end: 9094
    router: my-authenticator
    # no filters, no targetCluster
 */
/*
interface Router {
  // tell the router about the routes available to it

  void init(Set<String> routes)
    throws RouterInitializationException;

  CompletionStage<RoutingResult> onClientRequest(short apiVersion,
                                                 RequestHeader header,
                                                 RequestFrame requestBody,
                                                 RoutingContext context);

  // TODO routing responses???
  onResponse(String fromRoute)

}
interface RoutingContext {
  // TODO expose info about the client and server

  RoutingResultBuilder routingResultBuilder();
}
interface RoutingResultBuilder {
  // Forward the request on the given route (that was previously configured via init())
  CompletionStage<RoutingResult> forwardTo(String route);

  // Send a response to the client
  CompletionStage<RoutingResult> respondWith(ResponseFrame responseBody);

  // Disconnect from the client, tearing down any connection to a broker
  RoutingResultTerminal disconnect();

  // Make an out-of-band request
  CompletionStage<RoutingResult> makeRequest(String route,
                                             short apiVersion,
                                             RequestHeader header,
                                             RequestFrame requestBody,
                                             RoutingContext context);
}
interface RoutingResultTerminal {
  CompletionStage<RoutingResult> build();
}
 */

/**
 * A virtual cluster.
 *
 * @param name virtual cluster name
 * @param targetCluster the cluster being proxied
 * @param clusterNetworkAddressConfigProvider virtual cluster network config - deprecated - use a named gateway
 * @param tls deprecated - tls settings for the virtual cluster - deprecated - use a named gateway
 * @param gateways virtual cluster gateways
 * @param logNetwork if true, network will be logged
 * @param logFrames if true, kafka rpcs will be logged
 * @param filters filers.
 */
@SuppressWarnings("java:S1123") // suppressing the spurious warning about missing @deprecated in javadoc. It is the field that is deprecated, not the class.
public record VirtualCluster(@NonNull @JsonProperty(required = true) String name,
                             @NonNull @JsonProperty(required = true) TargetCluster targetCluster,
                             @Deprecated(forRemoval = true, since = "0.11.0") ClusterNetworkAddressConfigProviderDefinition clusterNetworkAddressConfigProvider,
                             @Deprecated(forRemoval = true, since = "0.11.0") @JsonProperty() Optional<Tls> tls,

                             @JsonProperty(required = false) List<VirtualClusterGateway> gateways,
                             boolean logNetwork,
                             boolean logFrames,
                             @Nullable List<String> filters,
                             @Nullable @JsonProperty("router") String routerName) {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualCluster.class);

    private static final Pattern DNS_LABEL_PATTERN = Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$", Pattern.CASE_INSENSITIVE);

    public VirtualCluster(@NonNull @JsonProperty(required = true) String name,
                   @JsonProperty TargetCluster targetCluster,
                   @Deprecated(forRemoval = true, since = "0.11.0") ClusterNetworkAddressConfigProviderDefinition clusterNetworkAddressConfigProvider,
                   @Deprecated(forRemoval = true, since = "0.11.0") @JsonProperty() Optional<Tls> tls,

                   @JsonProperty(required = false) List<VirtualClusterGateway> gateways,
                   boolean logNetwork,
                   boolean logFrames,
                   @Nullable List<String> filters) {
        this(name, targetCluster, clusterNetworkAddressConfigProvider, tls, gateways, logNetwork, logFrames, filters, null);
    }

    /**
     * Name given to the gateway defined using the deprecated fields.
     */
    @Deprecated(forRemoval = true, since = "0.11.0")
    static final String DEFAULT_GATEWAY_NAME = "default";

    @SuppressWarnings({ "removal", "java:S2789" }) // S2789 - checking for null tls is the intent
    public VirtualCluster {
        Objects.requireNonNull(name);
        if ((targetCluster == null) == (routerName == null)) {
            throw new IllegalConfigurationException("Exactly one of 'targetCluster' or 'router' must be specified");
        }

        if (filters != null && routerName != null) {
            throw new IllegalConfigurationException("'filters' cannot be configured directly on a virtual cluster when 'router' is specified. "
                    + "Configure `filters` on the route instead.");
        }

        if (!isDnsLabel(name)) {
            throw new IllegalConfigurationException(
                    "Virtual cluster name '" + name + "' is invalid. It must be less than 64 characters long and match pattern " + DNS_LABEL_PATTERN.pattern()
                            + " (case insensitive)");
        }

        if (clusterNetworkAddressConfigProvider != null || (tls != null && tls.isPresent())) {
            if (clusterNetworkAddressConfigProvider == null) {
                throw new IllegalConfigurationException("Deprecated virtualCluster property 'tls' supplied, but 'clusterNetworkAddressConfigProvider' is null");
            }
            if (gateways == null || gateways.isEmpty()) {
                LOGGER.warn(
                        "The 'clusterNetworkAddressConfigProvider' and 'tls' configuration properties are deprecated and will be removed in a future release.  Configurations should be updated to use 'gateways'.");
            }
            else {
                throw new IllegalConfigurationException(
                        "When using gateways, the virtualCluster properties 'clusterNetworkAddressConfigProvider' and 'tls' must be omitted");
            }
        }
        else {
            if (gateways == null || gateways.isEmpty()) {
                throw new IllegalConfigurationException("no gateways configured for virtualCluster");
            }
            if (gateways.stream().anyMatch(Objects::isNull)) {
                throw new IllegalConfigurationException("one or more gateways were null");
            }
            validateNoDuplicatedGatewayNames(gateways);
        }
    }

    boolean isDnsLabel(String name) {
        if (name.length() > 63) {
            return false;
        }
        else {
            return DNS_LABEL_PATTERN.matcher(name).matches();
        }
    }

    private void validateNoDuplicatedGatewayNames(List<VirtualClusterGateway> gateways) {
        var names = gateways.stream()
                .map(VirtualClusterGateway::name)
                .toList();
        var duplicates = names.stream()
                .filter(i -> Collections.frequency(names, i) > 1)
                .collect(Collectors.toSet());
        if (!duplicates.isEmpty()) {
            throw new IllegalConfigurationException(
                    "Gateway names for a virtual cluster must be unique. The following gateway names are duplicated: [%s]".formatted(
                            String.join(", ", duplicates)));
        }
    }

    @Deprecated(since = "0.11.0", forRemoval = true)
    @SuppressWarnings("java:S6207") // overriding the method to add the deprecated annotation
    public ClusterNetworkAddressConfigProviderDefinition clusterNetworkAddressConfigProvider() {
        return clusterNetworkAddressConfigProvider;
    }

    @Deprecated(since = "0.11.0", forRemoval = true)
    @SuppressWarnings("java:S6207") // overriding the method to add the deprecated annotation
    public Optional<Tls> tls() {
        return tls;
    }

}
