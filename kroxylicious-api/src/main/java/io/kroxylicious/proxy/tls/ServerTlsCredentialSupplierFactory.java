/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.tls;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.concurrent.ScheduledExecutorService;

import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterDispatchExecutor;
import io.kroxylicious.proxy.filter.FilterFactoryContext;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;
import io.kroxylicious.proxy.plugin.UnknownPluginInstanceException;

/**
 * <p>A pluggable source of {@link ServerTlsCredentialSupplier} instances.</p>
 * <p>ServerTlsCredentialSupplierFactories are:</p>
 * <ul>
 * <li>{@linkplain java.util.ServiceLoader service} implementations provided by plugin authors</li>
 * <li>called by the proxy runtime to {@linkplain #create(Context, Object) create} instances</li>
 * </ul>
 * @param <C> The type of configuration.
 * @param <I> The type of initialization data.
 */
public interface ServerTlsCredentialSupplierFactory<C, I> {

    I initialize(Context context, C config) throws PluginConfigurationException;

    ServerTlsCredentialSupplier create(Context context, I initializationData);

    default void close(I initializationData) {
    }

    interface Context {

        /**
         * An executor backed by the single Thread responsible for dispatching
         * work to a ServerTlsCredentialSupplier instance for a channel.
         * It is safe to mutate ServerTlsCredentialSupplier members from this executor.
         * @return executor
         * @throws IllegalStateException if the factory is not bound to a channel yet.
         */
        ScheduledExecutorService filterDispatchExecutor();

        /**
         * Gets a plugin instance for the given plugin type and name
         * @param pluginClass The plugin type
         * @param instanceName The plugin instance name
         * @return The plugin instance
         * @param <P> The plugin manager type
         * @throws UnknownPluginInstanceException If the plugin could not be instantiated.
         */
        <P> P pluginInstance(Class<P> pluginClass,
                             String instanceName)
                throws UnknownPluginInstanceException;

         /**
         * Creates some TLS credentials for the given parameters.
         * @param certificate The TLS certificate
         * @param key The key corresponding to the given {@code certificate}.
         * @param intermediateCertificates Intermediate certificates forming the certificate chain up to (but not including)
         * the TLS certificate trusted by the peer.
         * @return The TLS credentials instance.
         * @see ServerTlsCredentialSupplier.Context#tlsCredentials(Certificate, PrivateKey, Certificate[])
         */
         TlsCredentials tlsCredentials(Certificate certificate,
                                       PrivateKey key,
                                       Certificate[] intermediateCertificates);
    }
}
