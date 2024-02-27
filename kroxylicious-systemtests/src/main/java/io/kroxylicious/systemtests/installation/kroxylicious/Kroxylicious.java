/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.systemtests.installation.kroxylicious;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.SecretBuilder;

import io.kroxylicious.kms.provider.hashicorp.vault.config.Config;
import io.kroxylicious.systemtests.Constants;
import io.kroxylicious.systemtests.Environment;
import io.kroxylicious.systemtests.k8s.exception.KubeClusterException;
import io.kroxylicious.systemtests.resources.manager.ResourceManager;
import io.kroxylicious.systemtests.templates.kroxylicious.KroxyliciousConfigMapTemplates;
import io.kroxylicious.systemtests.templates.kroxylicious.KroxyliciousDeploymentTemplates;
import io.kroxylicious.systemtests.templates.kroxylicious.KroxyliciousSecretTemplates;
import io.kroxylicious.systemtests.templates.kroxylicious.KroxyliciousServiceTemplates;

import static io.kroxylicious.systemtests.k8s.KubeClusterResource.kubeClient;
import static org.awaitility.Awaitility.await;

/**
 * The type Kroxylicious.
 */
public class Kroxylicious {
    private static final Logger LOGGER = LoggerFactory.getLogger(Kroxylicious.class);
    private final String deploymentNamespace;
    private final String containerImage;
    private final ResourceManager resourceManager = ResourceManager.getInstance();

    /**
     * Instantiates a new Kroxylicious Service to be used in kubernetes.
     *
     * @param deploymentNamespace the deployment namespace
     */
    public Kroxylicious(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        String kroxyUrl = Environment.KROXY_IMAGE_REPO + (Environment.KROXY_IMAGE_REPO.endsWith(":") ? "" : ":");
        this.containerImage = kroxyUrl + Environment.KROXY_VERSION;
    }

    private void createDefaultConfigMap(String clusterName) {
        LOGGER.info("Deploy Kroxylicious default config Map without filters in {} namespace", deploymentNamespace);
        resourceManager.createResourceWithWait(KroxyliciousConfigMapTemplates.defaultKroxyliciousConfig(clusterName, deploymentNamespace).build());
    }

    private void createRecordEncryptionFilterConfigMap(String clusterName, String topicName, Config config) {
        LOGGER.info("Deploy Kroxylicious config Map without filters in {} namespace", deploymentNamespace);
        resourceManager
                .createResourceWithWait(KroxyliciousConfigMapTemplates.kroxyliciousRecordEncryptionConfig(clusterName, deploymentNamespace, topicName, config).build());
    }

    private void deployPortPerBrokerPlain(int replicas) {
        LOGGER.info("Deploy Kroxylicious in {} namespace", deploymentNamespace);
        createSecrets();
        resourceManager.createResourceWithWait(KroxyliciousDeploymentTemplates.defaultKroxyDeployment(deploymentNamespace, containerImage, replicas).build());
        resourceManager.createResourceWithoutWait(KroxyliciousServiceTemplates.defaultKroxyService(deploymentNamespace).build());
    }

    /**
     * Needed for downstream Kroxylicious images
     */
    private void createSecrets() {
        String configFolder = Environment.CONTAINER_CONFIG_PATH;
        SecretBuilder secretBuilder = KroxyliciousSecretTemplates.createRegistryCredentialsSecret(configFolder, deploymentNamespace);
        if (secretBuilder != null) {
            resourceManager.createResourceWithWait(secretBuilder.build());
        }
    }

    /**
     * Deploy - Port per broker plain with no filters config
     * @param clusterName the cluster name
     * @param replicas the replicas
     */
    public void deployPortPerBrokerPlainWithNoFilters(String clusterName, int replicas) {
        createDefaultConfigMap(clusterName);
        deployPortPerBrokerPlain(replicas);
    }

    /**
     * Deploy port per broker plain with topic encryption filter.
     *
     * @param clusterName the cluster name
     * @param replicas the replicas
     * @param topicName the topic name
     * @param config config
     */
    public void deployPortPerBrokerPlainWithRecordEncryptionFilter(String clusterName, int replicas, String topicName, Config config) {
        createRecordEncryptionFilterConfigMap(clusterName, topicName, config);
        deployPortPerBrokerPlain(replicas);
    }

    /**
     * Gets number of replicas.
     *
     * @return the number of replicas
     */
    public int getNumberOfReplicas() {
        LOGGER.info("Getting number of replicas..");
        return kubeClient().getDeployment(deploymentNamespace, Constants.KROXY_DEPLOYMENT_NAME).getStatus().getReplicas();
    }

    /**
     * Get bootstrap
     *
     * @return the bootstrap
     */
    public String getBootstrap() {
        String clusterIP = kubeClient().getService(deploymentNamespace, Constants.KROXY_SERVICE_NAME).getSpec().getClusterIP();
        if (clusterIP == null || clusterIP.isEmpty()) {
            throw new KubeClusterException("Unable to get the clusterIP of Kroxylicious");
        }
        String bootstrap = clusterIP + ":9292";
        LOGGER.debug("Kroxylicious bootstrap: {}", bootstrap);
        return bootstrap;
    }

    /**
     * Scale replicas to.
     *
     * @param scaledTo the number of replicas to scale up/down
     * @param timeout the timeout
     */
    public void scaleReplicasTo(int scaledTo, Duration timeout) {
        LOGGER.info("Scaling number of replicas to {}..", scaledTo);
        kubeClient().getClient().apps().deployments().inNamespace(deploymentNamespace).withName(Constants.KROXY_DEPLOYMENT_NAME).scale(scaledTo);
        await().atMost(timeout).pollInterval(Duration.ofSeconds(1))
                .until(() -> getNumberOfReplicas() == scaledTo && kubeClient().isDeploymentReady(deploymentNamespace, Constants.KROXY_DEPLOYMENT_NAME));
    }
}
