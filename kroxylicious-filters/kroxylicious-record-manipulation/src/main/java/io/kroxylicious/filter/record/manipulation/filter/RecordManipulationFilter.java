/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import io.kroxylicious.filter.record.manipulation.filter.config.Direction;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
import io.kroxylicious.filter.record.manipulation.filter.resolve.ManipulationApplier;
import io.kroxylicious.filter.record.manipulation.filter.resolve.PartitionWorkItem;
import io.kroxylicious.kafka.common.Uuid;
import io.kroxylicious.kafka.common.message.FetchResponseData;
import io.kroxylicious.kafka.common.message.ProduceRequestData;
import io.kroxylicious.kafka.common.message.RequestHeaderData;
import io.kroxylicious.kafka.common.message.ResponseHeaderData;
import io.kroxylicious.kafka.common.message.ShareFetchResponseData;
import io.kroxylicious.kafka.common.protocol.Errors;
import io.kroxylicious.kafka.common.record.internal.BaseRecords;
import io.kroxylicious.kafka.common.record.internal.MemoryRecords;
import io.kroxylicious.proxy.filter.FetchResponseFilter;
import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.ProduceRequestFilter;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.ResponseFilterResult;
import io.kroxylicious.proxy.filter.ShareFetchResponseFilter;
import io.kroxylicious.proxy.filter.metadata.TopicNameMapping;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A filter that selects records ({@code when}) and rewrites them ({@code then}). Matching partitions are
 * gathered into work items and transformed by the {@link ManipulationApplier}, which drives the
 * {@code records} transform engine per partition and copies unmatched records verbatim.
 */
