/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.kroxylicious.it;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.nettyplus.leakdetector.junit.NettyLeakDetectorExtension;

import io.kroxylicious.filter.sasl.inspection.SaslInspection;
import io.kroxylicious.proxy.config.ClusterDefinition;
import io.kroxylicious.proxy.config.ConfigurationBuilder;
import io.kroxylicious.proxy.config.NamedRange;
import io.kroxylicious.proxy.config.RouteDefinition;
import io.kroxylicious.proxy.config.RouteTarget;
import io.kroxylicious.proxy.config.RouterDefinition;
import io.kroxylicious.proxy.config.VirtualClusterBuilder;
import io.kroxylicious.proxy.router.topic.TopicPartitionRouterFactory;
import io.kroxylicious.proxy.router.topic.config.RouteConfig;
import io.kroxylicious.proxy.router.topic.config.TopicPartitionRouterConfig;
import io.kroxylicious.testing.integration.config.NamedFilterDefinitionBuilder;
import io.kroxylicious.testing.kafka.api.KafkaCluster;
import io.kroxylicious.testing.kafka.api.TerminationStyle;
import io.kroxylicious.testing.kafka.common.BrokerCluster;
import io.kroxylicious.testing.kafka.common.BrokerConfig;
import io.kroxylicious.testing.kafka.common.SaslMechanism;
import io.kroxylicious.testing.kafka.junit5ext.KafkaClusterExtension;

import static io.kroxylicious.testing.integration.tester.KroxyliciousConfigUtils.baseConfigurationBuilder;
import static io.kroxylicious.testing.integration.tester.KroxyliciousConfigUtils.defaultPortIdentifiesNodeGatewayBuilder;
import static io.kroxylicious.testing.integration.tester.KroxyliciousTesters.kroxyliciousTester;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that proxied transactions and consumer groups survive a
 * coordinator change triggered by stopping the broker that leads the
 * relevant {@code __transaction_state} or {@code __consumer_offsets}
 * partition.
 */
