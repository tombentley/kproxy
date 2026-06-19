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
import org.apache.kafka.common.message.CreateTopicsRequestData;
import org.apache.kafka.common.message.CreateTopicsRequestData.CreatableTopic;
import org.apache.kafka.common.message.CreateTopicsResponseData;
import org.apache.kafka.common.message.CreateTopicsResponseData.CreatableTopicResult;
import org.apache.kafka.common.protocol.Errors;

/**
 * Splits a CREATE_TOPICS request by topic ownership and merges
 * the per-route responses.
 */
class CreateTopicsDecomposer implements RequestDecomposer<CreateTopicsRequestData, CreateTopicsResponseData> {

    static final String CROSS_ROUTE_ASSIGNMENTS_MESSAGE = "Replica assignment references brokers from a route other than the topic's owning route";

    private final BiPredicate<Integer, String> canServeRoute;

    CreateTopicsDecomposer(BiPredicate<Integer, String> canServeRoute) {
        this.canServeRoute = canServeRoute;
    }

    @Override
    public Map<String, CreateTopicsRequestData> decompose(CreateTopicsRequestData request,
                                                          TopicRoutingTable table,
                                                          short apiVersion,
                                                          Function<Uuid, String> topicNameResolver) {
        var result = new LinkedHashMap<String, CreateTopicsRequestData>();
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
    public CreateTopicsResponseData recompose(Map<String, CreateTopicsResponseData> responses,
                                              CreateTopicsRequestData originalRequest,
                                              short apiVersion) {
        var merged = new CreateTopicsResponseData();
        int maxThrottle = 0;
        for (var resp : responses.values()) {
            for (var topic : resp.topics()) {
                merged.topics().add(topic.duplicate());
            }
            maxThrottle = Math.max(maxThrottle, resp.throttleTimeMs());
        }
        merged.setThrottleTimeMs(maxThrottle);
        return merged;
    }

    static CreateTopicsResponseData errorResponseForUnroutableTopics(CreateTopicsRequestData request,
                                                                     TopicRoutingTable table) {
        var errorResponse = new CreateTopicsResponseData();
        for (var topic : request.topics()) {
            if (!table.isRoutable(topic.name())) {
                errorResponse.topics().add(
                        new CreatableTopicResult()
                                .setName(topic.name())
                                .setErrorCode(Errors.UNKNOWN_TOPIC_OR_PARTITION.code()));
            }
        }
        return errorResponse;
    }

    CreateTopicsResponseData errorResponseForInvalidAssignments(
                                                                CreateTopicsRequestData request,
                                                                TopicRoutingTable table) {
        var errorResponse = new CreateTopicsResponseData();
        for (var topic : request.topics()) {
            String route = table.routeForTopic(topic.name());
            if (route != null && hasInvalidAssignments(topic, route)) {
                errorResponse.topics().add(
                        new CreatableTopicResult()
                                .setName(topic.name())
                                .setErrorCode(Errors.INVALID_REPLICA_ASSIGNMENT.code())
                                .setErrorMessage(CROSS_ROUTE_ASSIGNMENTS_MESSAGE));
            }
        }
        return errorResponse;
    }

    private boolean hasInvalidAssignments(CreatableTopic topic, String owningRoute) {
        if (topic.assignments().isEmpty()) {
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

    private static CreateTopicsRequestData copyEnvelope(CreateTopicsRequestData original) {
        var copy = new CreateTopicsRequestData();
        copy.setTimeoutMs(original.timeoutMs());
        copy.setValidateOnly(original.validateOnly());
        return copy;
    }
}