public class RecordManipulationFilter implements
        ProduceRequestFilter,
        FetchResponseFilter,
        ShareFetchResponseFilter {

    private static final Logger LOGGER = getLogger(RecordManipulationFilter.class);

    private final Init init;

    private final ManipulationApplier applier;

    public RecordManipulationFilter(Init init, Executor filterExecutor) {
        this.init = init;
        this.applier = new ManipulationApplier(filterExecutor);
    }

    @Override
    public CompletionStage<RequestFilterResult> onProduceRequest(short apiVersion,
                                                                 RequestHeaderData header,
                                                                 ProduceRequestData request,
                                                                 FilterContext context) {
        List<Uuid> idsToResolve = request.topicData().stream()
                .filter(topicData -> topicData.name().isEmpty())
                .map(ProduceRequestData.TopicProduceData::topicId)
                .toList();
        return context.topicNames(idsToResolve).thenCompose(topicNameMapping -> {
            List<PartitionWorkItem> workItems = new ArrayList<>();
            for (var topicData : request.topicData()) {
                String topicName = resolveTopicName(topicData.name(), topicData.topicId(), topicNameMapping);
                var matchingManipulations = init.compiledManipulations().stream()
                        .filter(manipulation -> manipulation.direction() == Direction.INTO_CLUSTER)
                        .filter(manipulation -> manipulation.topicMatcher().test(topicName))
                        .toList();
                if (!matchingManipulations.isEmpty()) {
                    for (var partitionData : topicData.partitionData()) {
                        addWorkItem(matchingManipulations, workItems, topicName, partitionData.records(), partitionData::setRecords);
                    }
                }
            }
            if (workItems.isEmpty()) {
                LOGGER.atDebug()
                        .addKeyValue("topic", () -> topicNames(request, topicNameMapping))
                        .log("ProduceRequest requires no manipulation");
                return context.forwardRequest(header, request);
            }
            LOGGER.atDebug()
                    .addKeyValue("topic", () -> topicNames(request, topicNameMapping))
                    .addKeyValue("workItems", workItems)
                    .log("ProduceRequest may require manipulation");
            return applier.applyProduce(workItems, context::createByteBufferOutputStream)
                    .thenCompose(ignored -> context.forwardRequest(header, request));
        });
    }

    @NonNull
    private static String topicNames(ProduceRequestData request, TopicNameMapping topicNameMapping) {
        return request.topicData().stream().map(td -> resolveTopicName(td.name(), td.topicId(), topicNameMapping)).collect(Collectors.joining(","));
    }

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion,
                                                                 ResponseHeaderData header,
                                                                 FetchResponseData response,
                                                                 FilterContext context) {
        List<Uuid> idsToResolve = response.responses().stream()
                .filter(topicData -> topicData.topic().isEmpty())
                .map(FetchResponseData.FetchableTopicResponse::topicId)
                .toList();
        return context.topicNames(idsToResolve).thenCompose(topicNameMapping -> {
            List<PartitionWorkItem> workItems = new ArrayList<>();
            for (var topicData : response.responses()) {
                String topicName = resolveTopicName(topicData.topic(), topicData.topicId(), topicNameMapping);
                var matchingManipulations = init.compiledManipulations().stream()
                        .filter(manipulation -> manipulation.direction() == Direction.OUT_OF_CLUSTER)
                        .filter(manipulation -> manipulation.topicMatcher().test(topicName))
                        .toList();
                if (!matchingManipulations.isEmpty()) {
                    for (var partitionData : topicData.partitions()) {
                        addWorkItem(matchingManipulations, workItems, topicName, partitionData.records(), partitionData::setRecords);
                    }
                }
            }
            if (workItems.isEmpty()) {
                LOGGER.atDebug()
                        .addKeyValue("topics", () -> topicNames(response, topicNameMapping))
                        .log("FetchResponse requires no manipulation");
                return context.forwardResponse(header, response);
            }

            LOGGER.atDebug()
                    .addKeyValue("topics", () -> topicNames(response, topicNameMapping))
                    .addKeyValue("workItems", workItems)
                    .log("FetchResponse may require manipulation");
            return applier.applyFetch(workItems, context::createByteBufferOutputStream)
                    .thenCompose(ignored -> context.forwardResponse(header, response));
        });
    }

    @NonNull
    private static String topicNames(FetchResponseData response, TopicNameMapping topicNameMapping) {
        return response.responses().stream().map(td -> resolveTopicName(td.topic(), td.topicId(), topicNameMapping)).collect(Collectors.joining(","));
    }

    @Override
    public CompletionStage<ResponseFilterResult> onShareFetchResponse(short apiVersion,
                                                                      ResponseHeaderData header,
                                                                      ShareFetchResponseData response,
                                                                      FilterContext context) {
        List<Uuid> idsToResolve = response.responses().stream()
                .map(ShareFetchResponseData.ShareFetchableTopicResponse::topicId)
                .toList();
        return context.topicNames(idsToResolve).thenCompose(topicMapping -> {
            List<PartitionWorkItem> workItems = new ArrayList<>();
            for (var topicData : response.responses()) {
                if (topicMapping.failures().containsKey(topicData.topicId())) {
                    for (var partitionData : topicData.partitions()) {
                        partitionData.setRecords(null);
                        partitionData.setErrorCode(Errors.UNKNOWN_TOPIC_ID.code());
                        partitionData.setErrorMessage("Oops");
                    }
                }
                else {
                    String topicName = topicMapping.topicNames().get(topicData.topicId());
                    var matchingManipulations = init.compiledManipulations().stream()
                            .filter(manipulation -> manipulation.direction() == Direction.OUT_OF_CLUSTER)
                            .filter(manipulation -> manipulation.topicMatcher().test(topicName))
                            .toList();
                    if (!matchingManipulations.isEmpty()) {
                        for (var partitionData : topicData.partitions()) {
                            addWorkItem(matchingManipulations, workItems, topicName, partitionData.records(), partitionData::setRecords);
                        }
                    }
                }
            }
            if (workItems.isEmpty()) {
                LOGGER.atDebug()
                        .addKeyValue("topics", () -> topicNames(response, topicMapping))
                        .log("ShareFetchResponse requires no manipulation");
                return context.forwardResponse(header, response);
            }
            LOGGER.atDebug()
                    .addKeyValue("topics", () -> topicNames(response, topicMapping))
                    .addKeyValue("workItems", workItems)
                    .log("ShareFetchResponse may require manipulation");
            return applier.applyFetch(workItems, context::createByteBufferOutputStream)
                    .thenCompose(ignored -> context.forwardResponse(header, response));
        });
    }

    @NonNull
    private static String topicNames(ShareFetchResponseData response, TopicNameMapping topicNameMapping) {
        return response.responses().stream().map(td -> topicNameMapping.topicNames().get(td.topicId())).collect(Collectors.joining(","));
    }

    private void addWorkItem(List<CompiledManipulation<?, ?, ?>> manipulations,
                             List<PartitionWorkItem> workItems,
                             String topicName,
                             @Nullable BaseRecords records,
                             Consumer<MemoryRecords> writeBack) {
        if (records instanceof MemoryRecords memoryRecords) {
            workItems.add(new PartitionWorkItem(topicName, memoryRecords, manipulations, writeBack));
        }
    }

    /**
     * Resolves a topic's name, falling back to {@code topicNameMapping} when {@code name} is absent -
     * true from Produce (v13+, KIP-951) and Fetch (v13+, KIP-516) protocol versions onwards, which
     * identify topics by {@code topicId} alone.
     */
    @Nullable
    private static String resolveTopicName(String name, Uuid topicId, TopicNameMapping topicNameMapping) {
        return name.isEmpty() ? topicNameMapping.topicNames().get(topicId) : name;
    }
}
