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

import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
import io.kroxylicious.filter.record.manipulation.filter.resolve.ContextualBufferTransform;
import io.kroxylicious.filter.record.manipulation.filter.resolve.ManipulationOrchestrator;
import io.kroxylicious.filter.record.manipulation.filter.resolve.PartitionWorkItem;
import io.kroxylicious.filter.record.manipulation.filter.resolve.SchemaFacets;
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

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A filter that selects records ({@code when}) and rewrites them ({@code then}). Selection and
 * rewriting may depend on "related things" (e.g. schema-registry content) that must be fetched from an
 * external system; the {@link ManipulationOrchestrator} pools those lookups across all partitions in a
 * request/response, resolves them (deduplicated and cached), and applies the matched transforms.
 */
public class RecordManipulationFilter implements
        ProduceRequestFilter,
        FetchResponseFilter,
        ShareFetchResponseFilter {

    private final String topic;
    private final Direction direction;
    private final List<CompiledManipulation> manipulations;
    private final ManipulationOrchestrator orchestrator;

    public RecordManipulationFilter(Init init, Executor filterExecutor, @Nullable SchemaFacets schemaFacets) {
        this.topic = init.topic();
        this.direction = init.direction();
        this.manipulations = List.of(compile(init));
        this.orchestrator = new ManipulationOrchestrator(schemaFacets, filterExecutor);
    }

    /**
     * Compiles the (single, global) manipulation expressed by the current configuration: it selects
     * every record of the configured topic/direction and applies the configured timestamp/key/value
     * transforms. Per-manipulation, schema-selecting configuration is resolved and applied by the same
     * {@link ManipulationOrchestrator} once the configuration surface exposes it.
     */
    private CompiledManipulation compile(Init init) {
        return new CompiledManipulation(
                init.direction(),
                name -> true,
                null,
                null,
                null,
                init.recordTimestampPipeline(),
                ContextualBufferTransform.of(init.recordKeyPipeline()),
                ContextualBufferTransform.of(init.recordValuePipeline()));
    }

    @Override
    public CompletionStage<RequestFilterResult> onProduceRequest(short apiVersion,
                                                                 RequestHeaderData header,
                                                                 ProduceRequestData request,
                                                                 FilterContext context) {
        if (this.direction != Direction.IN) {
            return context.forwardRequest(header, request);
        }
        List<Uuid> idsToResolve = request.topicData().stream()
                .filter(topicData -> topicData.name().isEmpty())
                .map(ProduceRequestData.TopicProduceData::topicId)
                .toList();
        return context.topicNames(idsToResolve).thenCompose(topicNameMapping -> {
            List<PartitionWorkItem> workItems = new ArrayList<>();
            for (var topicData : request.topicData()) {
                String topicName = resolveTopicName(topicData.name(), topicData.topicId(), topicNameMapping);
                if (topic.equals(topicName)) {
                    for (var partitionData : topicData.partitionData()) {
                        addWorkItem(workItems, topicName, partitionData.records(), partitionData::setRecords);
                    }
                }
            }
            return orchestrator.process(workItems, context::createByteBufferOutputStream)
                    .thenCompose(ignored -> context.forwardRequest(header, request));
        });
    }

    @Override
    public CompletionStage<ResponseFilterResult> onFetchResponse(short apiVersion,
                                                                 ResponseHeaderData header,
                                                                 FetchResponseData response,
                                                                 FilterContext context) {
        if (this.direction != Direction.OUT) {
            return context.forwardResponse(header, response);
        }
        List<Uuid> idsToResolve = response.responses().stream()
                .filter(topicData -> topicData.topic().isEmpty())
                .map(FetchResponseData.FetchableTopicResponse::topicId)
                .toList();
        return context.topicNames(idsToResolve).thenCompose(topicNameMapping -> {
            List<PartitionWorkItem> workItems = new ArrayList<>();
            for (var topicData : response.responses()) {
                String topicName = resolveTopicName(topicData.topic(), topicData.topicId(), topicNameMapping);
                if (topic.equals(topicName)) {
                    for (var partitionData : topicData.partitions()) {
                        addWorkItem(workItems, topicName, partitionData.records(), partitionData::setRecords);
                    }
                }
            }
            return orchestrator.process(workItems, context::createByteBufferOutputStream)
                    .thenCompose(ignored -> context.forwardResponse(header, response));
        });
    }

    @Override
    public CompletionStage<ResponseFilterResult> onShareFetchResponse(short apiVersion,
                                                                      ResponseHeaderData header,
                                                                      ShareFetchResponseData response,
                                                                      FilterContext context) {
        if (this.direction != Direction.OUT) {
            return context.forwardResponse(header, response);
        }
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
                    if (topic.equals(topicName)) {
                        for (var partitionData : topicData.partitions()) {
                            addWorkItem(workItems, topicName, partitionData.records(), partitionData::setRecords);
                        }
                    }
                }
            }
            return orchestrator.process(workItems, context::createByteBufferOutputStream)
                    .thenCompose(ignored -> context.forwardResponse(header, response));
        });
    }

    private void addWorkItem(List<PartitionWorkItem> workItems,
                             String topicName,
                             @Nullable BaseRecords records,
                             java.util.function.Consumer<MemoryRecords> writeBack) {
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
