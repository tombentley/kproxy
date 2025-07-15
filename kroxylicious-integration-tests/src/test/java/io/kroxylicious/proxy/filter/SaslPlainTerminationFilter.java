/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.filter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.message.RequestHeaderData;
import org.apache.kafka.common.message.SaslAuthenticateRequestData;
import org.apache.kafka.common.message.SaslAuthenticateResponseData;
import org.apache.kafka.common.message.SaslHandshakeRequestData;
import org.apache.kafka.common.message.SaslHandshakeResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ApiMessage;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.apache.kafka.common.security.plain.internals.PlainServerCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kroxylicious.proxy.internal.KafkaAuthnHandler;

public class SaslPlainTerminationFilter
        implements RequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaslPlainTerminationFilter.class);

    private SaslServer saslServer;

    private PlainServerCallbackHandler saslPlainCallbackHandler(String user,
                                                                String password) {
        PlainServerCallbackHandler plainServerCallbackHandler = new PlainServerCallbackHandler();
        plainServerCallbackHandler.configure(Map.of(),
                KafkaAuthnHandler.SaslMechanism.PLAIN.mechanismName(),
                List.of(new AppConfigurationEntry(PlainLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        Map.of("user_" + user, password))));
        return plainServerCallbackHandler;
    }

    CompletionStage<RequestFilterResult> onSaslHandshakeRequest(short apiVersion,
                                                                RequestHeaderData header,
                                                                SaslHandshakeRequestData request,
                                                                FilterContext context) {
        Errors errorCode = Errors.UNSUPPORTED_SASL_MECHANISM;
        if ("PLAIN".equals(request.mechanism())) {
            PlainServerCallbackHandler cbh = saslPlainCallbackHandler("alice", "alice-secret");
            try {
                saslServer = Sasl.createSaslServer(request.mechanism(), "kafka", null, null, cbh);
                if (saslServer == null) {
                    throw new IllegalStateException("SASL mechanism had no providers: " + request.mechanism());
                }
                errorCode = Errors.NONE;
            }
            catch (SaslException e) {
                throw new RuntimeException(e);
            }
        }
        return context.requestFilterResultBuilder()
                .shortCircuitResponse(new SaslHandshakeResponseData()
                        .setErrorCode(errorCode.code())
                        .setMechanisms(List.of("PLAIN")))
                .completed();
    }

    CompletionStage<RequestFilterResult> onSaslAuthenticateRequest(short apiVersion,
                                                                   RequestHeaderData header,
                                                                   SaslAuthenticateRequestData request,
                                                                   FilterContext context) {
        byte[] bytes = new byte[0];
        Errors error;
        String errorMessage;

        try {
            bytes = doEvaluateResponse(context, request.authBytes());
            error = Errors.NONE;
            errorMessage = null;
        }
        catch (SaslAuthenticationException e) {
            error = Errors.SASL_AUTHENTICATION_FAILED;
            errorMessage = e.getMessage();
            context.clientSaslAuthenticationFailure(null, null, e);
        }
        catch (SaslException e) {
            error = Errors.SASL_AUTHENTICATION_FAILED;
            errorMessage = "An error occurred";
            context.clientSaslAuthenticationFailure(null, null, e);
        }

        SaslAuthenticateResponseData body = new SaslAuthenticateResponseData()
                .setErrorCode(error.code())
                .setErrorMessage(errorMessage)
                .setAuthBytes(bytes);

        return context.requestFilterResultBuilder().shortCircuitResponse(body).completed();
    }

    private byte[] doEvaluateResponse(FilterContext context,
                                      byte[] authBytes)
            throws SaslException {
        final byte[] bytes;
        try {
            bytes = saslServer.evaluateResponse(authBytes);
        }
        catch (SaslAuthenticationException e) {
            LOGGER.debug("{}: Authentication failed", context.channelDescriptor());
            saslServer.dispose();
            throw e;
        }
        catch (Exception e) {
            LOGGER.debug("{}: Authentication failed", context.channelDescriptor());
            saslServer.dispose();
            throw new SaslAuthenticationException(e.getMessage());
        }

        if (saslServer.isComplete()) {
            try {
                String authorizationId = saslServer.getAuthorizationID();
                // var properties = KafkaAuthnHandler.SaslMechanism.fromMechanismName(saslServer.getMechanismName()).negotiatedProperties(saslServer);
                LOGGER.debug("{}: Authentication successful, authorizationId={}", context.channelDescriptor(), authorizationId);
                context.clientSaslAuthenticationSuccess(saslServer.getMechanismName(), authorizationId);
            }
            finally {
                saslServer.dispose();
            }
        }
        return bytes;
    }

    @Override
    public CompletionStage<RequestFilterResult> onRequest(ApiKeys apiKey,
                                                          RequestHeaderData header,
                                                          ApiMessage request,
                                                          FilterContext context) {
        return switch (apiKey) {
            case API_VERSIONS -> context.forwardRequest(header, request);
            case SASL_HANDSHAKE -> onSaslHandshakeRequest(header.requestApiVersion(), header, (SaslHandshakeRequestData) request, context);
            case SASL_AUTHENTICATE -> onSaslAuthenticateRequest(header.requestApiVersion(), header, (SaslAuthenticateRequestData) request, context);
            default -> {
                if (context.clientSaslContext().isPresent()) {
                    yield context.forwardRequest(header, request);
                }
                else {
                    yield context.requestFilterResultBuilder()
                            .errorResponse(header, request, Errors.CLUSTER_AUTHORIZATION_FAILED.exception())
                            .completed();
                }
            }
        };
    }
}
