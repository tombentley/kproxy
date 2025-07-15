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
    private String clientAuthorizationId;
    private String mechanism;
    private String proxyServerId;

    public FilterChainDispatch(boolean useSaslPrincipal) {
        this.useSaslPrincipal = useSaslPrincipal;
        this.clientAuthorizationId = null;
    }

    boolean isUseSaslPrincipal() {
        return useSaslPrincipal;
    }

    void clientSaslAuthenticationSuccess(String mechanism,
                                         String clientAuthorizationId,
                                         @Nullable String proxyServerId) {
        Objects.requireNonNull(mechanism, "mechanism");
        Objects.requireNonNull(clientAuthorizationId, "clientAuthorizationId");
        if (this.clientAuthorizationId != null) {
            throw new IllegalStateException("SaslPrincipal is already set");
        }
        this.clientAuthorizationId = clientAuthorizationId;
        this.mechanism = mechanism;
        this.proxyServerId = proxyServerId;
    }

    public Optional<ClientSaslContext> clientSaslContext() {
        if (clientAuthorizationId != null) {
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
    public String authorizationId() {
        return this.clientAuthorizationId;
    }

    @Override
    public Optional<String> proxyServerId() {
        return Optional.ofNullable(this.proxyServerId);
    }

    Optional<SaslPrincipal> clientPrincipal() {
        return Optional.ofNullable(this.clientAuthorizationId)
                .map(SaslPrincipal::new);
    }

}
