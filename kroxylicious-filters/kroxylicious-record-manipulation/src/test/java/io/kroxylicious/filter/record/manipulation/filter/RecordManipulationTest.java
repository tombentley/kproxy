/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.kroxylicious.filter.record.manipulation.filter.config.Direction;
import io.kroxylicious.filter.record.manipulation.filter.config.HeaderPredicate;
import io.kroxylicious.filter.record.manipulation.filter.config.Manipulation;
import io.kroxylicious.filter.record.manipulation.filter.config.RecordManipulationConfig;
import io.kroxylicious.filter.record.manipulation.filter.config.SchemaRef;
import io.kroxylicious.filter.record.manipulation.filter.config.SchemaRegistry;
import io.kroxylicious.filter.record.manipulation.filter.config.Then;
import io.kroxylicious.filter.record.manipulation.filter.config.TopicName;
import io.kroxylicious.filter.record.manipulation.filter.config.When;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
import io.kroxylicious.filter.record.manipulation.filter.transforms.EmptyBuffer;
import io.kroxylicious.filter.record.manipulation.filter.transforms.NullBuffer;
import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordKey;
import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordTimestamp;
import io.kroxylicious.filter.record.manipulation.filter.transforms.RecordValue;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;
import io.kroxylicious.kafka.common.record.internal.Record;
import io.kroxylicious.proxy.filter.FilterDispatchExecutor;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.UnknownPluginInstanceException;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.PluginLookup;
import io.kroxylicious.transform.service.TimestampTransform;
import io.kroxylicious.transform.service.TimestampTransformService;

