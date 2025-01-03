/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.config.tls;

import java.util.Objects;

public class TlsUtils {
    private TlsUtils() {
    }

    public static String getType(TrustStore trustStore) {
        return Tls.getStoreTypeOrPlatformDefault(trustStore.storeType());
    }

    public static boolean isPemType(TrustStore trustStore) {
        return Objects.equals(getType(trustStore), Tls.PEM);
    }

    public static String getType(KeyStore keyStore) {
        return Tls.getStoreTypeOrPlatformDefault(keyStore.storeType());
    }

    public static boolean isPemType(KeyStore keyStore) {
        return Objects.equals(getType(keyStore), Tls.PEM);
    }
}
