/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.internal;

import java.util.Objects;
import java.util.Optional;

import io.kroxylicious.proxy.authentication.ClientSaslContext;
import io.kroxylicious.proxy.authentication.SaslPrincipal;

import edu.umd.cs.findbugs.annotations.Nullable;

public class FilterChainDispatch implements ClientSaslContext {

    private final boolean useSaslPrincipal;
    private SaslPrincipal saslPrincipal = null;
    private String mechanism;
    private SaslPrincipal proxyPrincipal;

    public FilterChainDispatch(boolean useSaslPrincipal) {
        this.useSaslPrincipal = useSaslPrincipal;
        this.saslPrincipal = null;
    }

    boolean isUseSaslPrincipal() {
        return useSaslPrincipal;
    }

    void clientSaslAuthenticationSuccess(String mechanism,
                                         SaslPrincipal principal,
                                         @Nullable SaslPrincipal proxyPrincipal) {
        Objects.requireNonNull(mechanism, "mechanism");
        Objects.requireNonNull(principal, "principal");
        if (this.saslPrincipal != null) {
            throw new IllegalStateException("SaslPrincipal is already set");
        }
        this.saslPrincipal = principal;
        this.mechanism = mechanism;
        this.proxyPrincipal = proxyPrincipal;
    }

    public Optional<ClientSaslContext> clientSaslContext() {
        if (saslPrincipal != null) {
            return Optional.of(this);
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public String mechanismName() {
        return this.mechanism;
    }

    @Override
    public SaslPrincipal clientPrincipal() {
        return saslPrincipal;
    }

    @Override
    public Optional<SaslPrincipal> proxyServerPrincipal() {
        return Optional.ofNullable(this.proxyPrincipal);
    }

}
