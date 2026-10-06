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
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import io.kroxylicious.filter.record.manipulation.filter.apicurio.ApicurioSchemaRegistryClient;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.SchemaFacets;
import io.kroxylicious.filter.record.manipulation.filter.config.Manipulation;
import io.kroxylicious.filter.record.manipulation.filter.config.RecordManipulationConfig;
import io.kroxylicious.filter.record.manipulation.filter.config.Then;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CachingEntityResolver;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.DependencyKey;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * The FilterFactory (service) for the {@link RecordManipulationFilter}.
 *
 * <p>Compiles each configured {@link Manipulation} into a {@link CompiledManipulation}: its {@code when}
 * selectors (topic matcher, synchronous header predicate) and its {@code then} key/value transforms. A
 * transform that is not configured is left {@code null} so the engine copies that field verbatim.</p>
 *
 * <p>Features whose records-engine support is not yet implemented are rejected here (fail-fast) rather than
 * silently ignored: schema-based selection, per-record timestamp transforms, and transforms that declare
 * {@link DependencyKey dependencies}.</p>
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
            rejectUnsupported(m);

            Predicate<String> topicMatcher = compileTopicMatcher(m);
            Predicate<Header[]> headerPredicate = compileHeaderPredicate(m);

            BufferTransformService keyService = null;
            Object keyInitData = null;
            BufferTransform keyTransform = null;
            if (m.then().newKey() != null) {
                keyService = context.pluginInstance(BufferTransformService.class, m.then().newKey());
                keyInitData = keyService.configure(context::pluginInstance, m.then().newKeyConfig());
                requireNoDependencies(keyService.dependencies(keyInitData), "newKey", m.then().newKey());
                keyTransform = keyService.create(keyInitData);
            }

            BufferTransformService valueService = null;
            Object valueInitData = null;
            BufferTransform valueTransform = null;
            if (m.then().newValue() != null) {
                valueService = context.pluginInstance(BufferTransformService.class, m.then().newValue());
                valueInitData = valueService.configure(context::pluginInstance, m.then().newValueConfig());
                requireNoDependencies(valueService.dependencies(valueInitData), "newValue", m.then().newValue());
                valueTransform = valueService.create(valueInitData);
            }

            compiledManipulations.add(new CompiledManipulation<>(
                    m.when().direction(),
                    topicMatcher,
                    headerPredicate,
                    null, // keySchema - schema selection rejected above
                    null, // valueSchema - schema selection rejected above
                    null, // newTimestampService - timestamp transforms rejected above
                    null, // newTimestampInitData
                    null, // newTimestamp
                    keyService,
                    keyInitData,
                    keyTransform,
                    valueService,
                    valueInitData,
                    valueTransform));
        }

        SchemaFacets schemaFacets = null;
        if (config.schemaRegistry() != null) {
            var client = new ApicurioSchemaRegistryClient(config.schemaRegistry().url(), Duration.ofSeconds(20), config.schemaRegistry().tls());
            schemaFacets = new SchemaFacets(client, CachingEntityResolver.UNBOUNDED, CachingEntityResolver.UNBOUNDED);
        }

        return new Init(compiledManipulations, schemaFacets);
    }

    /**
     * Rejects configuration whose records-engine support is not yet implemented, so it fails at startup rather
     * than being silently ignored at runtime.
     */
    private static void rejectUnsupported(Manipulation m) throws PluginConfigurationException {
        if (m.when().withKeySchema() != null || m.when().withValueSchema() != null) {
            throw new PluginConfigurationException(
                    "when.withKeySchema/withValueSchema (schema-based selection) is not yet supported by the records transform engine");
        }
        Then then = m.then();
        if (then.newTimestamp() != null) {
            throw new PluginConfigurationException(
                    "then.newTimestamp is not yet supported by the records transform engine: per-record timestamps cannot be "
                            + "expressed (timestamps are coupled to exactly-once semantics); batch-level timestamp edits are a planned follow-up");
        }
    }

    private static void requireNoDependencies(Set<DependencyKey<?, ?>> dependencies, String field, String implementationName)
            throws PluginConfigurationException {
        if (!dependencies.isEmpty()) {
            throw new PluginConfigurationException(
                    "then." + field + " transform '" + implementationName + "' declares dependencies, which are not yet supported "
                            + "by the records transform engine");
        }
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