import edu.umd.cs.findbugs.annotations.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the compilation performed by {@link RecordManipulation#initialize}: turning each configured
 * {@link Manipulation} into a {@link CompiledManipulation} by compiling its {@code when} selectors (topic
 * matchers - including regexes - and header predicates) and wiring its {@code then} transforms through
 * their {@link BufferTransformService}/{@link TimestampTransformService} factories.
 */
class RecordManipulationTest {

    private static final Header[] NO_HEADERS = new Header[0];
    private static final Then DEFAULT_THEN = new Then(null, null, null, null, null, null);

    @Test
    void compilesOneManipulationPerConfigEntry() {
        // Given
        var config = config(
                manipulation(when(Direction.IN, topic("orders"), null, null, null), DEFAULT_THEN),
                manipulation(when(Direction.OUT, topic("payments"), null, null, null), DEFAULT_THEN));

        // When
        var init = new RecordManipulation().initialize(new FakeFactoryContext(), config);

        // Then
        assertThat(init.compiledManipulations()).hasSize(2);
    }

    @Test
    void defaultsTimestampKeyAndValueTransformsWhenThenClauseIsEmpty() {
        // Given
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, null), DEFAULT_THEN));

        // When
        var init = new RecordManipulation().initialize(new FakeFactoryContext(), config);

        // Then
        var compiled = init.compiledManipulations().get(0);
        assertThat(compiled.newTimestampService()).isInstanceOf(RecordTimestamp.class);
        assertThat(compiled.newKeyService()).isInstanceOf(RecordKey.class);
        assertThat(compiled.newValueService()).isInstanceOf(RecordValue.class);
        assertThat(compiled.newTimestamp()).isNotNull();
        assertThat(compiled.newKey()).isNotNull();
        assertThat(compiled.newValue()).isNotNull();
    }

    @Test
    void wiresConfiguredKeyAndValueTransformsThroughTheirFactories() {
        // Given
        var then = new Then(null, null, EmptyBuffer.class.getName(), null, NullBuffer.class.getName(), null);
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, null), then));

        // When
        var init = new RecordManipulation().initialize(new FakeFactoryContext(), config);

        // Then
        var compiled = init.compiledManipulations().get(0);
        assertThat(compiled.newKeyService()).isInstanceOf(EmptyBuffer.class);
        assertThat(compiled.newValueService()).isInstanceOf(NullBuffer.class);
    }

    @Test
    void compilesExactTopicMatcher() {
        // Given
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0).topicMatcher();

        // Then
        assertThat(matcher.test("orders")).isTrue();
        assertThat(matcher.test("orders-eu")).isFalse();
    }

    @Test
    void compilesTopicSetMatcher() {
        // Given
        var inTopic = new TopicName(null, Set.of("orders", "payments"), null);
        var config = config(manipulation(when(Direction.IN, inTopic, null, null, null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0).topicMatcher();

        // Then
        assertThat(matcher.test("orders")).isTrue();
        assertThat(matcher.test("payments")).isTrue();
        assertThat(matcher.test("audit")).isFalse();
    }

    @Test
    void compilesTopicRegexMatcher() {
        // Given
        var inTopic = new TopicName(null, null, "orders-.*");
        var config = config(manipulation(when(Direction.IN, inTopic, null, null, null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0).topicMatcher();

        // Then
        assertThat(matcher.test("orders-eu")).isTrue();
        assertThat(matcher.test("payments")).isFalse();
    }

    @Test
    void matchesAllTopicsWhenInTopicOmitted() {
        // Given
        var config = config(manipulation(when(Direction.IN, null, null, null, null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0).topicMatcher();

        // Then
        assertThat(matcher.test("anything")).isTrue();
    }

    @Test
    void compilesExactHeaderKeyPredicate() {
        // Given
        var hasAnyHeader = new HeaderPredicate("trace-id", null, null);
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, hasAnyHeader), DEFAULT_THEN));

        // When
        var predicate = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0).headerPredicate();

        // Then
        assertThat(predicate).isNotNull();
        assertThat(predicate.test(headers("trace-id"))).isTrue();
        assertThat(predicate.test(headers("other"))).isFalse();
    }

    @Test
    void compilesHeaderKeySetPredicate() {
        // Given
        var hasAnyHeader = new HeaderPredicate(null, null, Set.of("trace-id", "span-id"));
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, hasAnyHeader), DEFAULT_THEN));

        // When
        var predicate = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0).headerPredicate();

        // Then
        assertThat(predicate).isNotNull();
        assertThat(predicate.test(headers("span-id"))).isTrue();
        assertThat(predicate.test(headers("other"))).isFalse();
    }

    @Test
    void compilesHeaderKeyRegexPredicate() {
        // Given
        var hasAnyHeader = new HeaderPredicate(null, "trace-.*", null);
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, hasAnyHeader), DEFAULT_THEN));

        // When
        var predicate = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0).headerPredicate();

        // Then
        assertThat(predicate).isNotNull();
        assertThat(predicate.test(headers("trace-id"))).isTrue();
        assertThat(predicate.test(headers("span-id"))).isFalse();
    }

    @Test
    void headerPredicateIsNullWhenNoHasAnyHeaderConfigured() {
        // Given
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, null), DEFAULT_THEN));

        // When
        var compiled = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0);

        // Then
        assertThat(compiled.headerPredicate()).isNull();
    }

    @Test
    void preservesDirectionAndSchemaRefs() {
        // Given
        var keySchema = new SchemaRef("com.myco", "orders-key", "1");
        var valueSchema = new SchemaRef("com.myco", "orders-value", "*");
        var config = config(new SchemaRegistry(URI.create("http://localhost:8888"), null),
                manipulation(when(Direction.OUT, topic("orders"), keySchema, valueSchema, null), DEFAULT_THEN));

        // When
        var compiled = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().get(0);

        // Then
        assertThat(compiled.direction()).isEqualTo(Direction.OUT);
        assertThat(compiled.keySchema()).isEqualTo(keySchema);
        assertThat(compiled.valueSchema()).isEqualTo(valueSchema);
    }

    @Test
    void closeClosesEachManipulationsTransformServicesWithTheirInitData() {
        // Given
        var timestampService = new RecordingTimestampTransformService();
        var keyService = new RecordingBufferTransformService();
        var valueService = new RecordingBufferTransformService();
        var then = new Then("ts", null, "key", null, "value", null);
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, null), then));
        var context = new FakeFactoryContext()
                .with("ts", timestampService)
                .with("key", keyService)
                .with("value", valueService);
        var factory = new RecordManipulation();
        var init = factory.initialize(context, config);

        // When
        factory.close(init);

        // Then
        assertThat(timestampService.closedWith).containsExactly(timestampService.initData);
        assertThat(keyService.closedWith).containsExactly(keyService.initData);
        assertThat(valueService.closedWith).containsExactly(valueService.initData);
    }

    @Test
    void buildsNonNullSchemaFacetsWhenSchemaRegistryConfigured() {
        // Given
        var schemaRegistry = new SchemaRegistry(URI.create("http://registry.local:8080"), null);
        var config = new RecordManipulationConfig(schemaRegistry, List.of(manipulation(when(Direction.IN, topic("orders"), null, null, null), DEFAULT_THEN)));

        // When
        var init = new RecordManipulation().initialize(new FakeFactoryContext(), config);

        // Then
        assertThat(init.schemaFacets()).isNotNull();
    }

    @Test
    void schemaFacetsIsNullWhenSchemaRegistryAbsentAndNoManipulationNeedsSchema() {
        // Given
        var config = config(manipulation(when(Direction.IN, topic("orders"), null, null, null), DEFAULT_THEN));

        // When
        var init = new RecordManipulation().initialize(new FakeFactoryContext(), config);

        // Then
        assertThat(init.schemaFacets()).isNull();
    }

    @Test
    void throwsIllegalArgumentExceptionWhenKeySchemaSelectionConfiguredWithoutSchemaRegistry() {
        // Given
        var keySchema = new SchemaRef("com.myco", "orders-key", "1");

        // When / Then
        assertThatThrownBy(() -> config(manipulation(when(Direction.IN, topic("orders"), keySchema, null, null), DEFAULT_THEN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schemaRegistry is required when a manipulation has a when.withValueSchema or when.withKeySchema");
    }

    @Test
    void throwsIllegalArgumentExceptionWhenValueSchemaSelectionConfiguredWithoutSchemaRegistry() {
        // Given
        var valueSchema = new SchemaRef("com.myco", "orders-value", "1");

        // When / Then
        assertThatThrownBy(() -> config(manipulation(when(Direction.IN, topic("orders"), null, valueSchema, null), DEFAULT_THEN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schemaRegistry is required when a manipulation has a when.withValueSchema or when.withKeySchema");
    }

    // --- helpers ------------------------------------------------------------------------------

    private static RecordManipulationConfig config(Manipulation... manipulations) {
        return config(null, manipulations);
    }

    private static RecordManipulationConfig config(@Nullable SchemaRegistry registry, Manipulation... manipulations) {
        return new RecordManipulationConfig(registry, List.of(manipulations));
    }

    private static Manipulation manipulation(When when, Then then) {
        return new Manipulation(when, then);
    }

    private static When when(Direction direction, TopicName inTopic, SchemaRef keySchema, SchemaRef valueSchema, HeaderPredicate hasAnyHeader) {
        return new When(direction, inTopic, keySchema, valueSchema, hasAnyHeader);
    }

    private static TopicName topic(String name) {
        return new TopicName(name, null, null);
    }

    private static Header[] headers(String... keys) {
        List<Header> headers = new ArrayList<>();
        for (String key : keys) {
            headers.add(new RecordHeader(key, null));
        }
        return headers.toArray(NO_HEADERS);
    }

    /**
     * A {@link FilterFactoryContext} that resolves plugin instances either from an explicit registration or, failing
     * that, by reflectively instantiating the named class (sufficient for the module's no-arg transform plugins).
     */
    private static final class FakeFactoryContext implements FilterFactoryContext {
        private final Map<String, Object> plugins = new java.util.HashMap<>();

        FakeFactoryContext with(String implementationName, Object instance) {
            plugins.put(implementationName, instance);
            return this;
        }

        @Override
        public <P> P pluginInstance(Class<P> pluginClass, String implementationName) {
            return pluginClass.cast(plugins.computeIfAbsent(implementationName, FakeFactoryContext::instantiate));
        }

        private static Object instantiate(String className) {
            try {
                return Class.forName(className).getDeclaredConstructor().newInstance();
            }
            catch (ReflectiveOperationException e) {
                throw new UnknownPluginInstanceException(className, e);
            }
        }

        @Override
        public FilterDispatchExecutor filterDispatchExecutor() {
            throw new UnsupportedOperationException("not used by initialize()");
        }

        @Override
        public <P> Set<String> pluginImplementationNames(Class<P> pluginClass) {
            throw new UnsupportedOperationException("not used by initialize()");
        }
    }

    private static final class RecordingBufferTransformService implements BufferTransformService<Object, Object> {
        private final Object initData = new Object();
        private final List<Object> closedWith = new ArrayList<>();

        @Override
        public Object configure(PluginLookup pluginLookup, Object config) {
            return initData;
        }

        @Override
        public BufferTransform create(Object initializationData) {
            return Record::value;
        }

        @Override
        public void close(Object initializationData) {
            closedWith.add(initializationData);
        }
    }

    private static final class RecordingTimestampTransformService implements TimestampTransformService<Object, Object> {
        private final Object initData = new Object();
        private final List<Object> closedWith = new ArrayList<>();

        @Override
        public Object configure(PluginLookup pluginLookup, Object config) {
            return initData;
        }

        @Override
        public TimestampTransform create(Object initializationData) {
            return Record::timestamp;
        }

        @Override
        public void close(Object initializationData) {
            closedWith.add(initializationData);
        }
    }
}
