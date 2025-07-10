/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.filter;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.common.header.internals.RecordHeader;

import io.kroxylicious.proxy.authentication.ClientPrincipalAware;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A filter that adds client principal-dependent headers to produced records.
 * Tests can consume the produced records and assert that those records have the expected header values.
 */
public class ClientSaslPrincipalAwareLawyerFilter
        extends AbstractProduceHeaderInjectionFilter
        implements ClientPrincipalAware<Principal> {

    public static final String HEADER_KEY_CLIENT_PRINCIPAL = ClientSaslPrincipalAwareLawyerFilter.class.getSimpleName() + "#clientPrincipal";
    private List<RecordHeader> result = new ArrayList<RecordHeader>();

    @Override
    public void onClientAuthentication(Principal clientPrincipal) {
        result.add(new RecordHeader(HEADER_KEY_CLIENT_PRINCIPAL, clientPrincipal == null ? null : clientPrincipal.getName().getBytes(StandardCharsets.UTF_8)));
    }

    @NonNull
    @Override
    protected List<RecordHeader> headersToAdd(FilterContext context) {
        return result;
    }
}
