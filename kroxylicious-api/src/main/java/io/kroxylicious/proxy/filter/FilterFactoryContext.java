/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.filter;

import java.security.PrivateKey;
import java.security.cert.Certificate;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.x500.X500Principal;

import io.kroxylicious.proxy.authentication.ClientSaslContext;
import io.kroxylicious.proxy.authentication.ClientSaslAware;
import io.kroxylicious.proxy.authentication.SaslPrincipal;
import io.kroxylicious.proxy.authentication.ServerTlsCredentialContext;
import io.kroxylicious.proxy.authentication.TlsCredentials;
import io.kroxylicious.proxy.plugin.UnknownPluginInstanceException;

/**
 * Construction context for Filters. Used to pass the filter configuration and environmental resources
 * to the FilterFactory when it is creating a new instance of the Filter. see {@link FilterFactory#createFilter(FilterFactoryContext, Object)}
 */
public interface FilterFactoryContext {

    /**
     * An executor backed by the single Thread responsible for dispatching
     * work to a Filter instance for a channel.
     * It is safe to mutate Filter members from this executor.
     * @return executor
     * @throws IllegalStateException if the factory is not bound to a channel yet.
     */
    FilterDispatchExecutor filterDispatchExecutor();

    /**
     * Gets a plugin instance for the given plugin type and name
     * @param pluginClass The plugin type
     * @param instanceName The plugin instance name
     * @return The plugin instance
     * @param <P> The plugin manager type
     * @throws UnknownPluginInstanceException
     */
    <P> P pluginInstance(Class<P> pluginClass, String instanceName);

    /**
     * Creates some TLS credentials for the given parameters.
     * @param certificate The TLS certificate
     * @param key The key corresponding to the given {@code certificate}.
     * @param intermediateCertificates Intermediate certificates forming the certificate chain up to (but not including)
     * the TLS certificate trusted by the peer.
     * @return The TLS credentials instance.
     * @see ServerTlsCredentialContext#tlsCredentials(Certificate, PrivateKey, Certificate[])
     */
    TlsCredentials tlsCredentials(Certificate certificate, PrivateKey key, Certificate[] intermediateCertificates);
}
