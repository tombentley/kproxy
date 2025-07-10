/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.filter;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.apache.kafka.common.header.internals.RecordHeader;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A filter that adds {@linkplain FilterContext#clientTlsContext() client-facing TLS context}-dependent headers to produced records.
 * Tests can consume the produced records and assert that those records have the expected header values.
 */
public class ClientTlsAwareLawyerFilter
        extends AbstractProduceHeaderInjectionFilter {

    public static final String HEADER_KEY_CLIENT_CONNECTION_TLS = ClientTlsAwareLawyerFilter.class.getSimpleName() + "#clientConnectionTls";
    public static final String HEADER_KEY_PROXY_PRINCIPAL_NAME = ClientTlsAwareLawyerFilter.class.getSimpleName() + "#proxyPrincipalName";
    public static final String HEADER_KEY_CLIENT_PRINCIPAL_NAME = ClientTlsAwareLawyerFilter.class.getSimpleName() + "#clientPrincipalName";

    private static String principalName(X509Certificate x509Certificate) {
        return x509Certificate.getSubjectX500Principal()
                .getName(X500Principal.RFC1779,
                        Map.of("1.2.840.113549.1.9.1", "emailAddress"));
    }

    @NonNull
    @Override
    protected List<RecordHeader> headersToAdd(FilterContext context) {
        var headers = new ArrayList<RecordHeader>();
        headers.add(new RecordHeader(HEADER_KEY_CLIENT_CONNECTION_TLS, context.clientTlsContext().isPresent() ? new byte[]{ 1 } : new byte[]{ 0 }));
        headers.add(new RecordHeader(HEADER_KEY_PROXY_PRINCIPAL_NAME, context.clientTlsContext()
                .map(FilterContext.ClientTlsContext::proxyServerCertificate)
                .map(ClientTlsAwareLawyerFilter::principalName)
                .map(String::getBytes)
                .orElse(null)));
        headers.add(new RecordHeader(HEADER_KEY_CLIENT_PRINCIPAL_NAME, context.clientTlsContext()
                .map(FilterContext.ClientTlsContext::clientCertificate)
                .flatMap(opt -> opt.map(ClientTlsAwareLawyerFilter::principalName))
                .map(String::getBytes)
                .orElse(null)));
        return headers;
    }

}
