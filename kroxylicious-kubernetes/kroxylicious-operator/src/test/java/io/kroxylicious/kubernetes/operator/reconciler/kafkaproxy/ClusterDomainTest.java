/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator.reconciler.kafkaproxy;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClusterDomainTest {

    @Test
    void isClusterIpMatches() {
        ClusterDomain.setClusterCidrs(List.of("10.244.0.0/16",
                "10.96.0.0/31",
                "fd00:10:244::/48",
                "fd00:10:96::/112"));
        assertThat(ClusterDomain.isClusterIp("10.244.0.0")).isTrue();
        assertThat(ClusterDomain.isClusterIp("10.244.1.0")).isTrue();
        assertThat(ClusterDomain.isClusterIp("10.244.0.255")).isTrue();
        assertThat(ClusterDomain.isClusterIp("10.244.255.0")).isTrue();
        assertThat(ClusterDomain.isClusterIp("10.244.255.255")).isTrue();
        assertThat(ClusterDomain.isClusterIp("10.245.0.0")).isFalse();

        assertThat(ClusterDomain.isClusterIp("10.96.0.0")).isTrue();
        assertThat(ClusterDomain.isClusterIp("10.96.0.1")).isTrue();
        assertThat(ClusterDomain.isClusterIp("10.96.0.2")).isFalse();
        assertThat(ClusterDomain.isClusterIp("10.96.0.0")).isTrue();

        assertThat(ClusterDomain.isClusterIp("fd00:10:244::")).isTrue();
        assertThat(ClusterDomain.isClusterIp("fd00:10:244:01::")).isTrue();
        assertThat(ClusterDomain.isClusterIp("fd00:10:244:ffff:ffff::")).isTrue();
        assertThat(ClusterDomain.isClusterIp("fd00:10:245::")).isFalse();

    }

}