@ExtendWith(KafkaClusterExtension.class)
@ExtendWith(NettyLeakDetectorExtension.class)
class CoordinatorChangeResilienceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoordinatorChangeResilienceIT.class);

    private static final String SASL_USERNAME = "alice";
    private static final String SASL_PASSWORD = "alice-secret";

    private RoutingEventCaptor routingCaptor;

    @BeforeEach
    void setUp() {
        routingCaptor = RoutingEventCaptor.install();
    }

    @AfterEach
    void tearDown() {
        if (routingCaptor != null) {
            routingCaptor.close();
        }
    }

    // --- Transaction coordinator change ---

    @Test
    void shouldCommitTransactionAfterCoordinatorChange(
                                                       @SaslMechanism(value = "PLAIN", principals = {
                                                               @SaslMechanism.Principal(user = SASL_USERNAME, password = SASL_PASSWORD)
                                                       }) @BrokerConfig(name = "transaction.state.log.replication.factor", value = "3") @BrokerConfig(name = "transaction.state.log.min.isr", value = "2") @BrokerConfig(name = "offsets.topic.replication.factor", value = "3") @BrokerCluster(numBrokers = 3) KafkaCluster saslClusterA,
                                                       @BrokerCluster(numBrokers = 3) KafkaCluster clusterB)
            throws Exception {
        String topic = "a.txn-coord-test";
        createTopic(saslClusterA, topic, 1);

        var config = buildSaslConfig(saslClusterA, clusterB, Map.of(SASL_USERNAME, "route-a"));

        try (var tester = kroxyliciousTester(config)) {
            String bootstrap = tester.getBootstrapAddress();

            // Warm up __transaction_state by doing a transaction, then find a
            // transactionalId whose coordinator is on a non-controller broker.
            String warmupTxnId = "txn-warmup";
            try (var producer = new KafkaProducer<String, String>(saslProducerConfig(bootstrap, warmupTxnId))) {
                producer.initTransactions();
                producer.beginTransaction();
                producer.commitTransaction();
            }
            String transactionalId = findKeyOnNonControllerBroker(saslClusterA, "__transaction_state", "txn-coord-resilience-");
            var producerConfig = saslProducerConfig(bootstrap, transactionalId);

            // Given: commit a transaction successfully before the coordinator change
            try (var producer = new KafkaProducer<String, String>(producerConfig)) {
                producer.initTransactions();
                producer.beginTransaction();
                producer.send(new ProducerRecord<>(topic, "key-before", "val-before")).get(10, TimeUnit.SECONDS);
                producer.commitTransaction();
            }

            var recordsBefore = consumeDirectly(saslClusterA, topic);
            assertThat(recordsBefore).extracting(ConsumerRecord::value).contains("val-before");

            // When: stop the transaction coordinator broker
            int coordinatorNodeId = findCoordinatorBroker(saslClusterA, "__transaction_state", transactionalId);
            LOGGER.info("Stopping transaction coordinator node {} for transactionalId '{}'", coordinatorNodeId, transactionalId);
            saslClusterA.stopNodes(nodeId -> nodeId == coordinatorNodeId, TerminationStyle.GRACEFUL);

            // Wait for Kafka to elect a new coordinator
            waitForNewCoordinator(saslClusterA, "__transaction_state", transactionalId, coordinatorNodeId);

            // Then: a new transaction should succeed after coordinator rediscovery
            try (var producer = new KafkaProducer<String, String>(producerConfig)) {
                producer.initTransactions();
                producer.beginTransaction();
                producer.send(new ProducerRecord<>(topic, "key-after", "val-after")).get(30, TimeUnit.SECONDS);
                producer.commitTransaction();
            }

            var recordsAfter = consumeDirectly(saslClusterA, topic);
            assertThat(recordsAfter).extracting(ConsumerRecord::value)
                    .contains("val-before", "val-after");

            saslClusterA.startNodes(nodeId -> nodeId == coordinatorNodeId);
        }
    }

    // --- New consumer group (KIP-848) coordinator change ---

    @Test
    void shouldRebalanceNewConsumerGroupAfterCoordinatorChange(
                                                               @SaslMechanism(value = "PLAIN", principals = {
                                                                       @SaslMechanism.Principal(user = SASL_USERNAME, password = SASL_PASSWORD)
                                                               }) @BrokerConfig(name = "offsets.topic.replication.factor", value = "3") @BrokerConfig(name = "offsets.topic.num.partitions", value = "10") @BrokerCluster(numBrokers = 3) KafkaCluster saslClusterA,
                                                               @BrokerCluster(numBrokers = 3) KafkaCluster clusterB)
            throws Exception {
        shouldRebalanceConsumerGroupAfterCoordinatorChange(saslClusterA, clusterB, "consumer");
    }

    // --- Classic consumer group coordinator change ---

    @Test
    void shouldRebalanceClassicConsumerGroupAfterCoordinatorChange(
                                                                   @SaslMechanism(value = "PLAIN", principals = {
                                                                           @SaslMechanism.Principal(user = SASL_USERNAME, password = SASL_PASSWORD)
                                                                   }) @BrokerConfig(name = "offsets.topic.replication.factor", value = "3") @BrokerConfig(name = "offsets.topic.num.partitions", value = "10") @BrokerCluster(numBrokers = 3) KafkaCluster saslClusterA,
                                                                   @BrokerCluster(numBrokers = 3) KafkaCluster clusterB)
            throws Exception {
        shouldRebalanceConsumerGroupAfterCoordinatorChange(saslClusterA, clusterB, "classic");
    }

    private void shouldRebalanceConsumerGroupAfterCoordinatorChange(
                                                                    KafkaCluster saslClusterA,
                                                                    KafkaCluster clusterB,
                                                                    String groupProtocol)
            throws Exception {
        String topic = "a.cg-coord-test-" + groupProtocol;
        int numPartitions = 4;
        createTopic(saslClusterA, topic, numPartitions);

        // Produce records to all partitions directly
        try (var producer = new KafkaProducer<>(directProducerConfig(saslClusterA))) {
            for (int p = 0; p < numPartitions; p++) {
                producer.send(new ProducerRecord<>(topic, p, "key-" + p, "val-" + p)).get(10, TimeUnit.SECONDS);
            }
        }

        // Warm up __consumer_offsets by briefly joining a throwaway group
        try (var consumer = new KafkaConsumer<String, String>(directConsumerConfig(saslClusterA, "warmup-" + groupProtocol))) {
            consumer.subscribe(List.of(topic));
            consumer.poll(Duration.ofSeconds(5));
        }

        String groupId = findKeyOnNonControllerBroker(saslClusterA, "__consumer_offsets", "coord-resilience-group-" + groupProtocol + "-");

        var config = buildSaslConfig(saslClusterA, clusterB, Map.of(SASL_USERNAME, "route-a"));

        try (var tester = kroxyliciousTester(config)) {
            String bootstrap = tester.getBootstrapAddress();

            // Given: consumer-1 joins and gets partitions
            var consumer1Ready = new AtomicBoolean(false);
            var consumer1Thread = new Thread(() -> {
                try (var consumer = new KafkaConsumer<String, String>(
                        saslConsumerConfig(bootstrap, groupId, groupProtocol))) {
                    consumer.subscribe(List.of(topic));
                    long deadline = System.currentTimeMillis() + 60_000;
                    while (System.currentTimeMillis() < deadline) {
                        consumer.poll(Duration.ofMillis(500));
                        if (!consumer.assignment().isEmpty()) {
                            consumer1Ready.set(true);
                        }
                    }
                }
            }, "consumer-1-" + groupProtocol);
            consumer1Thread.setDaemon(true);
            consumer1Thread.start();

            // Wait for consumer-1 to get an assignment
            long waitDeadline = System.currentTimeMillis() + 30_000;
            while (!consumer1Ready.get() && System.currentTimeMillis() < waitDeadline) {
                Thread.sleep(200);
            }
            assertThat(consumer1Ready.get())
                    .as("consumer-1 should get an assignment (protocol: %s)", groupProtocol)
                    .isTrue();

            // When: stop the group coordinator broker
            int coordinatorNodeId = findCoordinatorBroker(saslClusterA, "__consumer_offsets", groupId);
            LOGGER.info("Stopping group coordinator node {} for group '{}' (protocol: {})",
                    coordinatorNodeId, groupId, groupProtocol);
            saslClusterA.stopNodes(nodeId -> nodeId == coordinatorNodeId, TerminationStyle.GRACEFUL);

            // Then: consumer-2 should be able to join and get partitions
            var consumer2Partitions = Collections.synchronizedList(new ArrayList<TopicPartition>());
            var consumer2Thread = new Thread(() -> {
                try (var consumer = new KafkaConsumer<String, String>(
                        saslConsumerConfig(bootstrap, groupId, groupProtocol))) {
                    consumer.subscribe(List.of(topic));
                    long deadline = System.currentTimeMillis() + 60_000;
                    while (System.currentTimeMillis() < deadline) {
                        consumer.poll(Duration.ofMillis(500));
                        var assignment = consumer.assignment();
                        if (!assignment.isEmpty()) {
                            synchronized (consumer2Partitions) {
                                consumer2Partitions.clear();
                                consumer2Partitions.addAll(assignment);
                            }
                        }
                    }
                }
            }, "consumer-2-" + groupProtocol);
            consumer2Thread.setDaemon(true);
            consumer2Thread.start();

            // Wait for consumer-2 to get a non-empty assignment
            waitDeadline = System.currentTimeMillis() + 45_000;
            while (consumer2Partitions.isEmpty() && System.currentTimeMillis() < waitDeadline) {
                Thread.sleep(200);
            }

            assertThat(consumer2Partitions)
                    .as("consumer-2 should receive partition assignments after coordinator change (protocol: %s)", groupProtocol)
                    .isNotEmpty();

            LOGGER.info("consumer-2 got {} partitions (protocol: {})", consumer2Partitions.size(), groupProtocol);

            // Cleanup
            saslClusterA.startNodes(nodeId -> nodeId == coordinatorNodeId);
            consumer1Thread.join(5000);
            consumer2Thread.join(5000);
        }
    }

    // --- Helpers ---

    private static void waitForNewCoordinator(KafkaCluster cluster, String internalTopic, String key, int oldNodeId) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                int newNodeId = findCoordinatorBroker(cluster, internalTopic, key);
                if (newNodeId != oldNodeId) {
                    LOGGER.info("New coordinator elected: node {} (was {})", newNodeId, oldNodeId);
                    return;
                }
            }
            catch (Exception e) {
                LOGGER.debug("Waiting for new coordinator, got error: {}", e.getMessage());
            }
            Thread.sleep(500);
        }
        LOGGER.warn("Timed out waiting for new coordinator (old node {}), proceeding anyway", oldNodeId);
    }

    private static int findCoordinatorBroker(KafkaCluster cluster, String internalTopic, String key) throws Exception {
        try (var admin = AdminClient.create(cluster.getKafkaClientConfiguration())) {
            var topicDesc = admin.describeTopics(List.of(internalTopic))
                    .allTopicNames().get(10, TimeUnit.SECONDS);
            int numPartitions = topicDesc.get(internalTopic).partitions().size();
            int partition = Utils.abs(key.hashCode()) % numPartitions;
            int leaderNodeId = topicDesc.get(internalTopic).partitions().get(partition).leader().id();
            LOGGER.info("Coordinator for key '{}' on '{}': partition={}, leader={}",
                    key, internalTopic, partition, leaderNodeId);
            return leaderNodeId;
        }
    }

    /**
     * Finds a key suffix such that the coordinator for {@code prefix + suffix}
     * lands on a non-controller broker (i.e. not node 0).
     */
    private static String findKeyOnNonControllerBroker(KafkaCluster cluster, String internalTopic, String prefix) throws Exception {
        try (var admin = AdminClient.create(cluster.getKafkaClientConfiguration())) {
            var topicDesc = admin.describeTopics(List.of(internalTopic))
                    .allTopicNames().get(10, TimeUnit.SECONDS);
            int numPartitions = topicDesc.get(internalTopic).partitions().size();
            for (int i = 0; i < 100; i++) {
                String candidate = prefix + i;
                int partition = Utils.abs(candidate.hashCode()) % numPartitions;
                int leader = topicDesc.get(internalTopic).partitions().get(partition).leader().id();
                if (leader != 0) {
                    LOGGER.info("Selected key '{}' → partition={}, leader={} (non-controller)",
                            candidate, partition, leader);
                    return candidate;
                }
            }
            throw new IllegalStateException("Could not find a key whose coordinator is on a non-controller broker");
        }
    }

    private static void createTopic(KafkaCluster cluster, String topicName, int partitions) throws Exception {
        try (var admin = AdminClient.create(cluster.getKafkaClientConfiguration())) {
            admin.createTopics(List.of(new NewTopic(topicName, partitions, (short) 3)))
                    .all().get(10, TimeUnit.SECONDS);
        }
    }

    private static List<ConsumerRecord<String, String>> consumeDirectly(KafkaCluster cluster, String topic) {
        var props = new HashMap<>(cluster.getKafkaClientConfiguration());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "verify-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(Set.of(topic));
            List<ConsumerRecord<String, String>> all = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 15_000;
            int consecutiveEmpty = 0;
            while (System.currentTimeMillis() < deadline && consecutiveEmpty < 3) {
                ConsumerRecords<String, String> batch = consumer.poll(Duration.ofMillis(500));
                batch.forEach(all::add);
                if (batch.isEmpty() && !all.isEmpty()) {
                    consecutiveEmpty++;
                }
                else if (!batch.isEmpty()) {
                    consecutiveEmpty = 0;
                }
            }
            return all;
        }
    }

    private static ConfigurationBuilder buildSaslConfig(KafkaCluster clusterA,
                                                        KafkaCluster clusterB,
                                                        Map<String, String> subjectRoutes) {
        var targetA = new ClusterDefinition("cluster-a", clusterA.getBootstrapServers(), null);
        var targetB = new ClusterDefinition("cluster-b", clusterB.getBootstrapServers(), null);

        var routeA = new RouteDefinition("route-a", 0, null, new RouteTarget("cluster-a", null));
        var routeB = new RouteDefinition("route-b", 1, null, new RouteTarget("cluster-b", null));

        var routerConfig = new TopicPartitionRouterConfig(
                "route-a",
                List.of(
                        new RouteConfig("route-a", List.of("a."), null,
                                subjectsForRoute("route-a", subjectRoutes)),
                        new RouteConfig("route-b", List.of("b."), null,
                                subjectsForRoute("route-b", subjectRoutes))));

        var routerDef = new RouterDefinition("topic-router",
                TopicPartitionRouterFactory.class.getName(), routerConfig, List.of(routeA, routeB));

        var saslFilter = new NamedFilterDefinitionBuilder(
                SaslInspection.class.getName(),
                SaslInspection.class.getName())
                .withConfig("enabledMechanisms", Set.of("PLAIN"))
                .build();

        var vc = new VirtualClusterBuilder()
                .withName("demo")
                .withTarget(new RouteTarget(null, "topic-router"))
                .addToGateways(defaultPortIdentifiesNodeGatewayBuilder("localhost:9192")
                        .editPortIdentifiesNode()
                        .addToNodeIdRanges(new NamedRange("brokers", 0, 5))
                        .endPortIdentifiesNode()
                        .build())
                .build();

        return baseConfigurationBuilder()
                .addToClusterDefinitions(targetA, targetB)
                .addToRouterDefinitions(routerDef)
                .addToFilterDefinitions(saslFilter)
                .addToDefaultFilters(saslFilter.name())
                .addToVirtualClusters(vc);
    }

    private static List<String> subjectsForRoute(String route, Map<String, String> subjectRoutes) {
        var subjects = subjectRoutes.entrySet().stream()
                .filter(e -> route.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        return subjects.isEmpty() ? null : subjects;
    }

    private static Map<String, Object> saslProducerConfig(String bootstrap, String transactionalId) {
        var config = new HashMap<String, Object>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 30000);
        config.put("security.protocol", "SASL_PLAINTEXT");
        config.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        config.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                        + "username=\"" + SASL_USERNAME + "\"\n"
                        + "password=\"" + SASL_PASSWORD + "\";");
        return config;
    }

    private static Map<String, Object> saslConsumerConfig(String bootstrap, String groupId, String groupProtocol) {
        var config = new HashMap<String, Object>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, groupProtocol);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        if ("classic".equals(groupProtocol)) {
            config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
            config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        }
        config.put("security.protocol", "SASL_PLAINTEXT");
        config.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        config.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required\n"
                        + "username=\"" + SASL_USERNAME + "\"\n"
                        + "password=\"" + SASL_PASSWORD + "\";");
        return config;
    }

    private static Map<String, Object> directConsumerConfig(KafkaCluster cluster, String groupId) {
        var config = new HashMap<String, Object>(cluster.getKafkaClientConfiguration());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return config;
    }

    private static Map<String, Object> directProducerConfig(KafkaCluster cluster) {
        var config = new HashMap<String, Object>(cluster.getKafkaClientConfiguration());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return config;
    }
}
