/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import io.kroxylicious.filter.record.manipulation.filter.apicurio.ApicurioSchemaRegistryClient;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.SchemaFacets;
import io.kroxylicious.filter.record.manipulation.filter.config.Manipulation;
import io.kroxylicious.filter.record.manipulation.filter.config.RecordManipulationConfig;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CachingEntityResolver;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
import io.kroxylicious.filter.record.manipulation.filter.resolve.ContextualBufferTransform;
import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordKey;
import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordTimestamp;
import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordValue;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.TimestampTransformService;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The FilterFactory (service) for the {@link RecordManipulationFilter}.
 */
@Plugin(configType = RecordManipulationConfig.class)
public class RecordManipulation implements FilterFactory<RecordManipulationConfig, Init> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Init initialize(FilterFactoryContext context,
                           RecordManipulationConfig config)
            throws PluginConfigurationException {

        List<CompiledManipulation<?, ?, ?>> compiledManipulations = new ArrayList<>(config.manipulations().size());
        for (var m : config.manipulations()) {

            Predicate<String> topicMatcher = compileTopicMatcher(m);
            Predicate<Header[]> headerPredicate = compileHeaderPredicate(m);

            TimestampTransformService timestampTransformService = context.pluginInstance(TimestampTransformService.class,
                    Optional.ofNullable(m.then().newTimestamp()).orElse(RecordTimestamp.class.getName()));
            var timestampInitData = timestampTransformService
                    .configure(context::pluginInstance, m.then().newTimestampConfig());
            var timestampPipeline = timestampTransformService.create(timestampInitData);

            BufferTransformService keyTransformService = context.pluginInstance(BufferTransformService.class,
                    Optional.ofNullable(m.then().newKey()).orElse(RecordKey.class.getName()));
            var keyInitData = keyTransformService
                    .configure(context::pluginInstance, m.then().newKeyConfig());
            var keyPipeline = keyTransformService.create(keyInitData);

            BufferTransformService valueTransformService = context.pluginInstance(BufferTransformService.class,
                    Optional.ofNullable(m.then().newValue()).orElse(RecordValue.class.getName()));
            var valueInitData = valueTransformService
                    .configure(context::pluginInstance, m.then().newValueConfig());
            var valuePipeline = valueTransformService.create(valueInitData);

            compiledManipulations.add(new CompiledManipulation<>(
                    m.when().direction(),
                    topicMatcher,
                    headerPredicate,
                    m.when().withKeySchema(),
                    m.when().withValueSchema(),
                    timestampTransformService,
                    timestampInitData,
                    timestampPipeline,
                    keyTransformService,
                    keyInitData,
                    ContextualBufferTransform.of(keyPipeline),
                    valueTransformService,
                    valueInitData,
                    ContextualBufferTransform.of(valuePipeline)));
        }

        boolean needsSchema = compiledManipulations.stream().anyMatch(cm -> cm.needsKeySchema() || cm.needsValueSchema());
        if (needsSchema && config.schemaRegistry() == null) {
            throw new PluginConfigurationException(
                    "At least one manipulation uses withKeySchema/withValueSchema, but no schemaRegistry is configured.");
        }
        SchemaFacets schemaFacets = null;
        if (config.schemaRegistry() != null) {
            var client = new ApicurioSchemaRegistryClient(config.schemaRegistry().url(), Duration.ofSeconds(20), config.schemaRegistry().tls());
            schemaFacets = new SchemaFacets(client, CachingEntityResolver.UNBOUNDED, CachingEntityResolver.UNBOUNDED);
        }

        return new Init(compiledManipulations, schemaFacets);
    }

    @Nullable
    private static Predicate<Header[]> compileHeaderPredicate(Manipulation m) {
        Predicate<Header[]> headerPredicate;
        if (m.when().hasAnyHeader() != null) {
            var predicate = m.when().hasAnyHeader();
            if (predicate.withKey() != null) {
                String keyToMatch = predicate.withKey();
                headerPredicate = headers -> Arrays.stream(headers).anyMatch(header -> keyToMatch.equals(header.key()));
            }
            else if (predicate.withKeyIn() != null) {
                var set = predicate.withKeyIn();
                headerPredicate = headers -> Arrays.stream(headers).anyMatch(header -> set.contains(header.key()));
            }
            else if (predicate.withKeyMatching() != null) {
                Pattern pattern = Pattern.compile(predicate.withKeyMatching());
                headerPredicate = headers -> Arrays.stream(headers).anyMatch(header -> pattern.matcher(header.key()).matches());
            }
            else {
                // TopicName constructor should make this branch impossible
                throw new IllegalStateException("Invalid when.hasAnyHeader " + m.when().inTopic());
            }
        }
        else {
            headerPredicate = null;
        }
        return headerPredicate;
    }

    private static Predicate<String> compileTopicMatcher(Manipulation m) {
        Predicate<String> topicMatcher;
        if (m.when().inTopic() != null) {
            if (m.when().inTopic().is() != null) {
                topicMatcher = topicName -> m.when().inTopic().is().equals(topicName);
            }
            else if (m.when().inTopic().isIn() != null) {
                topicMatcher = topicName -> m.when().inTopic().isIn().contains(topicName);
            }
            else if (m.when().inTopic().matches() != null) {
                Pattern pattern = Pattern.compile(m.when().inTopic().matches());
                topicMatcher = topicName -> pattern.matcher(topicName).matches();
            }
            else {
                // TopicName constructor should make this branch impossible
                throw new IllegalStateException("Invalid when.inTopic " + m.when().inTopic());
            }
        }
        else {
            topicMatcher = topicName -> true;
        }
        return topicMatcher;
    }

    @Override
    public void close(Init initializationData) {
        Exception firstException = null;
        for (var cm : initializationData.compiledManipulations()) {
            try {
                cm.close();
            }
            catch (Exception e) {
                if (firstException == null) {
                    firstException = e;
                }
                else {
                    firstException.addSuppressed(e);
                }
            }
        }
    }

    @Override
    public Filter createFilter(FilterFactoryContext context,
                               Init initializationData) {
        return new RecordManipulationFilter(initializationData, context.filterDispatchExecutor());
    }
}
