/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.testplugins;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import io.kroxylicious.proxy.plugin.Plugin;
import io.kroxylicious.proxy.plugin.PluginConfigurationException;
import io.kroxylicious.proxy.tls.ServerTlsCredentialSupplier;
import io.kroxylicious.proxy.tls.ServerTlsCredentialSupplierFactory;

@Plugin(configType = Void.class)
public class PerPrincipalTlsCerts implements ServerTlsCredentialSupplierFactory<String, KeyStore> {
    @Override
    public KeyStore initialize(Context context, String keyStorePath) throws PluginConfigurationException {
        try {
            return KeyStore.getInstance(new File(keyStorePath), new char[0]);
        }
        catch (IOException | GeneralSecurityException e) {
            throw new PluginConfigurationException("Could not create KeyStore from file " + keyStorePath, e);
        }
    }

    @Override
    public ServerTlsCredentialSupplier create(Context context, KeyStore initializationData) {
        return new PerPrincipalTlsCertsSupplier(initializationData);
    }
}
