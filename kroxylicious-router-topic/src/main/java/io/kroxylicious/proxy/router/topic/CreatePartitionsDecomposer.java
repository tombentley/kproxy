/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.proxy.router.topic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.message.CreatePartitionsRequestData;
import org.apache.kafka.common.message.CreatePartitionsRequestData.CreatePartitionsTopic;
import org.apache.kafka.common.message.CreatePartitionsResponseData;
import org.apache.kafka.common.message.CreatePartitionsResponseData.CreatePartitionsTopicResult;
import org.apache.kafka.common.protocol.Errors;

/**
 * Splits a CREATE_PARTITIONS request by topic ownership and merges
 * the per-route responses.
 */
class CreatePartitionsDecomposer implements RequestDecomposer<CreatePartitionsRequestData, CreatePartitionsResponseData> {

    static final String CROSS_ROUTE_ASSIGNMENTS_MESSAGE = "Partition assignment references brokers from a route other than the topic's owning route";

    private final BiPredicate<Integer, String> canServeRoute;

    CreatePartitionsDecomposer(BiPredicate<Integer, String> canServeRoute) {
        this.canServeRoute = canServeRoute;
    }

    @Override
    public Map<String, CreatePartitionsRequestData> decompose(CreatePartitionsRequestData request,
                                                              TopicRoutingTable table,
                                                              short apiVersion,
                                                              Function<Uuid, String> topicNameResolver) {
        var result = new LinkedHashMap<String, CreatePartitionsRequestData>();
        for (var topic : request.topics()) {
            String route = table.routeForTopic(topic.name());
            if (route != null && !hasInvalidAssignments(topic, route)) {
                result.computeIfAbsent(route, k -> copyEnvelope(request))
                        .topics().add(topic.duplicate());
            }
        }
        return result;
    }

    @Override
    public CreatePartitionsResponseData recompose(Map<String, CreatePartitionsResponseData> responses,
                                                  CreatePartitionsRequestData originalRequest,
                                                  short apiVersion) {
        var merged = new CreatePartitionsResponseData();
        int maxThrottle = 0;
        for (var resp : responses.values()) {
            for (var topicResult : resp.results()) {
                merged.results().add(topicResult.duplicate());
            }
            maxThrottle = Math.max(maxThrottle, resp.throttleTimeMs());
        }
        merged.setThrottleTimeMs(maxThrottle);
        return merged;
    }

    static CreatePartitionsResponseData errorResponseForUnroutableTopics(CreatePartitionsRequestData request,
                                                                         TopicRoutingTable table) {
        var errorResponse = new CreatePartitionsResponseData();
        for (var topic : request.topics()) {
            if (!table.isRoutable(topic.name())) {
                errorResponse.results().add(
                        new CreatePartitionsTopicResult()
                                .setName(topic.name())
                                .setErrorCode(Errors.UNKNOWN_TOPIC_OR_PARTITION.code()));
            }
        }
        return errorResponse;
    }

    CreatePartitionsResponseData errorResponseForInvalidAssignments(
                                                                    CreatePartitionsRequestData request,
                                                                    TopicRoutingTable table) {
        var errorResponse = new CreatePartitionsResponseData();
        for (var topic : request.topics()) {
            String route = table.routeForTopic(topic.name());
            if (route != null && hasInvalidAssignments(topic, route)) {
                errorResponse.results().add(
                        new CreatePartitionsTopicResult()
                                .setName(topic.name())
                                .setErrorCode(Errors.INVALID_REPLICA_ASSIGNMENT.code())
                                .setErrorMessage(CROSS_ROUTE_ASSIGNMENTS_MESSAGE));
            }
        }
        return errorResponse;
    }

    private static boolean hasAssignments(CreatePartitionsTopic topic) {
        return topic.assignments() != null && !topic.assignments().isEmpty();
    }

    private boolean hasInvalidAssignments(CreatePartitionsTopic topic, String owningRoute) {
        if (!hasAssignments(topic)) {
            return false;
        }
        for (var assignment : topic.assignments()) {
            for (int brokerId : assignment.brokerIds()) {
                if (!canServeRoute.test(brokerId, owningRoute)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static CreatePartitionsRequestData copyEnvelope(CreatePartitionsRequestData original) {
        var copy = new CreatePartitionsRequestData();
        copy.setTimeoutMs(original.timeoutMs());
        copy.setValidateOnly(original.validateOnly());
        return copy;
    }
}
