/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import io.kroxylicious.filter.record.manipulation.filter.config.Manipulation;
import io.kroxylicious.filter.record.manipulation.filter.config.NamedService;
import io.kroxylicious.filter.record.manipulation.filter.config.PrincipalMatch;
import io.kroxylicious.filter.record.manipulation.filter.config.RecordManipulationConfig;
import io.kroxylicious.filter.record.manipulation.filter.config.SubjectMatch;
import io.kroxylicious.filter.record.manipulation.filter.config.Then;
import io.kroxylicious.filter.record.manipulation.filter.config.TopicName;
import io.kroxylicious.filter.record.manipulation.filter.resolve.CompiledManipulation;
import io.kroxylicious.filter.record.manipulation.filter.service.ServiceFactory;
import io.kroxylicious.identity.Identity;
import io.kroxylicious.identity.Principal;
import io.kroxylicious.identity.Subject;
import io.kroxylicious.kafka.common.header.Header;
import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterDispatchExecutor;
import io.kroxylicious.proxy.filter.FilterFactory;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;
import io.kroxylicious.transform.records.BatchEdit;
import io.kroxylicious.transform.service.BufferTransform;
import io.kroxylicious.transform.service.BufferTransformService;
import io.kroxylicious.transform.service.DependencyKey;
import io.kroxylicious.transform.service.TransformInitializationContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import static org.slf4j.LoggerFactory.getLogger;

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

    private static final Logger LOGGER = getLogger(RecordManipulation.class);

    @SuppressWarnings({ "unchecked", "rawtypes", "removal" })
    @Override
    public Init initialize(FilterFactoryContext context,
                           RecordManipulationConfig config)
            throws PluginConfigurationException {

        // create the services
        Map<String, AutoCloseable> services = createServices(context, config);

        MyTransformInitializationContext context1 = new MyTransformInitializationContext(services, context);

        List<CompiledManipulation<?, ?>> compiledManipulations = new ArrayList<>(config.manipulations().size());
        for (var m : config.manipulations()) {
            rejectUnsupported(m);

            Predicate<String> topicMatcher = topicPredicate(m.when().inTopic());
            Predicate<Identity> subjectMatcher = subjectPredicate(m.when().clientSubject());
            Predicate<Header[]> headerPredicate = compileHeaderPredicate(m);

            var batchEditBuilder = BatchEdit.builder();
            if (m.then().newTimestamp() != null) {
                batchEditBuilder.setTimestamps(m.then().newTimestamp().toInstant().toEpochMilli());
            }
            if (m.then().shiftTimestampBy() != null) {
                batchEditBuilder.shiftTimestamps(m.then().shiftTimestampBy().toMillis());
            }
            if (m.then().compressionType() != null) {
                batchEditBuilder.compression(m.then().compressionType());
            }

            BufferTransformService keyService = null;
            Object keyInitData = null;
            BufferTransform keyTransform = null;
            if (m.then().thenNewRecord().newKey() != null) {
                keyService = context.pluginInstance(BufferTransformService.class, m.then().thenNewRecord().newKey());
                keyInitData = keyService.configure(m.then().thenNewRecord().newKeyConfig(), context1);
                keyTransform = keyService.create(keyInitData);
            }

            BufferTransformService valueService = null;
            Object valueInitData = null;
            BufferTransform valueTransform = null;
            if (m.then().thenNewRecord().newValue() != null) {
                valueService = context.pluginInstance(BufferTransformService.class, m.then().thenNewRecord().newValue());
                valueInitData = valueService.configure(m.then().thenNewRecord().newValueConfig(), context1);
                valueTransform = valueService.create(valueInitData);
            }

            compiledManipulations.add(new CompiledManipulation<>(
                    m.when().direction(),
                    subjectMatcher, topicMatcher,
                    headerPredicate,
                    null, // keySchema - schema selection rejected above
                    null, // valueSchema - schema selection rejected above
                    batchEditBuilder.build(),
                    keyService,
                    keyInitData,
                    keyTransform,
                    valueService,
                    valueInitData,
                    valueTransform));
        }

        Executor filterDispatchExecutor = context.filterDispatchExecutor();
        return new Init(compiledManipulations, services, filterDispatchExecutor != null ? filterDispatchExecutor : Runnable::run);
    }

    @NonNull
    private Map<String, AutoCloseable> createServices(FilterFactoryContext context, RecordManipulationConfig config) {
        List<Map.Entry<String, List<NamedService>>> dupes = config.services().stream().collect(Collectors.groupingBy(NamedService::name))
                .entrySet().stream().collect(Collectors.partitioningBy(e -> e.getValue().size() >= 2))
                .get(true);
        if (!dupes.isEmpty()) {
            throw new PluginConfigurationException("Duplicate service names: " + dupes.stream().map(Map.Entry::getKey).collect(Collectors.joining(", ")));
        }
        Map<String, AutoCloseable> services = HashMap.newHashMap(config.services().size());
        try {
            for (var service : config.services()) {
                ServiceFactory serviceFactory = context.pluginInstance(ServiceFactory.class, service.type());
                AutoCloseable autoCloseable = serviceFactory.create(service.config());
                services.put(service.name(), autoCloseable);
            }
        }
        catch (RuntimeException e) {
            closeServices(services);
            throw e;
        }
        return services;
    }

    private void closeServices(Map<String, AutoCloseable> services) {
        for (var entry : services.entrySet()) {
            try {
                entry.getValue().close();
            }
            catch (Exception exception) {
                LOGGER.atError()
                        .addKeyValue("service", entry.getKey())
                        .setCause(exception)
                        .log("Error closing service " + entry.getKey());
            }
        }
    }

    /**
     * Rejects configuration whose records-engine support is not yet implemented, so it fails at startup rather
     * than being silently ignored at runtime.
     */
    private static void rejectUnsupported(Manipulation m) throws PluginConfigurationException {
        if (m.then().ifRecord() != null && (m.then().ifRecord().withKeySchema() != null || m.then().ifRecord().withValueSchema() != null)) {
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
        if (m.then().ifRecord() != null && m.then().ifRecord().hasAnyHeader() != null) {
            var predicate = m.then().ifRecord().hasAnyHeader();
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

    private static Predicate<String> topicPredicate(@Nullable TopicName inTopic) {
        Predicate<String> topicMatcher;
        if (inTopic != null) {
            if (inTopic.withName() != null) {
                topicMatcher = topicName -> inTopic.withName().equals(topicName);
            }
            else if (inTopic.withNameIn() != null) {
                topicMatcher = topicName -> inTopic.withNameIn().contains(topicName);
            }
            else if (inTopic.withNameMatching() != null) {
                Pattern pattern = Pattern.compile(inTopic.withNameMatching());
                topicMatcher = topicName -> pattern.matcher(topicName).matches();
            }
            else {
                // TopicName constructor should make this branch impossible
                throw new IllegalStateException("Invalid when.inTopic " + inTopic);
            }
        }
        else {
            topicMatcher = topicName -> true;
        }
        return topicMatcher;
    }

    @SuppressWarnings("removal")
    private static Predicate<Identity> subjectPredicate(@Nullable SubjectMatch subjectMatch) {
        Predicate<Identity> subjectPredicate;
        if (subjectMatch != null) {
            PrincipalMatch principalMatch = subjectMatch.containsPrincipal();
            if (principalMatch != null) {
                subjectPredicate = subject -> subject.principals().stream().anyMatch(principalPredicate(principalMatch));
            }
            else {
                throw new IllegalStateException("Invalid subject match " + subjectMatch);
            }
        }
        else {
            subjectPredicate = subject -> true;
        }
        return subjectPredicate;
    }

    @NonNull
    private static Predicate<Principal> principalPredicate(PrincipalMatch principalMatch) {
        Predicate<Subject> subjectPredicate;
        Predicate<Principal> principalPredicate;
        if (principalMatch.withName() != null) {
            principalPredicate = principal -> principalMatch.withName().equals(principal.name());
        }
        else if (principalMatch.withNameIn() != null) {
            principalPredicate = principal -> principalMatch.withNameIn().contains(principal.name());
        }
        else if (principalMatch.withNameMatching() != null) {
            Pattern pattern = Pattern.compile(principalMatch.withNameMatching());
            principalPredicate = principal -> pattern.matcher(principal.name()).matches();
        }
        else {
            throw new IllegalStateException("Invalid containsPrincipal " + principalMatch);
        }
        return principal -> principal.getClass().getName().equals(principalMatch.withType())
                && principalPredicate.test(principal);
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
        return new RecordManipulationFilter(initializationData);
    }

    private static class MyTransformInitializationContext implements TransformInitializationContext {
        private final Map<String, AutoCloseable> services;
        private final FilterFactoryContext context;

        MyTransformInitializationContext(Map<String, AutoCloseable> services, FilterFactoryContext context) {
            this.services = services;
            this.context = context;
        }

        @Override
        public <S> S serviceClient(Class<S> serviceClass, String instanceName) {
            AutoCloseable service = services.get(instanceName);
            if (service == null) {
                var typeCompatibleServices = services.entrySet().stream()
                        .filter(entry -> serviceClass.isInstance(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .sorted()
                        .collect(Collectors.joining(", "));
                String message = "No service with name " + instanceName + " has been defined";
                if (typeCompatibleServices.isEmpty()) {
                    message += ", and there are no services compatible with " + serviceClass;
                }
                else {
                    message += ". The follow services are compatible with " + serviceClass + ": " + typeCompatibleServices;
                }
                throw new IllegalStateException(message);
            }
            else if (!serviceClass.isInstance(service)) {
                throw new IllegalStateException("Service with name " + instanceName + " is not an instance of " + serviceClass.getName()
                        + " (is an instance of " + service.getClass().getName() + ")");
            }
            return serviceClass.cast(service);
        }

        @Override
        public <P> P pluginInstance(Class<P> pluginClass, String implementationName) {
            return context.pluginInstance(pluginClass, implementationName);
        }
    }
}
