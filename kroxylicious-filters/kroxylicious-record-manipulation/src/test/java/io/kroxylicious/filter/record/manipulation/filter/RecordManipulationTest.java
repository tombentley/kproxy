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
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.kroxylicious.filter.record.manipulation.filter.apicurio.ApicurioRegistryClientConfig;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.SchemaRegistryFactory;
import io.kroxylicious.filter.record.manipulation.filter.config.Direction;
import io.kroxylicious.filter.record.manipulation.filter.config.HeaderPredicate;
import io.kroxylicious.filter.record.manipulation.filter.config.Manipulation;
import io.kroxylicious.filter.record.manipulation.filter.config.NamedService;
import io.kroxylicious.filter.record.manipulation.filter.config.PrincipalMatch;
import io.kroxylicious.filter.record.manipulation.filter.config.RecordManipulationConfig;
import io.kroxylicious.filter.record.manipulation.filter.config.RecordThen;
import io.kroxylicious.filter.record.manipulation.filter.config.RecordWhen;
import io.kroxylicious.filter.record.manipulation.filter.config.SchemaRef;
import io.kroxylicious.filter.record.manipulation.filter.config.SubjectMatch;
import io.kroxylicious.filter.record.manipulation.filter.config.Then;
import io.kroxylicious.filter.record.manipulation.filter.config.TopicName;
import io.kroxylicious.filter.record.manipulation.filter.config.When;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
import io.kroxylicious.filter.record.manipulation.filter.transforms.EmptyBuffer;
import io.kroxylicious.filter.record.manipulation.filter.transforms.NullBuffer;
import io.kroxylicious.identity.Subject;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.kafka.common.header.internals.RecordHeader;
import io.kroxylicious.proxy.authentication.User;
import io.kroxylicious.proxy.filter.FilterDispatchExecutor;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;
import io.kroxylicious.proxy.plugin.UnknownPluginInstanceException;
import io.kroxylicious.transform.records.BatchEdit;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.TransformInitializationContext;

