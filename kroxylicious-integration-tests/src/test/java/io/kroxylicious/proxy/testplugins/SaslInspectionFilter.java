/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.testplugins;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.ResponseHeaderData;
import org.apache.kafka.common.message.SaslAuthenticateRequestData;
import org.apache.kafka.common.message.SaslAuthenticateResponseData;
import org.apache.kafka.common.message.SaslHandshakeRequestData;
import org.apache.kafka.common.message.SaslHandshakeResponseData;
import org.apache.kafka.common.protocol.Errors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kroxylicious.proxy.filter.FilterContext;
import io.kroxylicious.proxy.filter.RequestFilterResult;
import io.kroxylicious.proxy.filter.ResponseFilterResult;
import io.kroxylicious.proxy.filter.SaslAuthenticateRequestFilter;
import io.kroxylicious.proxy.filter.SaslAuthenticateResponseFilter;
import io.kroxylicious.proxy.filter.SaslHandshakeRequestFilter;
import io.kroxylicious.proxy.filter.SaslHandshakeResponseFilter;

public class SaslInspectionFilter
        implements
        SaslHandshakeRequestFilter,
        SaslHandshakeResponseFilter,
        SaslAuthenticateRequestFilter,
        SaslAuthenticateResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaslInspectionFilter.class);

    private final boolean producePrincipal;

    private String proposedMechanism;
    private boolean mechanismAgreed;
    private String authorizationIdFromClient;

    public SaslInspectionFilter(boolean producePrincipal) {
        this.producePrincipal = producePrincipal;
        resetState();
    }

    private void resetState() {
        proposedMechanism = null;
        mechanismAgreed = false;
        authorizationIdFromClient = null;
    }

    /* This function originally copied from Apach Kafka's PlainSaslServer */
    private static List<String> extractTokens(String string) {
        /*
         * Message format (from https://tools.ietf.org/html/rfc4616):
         *
         * message = [authzid] UTF8NUL authcid UTF8NUL passwd
         * authcid = 1*SAFE ; MUST accept up to 255 octets
         * authzid = 1*SAFE ; MUST accept up to 255 octets
         * passwd = 1*SAFE ; MUST accept up to 255 octets
         * UTF8NUL = %x00 ; UTF-8 encoded NUL character
         *
         * SAFE = UTF1 / UTF2 / UTF3 / UTF4
         * ;; any UTF-8 encoded Unicode character except NUL
         */
        List<String> tokens = new ArrayList<>();
        int startIndex = 0;
        for (int i = 0; i < 4; ++i) {
            int endIndex = string.indexOf("\u0000", startIndex);
            if (endIndex == -1) {
                tokens.add(string.substring(startIndex));
                break;
            }
            tokens.add(string.substring(startIndex, endIndex));
            startIndex = endIndex + 1;
        }

        if (tokens.size() != 3) {
            throw new SaslAuthenticationException("Invalid SASL/PLAIN response: expected 3 tokens, got " +
                    tokens.size());
        }

        return tokens;
    }

    @Override
    public CompletionStage<RequestFilterResult> onSaslHandshakeRequest(short apiVersion,
                                                                       RequestHeaderData header,
                                                                       SaslHandshakeRequestData request,
                                                                       FilterContext context) {
        this.mechanismAgreed = false;
        this.proposedMechanism = request.mechanism();
        if ("PLAIN".equals(proposedMechanism)) {

            LOGGER.info("Client '{}' on channel {} proposes SASL mechanism '{}'",
                    header.clientId(),
                    context.channelDescriptor(),
                    proposedMechanism);
            return context.forwardRequest(header, request);
        }
        else {
            LOGGER.info("Client '{}' on channel {} proposes SASL mechanism '{}' unsupported by this filter, responding with {}",
                    header.clientId(),
                    context.channelDescriptor(),
                    proposedMechanism,
                    Errors.UNSUPPORTED_SASL_MECHANISM.name());
            // shortcircuit response if mechanism is not one we support
            return context.requestFilterResultBuilder()
                    .shortCircuitResponse(
                            new SaslHandshakeResponseData()
                                    .setErrorCode(Errors.UNSUPPORTED_SASL_MECHANISM.code())
                                    .setMechanisms(List.of("PLAIN")))
                    .completed();
        }

    }

    @Override
    public CompletionStage<ResponseFilterResult> onSaslHandshakeResponse(short apiVersion,
                                                                         ResponseHeaderData header,
                                                                         SaslHandshakeResponseData response,
                                                                         FilterContext context) {
        this.mechanismAgreed = response.errorCode() == Errors.NONE.code();
        if (LOGGER.isInfoEnabled()) {
            if (this.mechanismAgreed) {
                LOGGER.info("Server accepts proposed SASL mechanism '{}' from client on channel {}",
                        proposedMechanism,
                        context.channelDescriptor());
            }
            else {
                LOGGER.info("Server rejects proposed SASL mechanism '{}' from client on channel {} with error {}; supports {}",
                        proposedMechanism,
                        context.channelDescriptor(),
                        Errors.forCode(response.errorCode()).name(),
                        response.mechanisms());
            }
        }
        return context.forwardResponse(header, response);
    }

    @Override
    public CompletionStage<RequestFilterResult> onSaslAuthenticateRequest(short apiVersion,
                                                                          RequestHeaderData header,
                                                                          SaslAuthenticateRequestData request,
                                                                          FilterContext context) {

        if (this.mechanismAgreed && "PLAIN".equals(this.proposedMechanism)) {
            var tokens = extractTokens(new String(request.authBytes(), StandardCharsets.UTF_8));
            String authorizationIdFromClient = tokens.get(0);
            String username = tokens.get(1);
            LOGGER.info("Client '{}' on channel {} sent PLAIN authorizationId '{}' username '{}'; forwarding to server",
                    header.clientId(),
                    context.channelDescriptor(),
                    authorizationIdFromClient,
                    username);
            this.authorizationIdFromClient = authorizationIdFromClient.isEmpty() ? username : authorizationIdFromClient;
            return context.forwardRequest(header, request);
        }
        else {
            LOGGER.info("Client '{}' on channel {} sent SaslAuthenticateRequest without a prior SaslHandshake",
                    header.clientId(),
                    context.channelDescriptor());
            return context.requestFilterResultBuilder().shortCircuitResponse(
                    new SaslAuthenticateResponseData()
                            .setErrorCode(Errors.SASL_AUTHENTICATION_FAILED.code())
                            .setErrorMessage("SaslHandshake has not been performed"))
                    .completed();
        }
    }

    @Override
    public CompletionStage<ResponseFilterResult> onSaslAuthenticateResponse(short apiVersion,
                                                                            ResponseHeaderData header,
                                                                            SaslAuthenticateResponseData response,
                                                                            FilterContext context) {
        if (response.errorCode() == Errors.NONE.code()) {
            LOGGER.info("Server accepts SASL credentials for client on channel {}",
                    context.channelDescriptor());
            if (producePrincipal) {
                LOGGER.info("Client on channel {} has authorizationId {}",
                        context.channelDescriptor(),
                        this.authorizationIdFromClient);
                context.clientSaslAuthenticationSuccess(proposedMechanism, this.authorizationIdFromClient);
            }
        }
        else {
            Errors error = Errors.forCode(response.errorCode());
            LOGGER.info("Server rejects SASL credentials with error {} for client on channel {}",
                    error.name(), context.channelDescriptor());
            context.clientSaslAuthenticationFailure(proposedMechanism, this.authorizationIdFromClient, error.exception());
        }

        resetState();
        return context.forwardResponse(header, response);
    }

}
