/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.authorization;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.kafka.common.message.DeleteTopicsRequestData;
import org.apache.kafka.common.message.DeleteTopicsResponseData;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.protocol.Errors;

import io.kroxylicious.authorizer.service.Action;
import io.kroxylicious.authorizer.service.Decision;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.metadata.TopicNameMapping;

public class DeleteTopicsEnforcement extends ApiEnforcement<DeleteTopicsRequestData, DeleteTopicsResponseData> {

    @Override
    short minSupportedVersion() {
        return 1;
    }

    @Override
    short maxSupportedVersion() {
        // DELETE_TOPICS: v6 allows topic ids: It's better to force the client to use v5 than fail a v6 later on
        // when the client goes ahead and uses topic ids
        return 6;
    }

    /**
     * Buffer responses for topics where the subject lacks DELETE
     */
    @Override
    CompletionStage<RequestFilterResult> onRequest(RequestHeaderData header,
                                                   DeleteTopicsRequestData request,
                                                   FilterContext context,
                                                   AuthorizationFilter authorizationFilter) {
        short apiVersion = header.requestApiVersion();
        boolean useStates = apiVersion >= 6;
        final CompletableFuture<TopicNameMapping> mappingStage;
        CompletionStage<List<Action>> actionsStage;
        TopicResource operation = TopicResource.DELETE;
        if (useStates) {
            var partitioned = request.topics().stream().collect(Collectors.partitioningBy(t -> t.name() != null));
            List<DeleteTopicsRequestData.DeleteTopicState> statesUsingIds = partitioned.get(false);
            if (statesUsingIds.isEmpty()) {
                actionsStage = CompletableFuture.completedStage(operation.actionsOf(partitioned.get(true).stream()
                        .map(DeleteTopicsRequestData.DeleteTopicState::name)));
                mappingStage = null;
            }
            else {
                mappingStage = new CompletableFuture<>();
                context.topicNames(statesUsingIds.stream()
                        .map(DeleteTopicsRequestData.DeleteTopicState::topicId)
                        .toList()).whenComplete((mapping, error) -> {
                            // painfully, the stage returned by context.topicNames() cannot be converted to a future
                            // so we have to make our own
                            if (error == null) {
                                mappingStage.complete(mapping);
                            }
                            else {
                                mappingStage.completeExceptionally(error);
                            }
                        });
                CompletionStage<List<Action>> actionsStage1 = mappingStage.thenApply(mapping -> {
                    Stream<String> names = request.topics().stream()
                            .map(t -> {
                                return mapping.topicNames().get(t.topicId());
                            });
                    List<Action> actions = operation.actionsOf(names);
                    return actions;
                });
                actionsStage = actionsStage1;
            }
        }
        else {
            actionsStage = CompletableFuture.completedStage(operation.actionsOf(request.topicNames().stream()));
            mappingStage = null;
        }
        return actionsStage
                .thenCompose(actions -> authorizationFilter.authorization(context, actions))
                .thenCompose(authorization -> {
                    if (useStates) {
                        Map<Decision, List<DeleteTopicsRequestData.DeleteTopicState>> decisions;
                        if (mappingStage != null) {
                            decisions = authorization.partition(request.topics(), operation,
                                    state -> mappingStage.join().topicNames().get(state.topicId()));
                        }
                        else {
                            decisions = authorization.partition(request.topics(), operation,
                                    DeleteTopicsRequestData.DeleteTopicState::name);
                        }
                        if (decisions.get(Decision.ALLOW).isEmpty()) {
                            // Shortcircuit if there's no allowed actions
                            DeleteTopicsResponseData.DeletableTopicResultCollection v = new DeleteTopicsResponseData.DeletableTopicResultCollection();
                            request.topics().stream()
                                    .map(topicState -> topicAuthzFailed(apiVersion, topicState))
                                    .forEach(v::mustAdd);
                            return context.requestFilterResultBuilder()
                                    .shortCircuitResponse(
                                            new DeleteTopicsResponseData()
                                                    .setResponses(v))
                                    .completed();
                        }
                        else if (decisions.get(Decision.DENY).isEmpty()) {
                            // Just forward if there's no denied actions
                            return context.forwardRequest(header, request);
                        }
                        else {
                            request.setTopics(request.topics().stream()
                                    .filter(topicState -> authorization.decision(operation, topicState.name()) == Decision.ALLOW)
                                    .toList());

                            var list = decisions.get(Decision.DENY)
                                    .stream().map(t -> topicAuthzFailed(apiVersion, t))
                                    .toList();
                            authorizationFilter.pushInflightState(header, (DeleteTopicsResponseData response) -> {
                                response.responses().addAll(list);
                                return response;
                            });
                            return context.forwardRequest(header, request);
                        }
                    }
                    else { // using topic names
                        var decisions = authorization.partition(request.topicNames(), operation, Function.identity());
                        if (decisions.get(Decision.ALLOW).isEmpty()) {
                            // Shortcircuit if there's no allowed actions
                            DeleteTopicsResponseData.DeletableTopicResultCollection v = new DeleteTopicsResponseData.DeletableTopicResultCollection();
                            request.topicNames().stream()
                                    .map(topicName -> topicAuthzFailed(apiVersion, topicName))
                                    .forEach(v::mustAdd);
                            return context.requestFilterResultBuilder()
                                    .shortCircuitResponse(
                                            new DeleteTopicsResponseData()
                                                    .setResponses(v))
                                    .completed();
                        }
                        else if (decisions.get(Decision.DENY).isEmpty()) {
                            // Just forward if there's no denied actions
                            return context.forwardRequest(header, request);
                        }
                        else {
                            request.setTopicNames(request.topicNames().stream()
                                    .filter(tn -> authorization.decision(operation, tn) == Decision.ALLOW)
                                    .toList());

                            var list = decisions.get(Decision.DENY)
                                    .stream().map(t -> topicAuthzFailed(apiVersion, t))
                                    .toList();
                            authorizationFilter.pushInflightState(header, (DeleteTopicsResponseData response) -> {
                                response.responses().addAll(list);
                                return response;
                            });
                            return context.forwardRequest(header, request);
                        }
                    }
                });
    }

    static DeleteTopicsResponseData.DeletableTopicResult topicAuthzFailed(short apiVersion,
                                                                          DeleteTopicsRequestData.DeleteTopicState state) {

        if (apiVersion < 6) {
            throw new IllegalStateException();
        }
        return topicAuthzFailed(apiVersion, new DeleteTopicsResponseData.DeletableTopicResult())
                .setTopicId(state.topicId())
                .setName(state.name());
    }

    static DeleteTopicsResponseData.DeletableTopicResult topicAuthzFailed(short apiVersion,
                                                                          DeleteTopicsResponseData.DeletableTopicResult topicResult) {
        return topicResult
                .setErrorCode(Errors.TOPIC_AUTHORIZATION_FAILED.code())
                .setErrorMessage(apiVersion >= 5 ? Errors.TOPIC_AUTHORIZATION_FAILED.message() : null);
    }

    static DeleteTopicsResponseData.DeletableTopicResult topicAuthzFailed(short apiVersion,
                                                                          String topicName) {
        if (apiVersion >= 6) {
            throw new IllegalStateException();
        }
        return topicAuthzFailed(apiVersion, new DeleteTopicsResponseData.DeletableTopicResult())
                .setName(topicName);
    }
}