import edu.umd.cs.findbugs.annotations.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the compilation performed by {@link RecordManipulation#initialize}: turning each configured
 * {@link Manipulation} into a {@link CompiledManipulation} by compiling its {@code when} selectors (topic
 * matchers - including regexes - and header predicates) and wiring its {@code then} transforms through
 * their {@link BufferTransformService} factories, leaving an unconfigured transform {@code null} so the
 * engine keeps that field verbatim. Also covers the fail-fast rejection of configuration whose
 * records-engine support is not yet implemented.
 */
class RecordManipulationTest {

    private static final Header[] NO_HEADERS = new Header[0];
    private static final Then DEFAULT_THEN = new Then(null, null, null, null,
            new RecordThen(null, null, null, null));

    @Test
    void compilesOneManipulationPerConfigEntry() {
        // Given
        var config = config(
                manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null), DEFAULT_THEN),
                manipulation(when(Direction.OUT_OF_CLUSTER, TopicName.is("payments"), null), DEFAULT_THEN));

        // When
        var init = new RecordManipulation().initialize(new FakeFactoryContext(), config);

        // Then
        assertThat(init.compiledManipulations()).hasSize(2);
    }

    @Test
    void emptyThenCompilesToKeepTransforms() {
        // Given
        var config = config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null), DEFAULT_THEN));

        // When
        var init = new RecordManipulation().initialize(new FakeFactoryContext(), config);

        // Then
        var compiled = init.compiledManipulations().getFirst();
        assertThat(compiled.batchEdit()).isEqualTo(BatchEdit.keep());
        assertThat(compiled.newKey()).isNull();
        assertThat(compiled.newValue()).isNull();
        assertThat(compiled.newKeyService()).isNull();
        assertThat(compiled.newValueService()).isNull();
    }

    @Test
    void wiresConfiguredKeyAndValueTransformsThroughTheirFactories() {
        // Given
        var then = new Then(null, null, null, null, new RecordThen(EmptyBuffer.class.getName(), null, NullBuffer.class.getName(), null));
        var config = config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null), then));

        // When
        var init = new RecordManipulation().initialize(new FakeFactoryContext(), config);

        // Then
        var compiled = init.compiledManipulations().getFirst();
        assertThat(compiled.newKeyService()).isInstanceOf(EmptyBuffer.class);
        assertThat(compiled.newValueService()).isInstanceOf(NullBuffer.class);
        assertThat(compiled.newKey()).isNotNull();
        assertThat(compiled.newValue()).isNotNull();
    }

    @Test
    void compilesExactTopicMatcher() {
        // Given
        var config = config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().topicMatcher();

        // Then
        assertThat(matcher.test("orders")).isTrue();
        assertThat(matcher.test("orders-eu")).isFalse();
    }

    @Test
    void compilesTopicSetMatcher() {
        // Given
        var inTopic = TopicName.isIn(Set.of("orders", "payments"));
        var config = config(manipulation(when(Direction.INTO_CLUSTER, inTopic, null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().topicMatcher();

        // Then
        assertThat(matcher.test("orders")).isTrue();
        assertThat(matcher.test("payments")).isTrue();
        assertThat(matcher.test("audit")).isFalse();
    }

    @Test
    void compilesTopicRegexMatcher() {
        // Given
        var inTopic = TopicName.matches("orders-.*");
        var config = config(manipulation(when(Direction.INTO_CLUSTER, inTopic, null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().topicMatcher();

        // Then
        assertThat(matcher.test("orders-eu")).isTrue();
        assertThat(matcher.test("payments")).isFalse();
    }

    @Test
    void matchesAllTopicsWhenInTopicOmitted() {
        // Given
        var config = config(manipulation(when(Direction.INTO_CLUSTER, null, null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().topicMatcher();

        // Then
        assertThat(matcher.test("anything")).isTrue();
    }

    @Test
    void compilesExactPrincipalMatcher() {
        // Given
        var subjectMatch = new SubjectMatch(PrincipalMatch.is(User.class.getName(), "alice"));
        var config = config(manipulation(when(Direction.INTO_CLUSTER, null, subjectMatch), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().subjectGuard();

        // Then
        assertThat(matcher.test(new Subject(Set.of(new User("alice"))))).isTrue();
        assertThat(matcher.test(new Subject(Set.of(new User("eve"))))).isFalse();
    }

    @Test
    void compilesPrincipalSetMatcher() {
        // Given
        var subjectMatch = new SubjectMatch(PrincipalMatch.isIn(User.class.getName(), Set.of("alice", "bob")));
        var config = config(manipulation(when(Direction.INTO_CLUSTER, null, subjectMatch), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().subjectGuard();

        // Then
        assertThat(matcher.test(new Subject(Set.of(new User("alice"))))).isTrue();
        assertThat(matcher.test(new Subject(Set.of(new User("bob"))))).isTrue();
        assertThat(matcher.test(new Subject(Set.of(new User("eve"))))).isFalse();
    }

    @Test
    void compilesPrincipalRegexMatcher() {
        // Given
        var subjectMatch = new SubjectMatch(PrincipalMatch.matches(User.class.getName(), "(alice|bob)"));
        var config = config(manipulation(when(Direction.INTO_CLUSTER, null, subjectMatch), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().subjectGuard();

        // Then
        assertThat(matcher.test(new Subject(Set.of(new User("alice"))))).isTrue();
        assertThat(matcher.test(new Subject(Set.of(new User("bob"))))).isTrue();
        assertThat(matcher.test(new Subject(Set.of(new User("eve"))))).isFalse();
    }

    @Test
    void matchesAllSubjectsWhenInTopicOmitted() {
        // Given
        var config = config(manipulation(when(Direction.INTO_CLUSTER, null, null), DEFAULT_THEN));

        // When
        var matcher = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().subjectGuard();

        // Then
        assertThat(matcher.test(new Subject(Set.of(new User("alice"))))).isTrue();
        assertThat(matcher.test(new Subject(Set.of(new User("bob"))))).isTrue();
        assertThat(matcher.test(new Subject(Set.of(new User("eve"))))).isTrue();
    }

    @Test
    void compilesExactHeaderKeyPredicate() {
        // Given
        var hasAnyHeader = new HeaderPredicate("trace-id", null, null);
        var config = config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null),
                new Then(null, null, null, new RecordWhen(null, null, hasAnyHeader), DEFAULT_THEN.thenNewRecord())));

        // When
        var predicate = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().headerPredicate();

        // Then
        assertThat(predicate).isNotNull();
        assertThat(predicate.test(headers("trace-id"))).isTrue();
        assertThat(predicate.test(headers("other"))).isFalse();
    }

    @Test
    void compilesHeaderKeySetPredicate() {
        // Given
        var hasAnyHeader = new HeaderPredicate(null, null, Set.of("trace-id", "span-id"));
        var config = config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null),
                new Then(null, null, null, new RecordWhen(null, null, hasAnyHeader), DEFAULT_THEN.thenNewRecord())));

        // When
        var predicate = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().headerPredicate();

        // Then
        assertThat(predicate).isNotNull();
        assertThat(predicate.test(headers("span-id"))).isTrue();
        assertThat(predicate.test(headers("other"))).isFalse();
    }

    @Test
    void compilesHeaderKeyRegexPredicate() {
        // Given
        var hasAnyHeader = new HeaderPredicate(null, "trace-.*", null);
        var config = config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null),
                new Then(null, null, null, new RecordWhen(null, null, hasAnyHeader), DEFAULT_THEN.thenNewRecord())));

        // When
        var predicate = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst().headerPredicate();

        // Then
        assertThat(predicate).isNotNull();
        assertThat(predicate.test(headers("trace-id"))).isTrue();
        assertThat(predicate.test(headers("span-id"))).isFalse();
    }

    @Test
    void headerPredicateIsNullWhenNoHasAnyHeaderConfigured() {
        // Given
        var config = config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null), DEFAULT_THEN));

        // When
        var compiled = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst();

        // Then
        assertThat(compiled.headerPredicate()).isNull();
    }

    @Test
    void preservesDirection() {
        // Given
        var config = config(manipulation(when(Direction.OUT_OF_CLUSTER, TopicName.is("orders"), null), DEFAULT_THEN));

        // When
        var compiled = new RecordManipulation().initialize(new FakeFactoryContext(), config).compiledManipulations().getFirst();

        // Then
        assertThat(compiled.direction()).isEqualTo(Direction.OUT_OF_CLUSTER);
    }

    @Test
    void rejectsSchemaSelection() {
        // Given
        var keySchema = new SchemaRef("com.myco", "orders-key", "1");
        var config = config(new ApicurioRegistryClientConfig(URI.create("http://localhost:8888"), null, null),
                manipulation(when(Direction.OUT_OF_CLUSTER, TopicName.is("orders")),
                        new Then(null, null, null,
                                new RecordWhen(keySchema, null, null), DEFAULT_THEN.thenNewRecord())));

        // When / Then
        assertThatThrownBy(() -> new RecordManipulation().initialize(new FakeFactoryContext(), config))
                .isInstanceOf(PluginConfigurationException.class)
                .hasMessageContaining("withKeySchema");
    }

    @Test
    void closeClosesConfiguredKeyAndValueServicesWithTheirInitData() {
        // Given
        var keyService = new RecordingBufferTransformService();
        var valueService = new RecordingBufferTransformService();
        var then = new Then(null, null, null, null, new RecordThen("key", null, "value", null));
        var config = config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null), then));
        var context = new FakeFactoryContext()
                .with("key", keyService)
                .with("value", valueService);
        var factory = new RecordManipulation();
        var init = factory.initialize(context, config);

        // When
        factory.close(init);

        // Then
        assertThat(keyService.closedWith).containsExactly(keyService.initData);
        assertThat(valueService.closedWith).containsExactly(valueService.initData);
    }

    // @Test
    // void throwsIllegalArgumentExceptionWhenKeySchemaSelectionConfiguredWithoutSchemaRegistry() {
    // // Given
    // var keySchema = new SchemaRef("com.myco", "orders-key", "1");
    //
    // // When / Then
    // assertThatThrownBy(() -> config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null),
    // new Then(null, null, null, new RecordWhen(keySchema, null, null), DEFAULT_THEN.thenNewRecord()))))
    // .isInstanceOf(IllegalArgumentException.class)
    // .hasMessageContaining("schemaRegistry is required when a manipulation has a then.ifRecord.withValueSchema or when.ifRecord.withKeySchema");
    // }

    // @Test
    // void throwsIllegalArgumentExceptionWhenValueSchemaSelectionConfiguredWithoutSchemaRegistry() {
    // // Given
    // var valueSchema = new SchemaRef("com.myco", "orders-value", "1");
    //
    // // When / Then
    // assertThatThrownBy(() -> config(manipulation(when(Direction.INTO_CLUSTER, TopicName.is("orders"), null),
    // new Then(null, null, null, new RecordWhen(null, valueSchema, null), DEFAULT_THEN.thenNewRecord()))))
    // .isInstanceOf(IllegalArgumentException.class)
    // .hasMessageContaining("schemaRegistry is required when a manipulation has a then.ifRecord.withValueSchema or when.ifRecord.withKeySchema");
    // }

    // --- helpers ------------------------------------------------------------------------------

    private static RecordManipulationConfig config(Manipulation... manipulations) {
        return config(null, manipulations);
    }

    private static RecordManipulationConfig config(@Nullable ApicurioRegistryClientConfig registry, Manipulation... manipulations) {
        List<NamedService> namedServices;
        if (registry == null) {
            namedServices = List.of();
        }
        else {
            namedServices = List.of(
                    new NamedService("registry", SchemaRegistryFactory.class.getName(), registry));
        }
        return new RecordManipulationConfig(namedServices,
                List.of(),
                List.of(manipulations));
    }

    private static Manipulation manipulation(When when, Then then) {
        return new Manipulation(when, then);
    }

    private static When when(Direction direction,
                             @Nullable TopicName inTopic) {
        return when(direction, inTopic, null);
    }

    private static When when(Direction direction,
                             @Nullable TopicName inTopic,
                             @Nullable SubjectMatch subjectMatch) {
        return new When(direction, inTopic, subjectMatch);
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
            return Mockito.mock(i -> {
                throw new UnsupportedOperationException();
            });
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
        public Object configure(Object config, TransformInitializationContext context) {
            return initData;
        }

        @Override
        public BufferTransform create(Object initializationData) {
            return record -> CompletableFuture.completedStage(record.value());
        }

        @Override
        public void close(Object initializationData) {
            closedWith.add(initializationData);
        }
    }
}
