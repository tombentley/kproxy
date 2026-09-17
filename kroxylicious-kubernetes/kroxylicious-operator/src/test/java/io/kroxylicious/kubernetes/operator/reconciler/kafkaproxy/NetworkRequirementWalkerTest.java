/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.kubernetes.operator.reconciler.kafkaproxy;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kms.provider.aws.kms.config.CredentialsConfig;
import io.kroxylicious.proxy.config.NamedRange;
import io.kroxylicious.proxy.config.NetworkRequirementWalker;
import io.kroxylicious.proxy.config.PortIdentifiesNodeIdentificationStrategy;
import io.kroxylicious.proxy.service.HostPort;

class NetworkRequirementWalkerTest {

    @Test
    void test() throws Exception {

        NetworkRequirementWalker networkRequirementWalker = new NetworkRequirementWalker();
        var x = networkRequirementWalker.walk(new io.kroxylicious.kms.provider.aws.kms.config.Config(URI.create("https://foo.example.com/bar"), new CredentialsConfig(null, null, null, null), "xxx", null));
        System.out.println(x);
        var y = networkRequirementWalker.walk(new PortIdentifiesNodeIdentificationStrategy(
                new HostPort("0.0.0.0", 9092),
                "proxy.example.com",
                1234,
                List.of(new NamedRange("a", 0, 3))));
        System.out.println(y);
    }

}