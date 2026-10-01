/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kroxylicious.filter.record.manipulation.filter.SchemaRegistryClient;
import io.kroxylicious.proxy.config.tls.InsecureTls;
import io.kroxylicious.proxy.config.tls.Tls;
import io.kroxylicious.proxy.tag.VisibleForTesting;

public class ApicurioSchemaRegistryClient implements SchemaRegistryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApicurioSchemaRegistryClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final URI apiBaseUri;
    private final Duration timeout;
    private final HttpClient httpClient;

    public ApicurioSchemaRegistryClient(URI registryUrl, Duration timeout, Tls tls) {
        Objects.requireNonNull(registryUrl);
        Objects.requireNonNull(timeout);
        if (tls != null && tls.key() != null) {
            throw new IllegalArgumentException("Client TLS authentication (key) is not supported by the schema registry client");
        }
        if (tls != null && tls.trust() != null && !(tls.trust() instanceof InsecureTls)) {
            throw new IllegalArgumentException("Custom TLS trust configuration is not yet supported by the schema registry client");
        }
        this.apiBaseUri = ensureEndsInSlash(registryUrl).resolve("apis/registry/v3/");
        this.timeout = timeout;
        this.httpClient = createClient(tls);
    }

    private static URI ensureEndsInSlash(URI uri) {
        String path = uri.getPath();
        return path.endsWith("/") ? uri : uri.resolve(path + "/");
    }

    private URI contentUri(long contentId) {
        return apiBaseUri.resolve("ids/contentIds/" + contentId);
    }

    private URI searchVersionsUri(long globalId) {
        return apiBaseUri.resolve("search/versions?globalId=" + globalId + "&limit=1");
    }

    private HttpClient createClient(Tls tls) {
        var builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(timeout);
        if (tls != null && tls.trust() instanceof InsecureTls insecureTls && insecureTls.insecure()) {
            LOGGER.atDebug().log("Skipping TLS certificate verification - insecure mode");
        }
        return builder.build();
    }

    @Override
    public CompletionStage<ContentResult> getContentById(long contentId) {
        try {
            var request = HttpRequest.newBuilder(contentUri(contentId)).timeout(timeout).GET().build();
            return send(request)
                    .thenApply(response -> checkContentStatus(contentId, response))
                    .thenApply(response -> new ContentResult(contentId, response.body()));
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new SchemaRegistryClientException("Failed to build request for contentId " + contentId, e));
        }
    }

    @Override
    public CompletionStage<VersionsResult> artifactIdForGlobalId(long globalId) {
        try {
            var request = HttpRequest.newBuilder(searchVersionsUri(globalId)).timeout(timeout).GET().build();
            return send(request)
                    .thenApply(response -> checkSearchStatus(globalId, response))
                    .thenApply(HttpResponse::body)
                    .thenApply(ApicurioSchemaRegistryClient::decodeVersionSearchResults)
                    .thenApply(results -> firstVersionOrThrow(globalId, results));
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new SchemaRegistryClientException("Failed to build request for globalId " + globalId, e));
        }
    }

    private CompletableFuture<HttpResponse<byte[]>> send(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpResponse<byte[]> checkContentStatus(long contentId, HttpResponse<byte[]> response) {
        if (response.statusCode() == 404) {
            LOGGER.atWarn()
                    .addKeyValue("contentId", contentId)
                    .addKeyValue("requestUrl", response.request().uri())
                    .addKeyValue("statusCode", 404)
                    .log("Schema content not found in Apicurio registry");
            throw new UnknownContentIdException(contentId);
        }
        if (response.statusCode() != 200) {
            LOGGER.atWarn()
                    .addKeyValue("contentId", contentId)
                    .addKeyValue("requestUrl", response.request().uri())
                    .addKeyValue("statusCode", response.statusCode())
                    .log("Failed to retrieve schema content from Apicurio registry");
            throw new SchemaRegistryClientException("Failed to retrieve content id %d, HTTP status code %d.".formatted(contentId, response.statusCode()));
        }
        return response;
    }

    private static HttpResponse<byte[]> checkSearchStatus(long globalId, HttpResponse<byte[]> response) {
        if (response.statusCode() != 200) {
            LOGGER.atWarn()
                    .addKeyValue("globalId", globalId)
                    .addKeyValue("requestUrl", response.request().uri())
                    .addKeyValue("statusCode", response.statusCode())
                    .log("Failed to search Apicurio registry for globalId");
            throw new SchemaRegistryClientException("Failed to search for globalId %d, HTTP status code %d.".formatted(globalId, response.statusCode()));
        }
        return response;
    }

    private static VersionSearchResults decodeVersionSearchResults(byte[] bytes) {
        try {
            return OBJECT_MAPPER.readValue(bytes, VersionSearchResults.class);
        }
        catch (IOException e) {
            LOGGER.atWarn()
                    .setCause(LOGGER.isDebugEnabled() ? e : null)
                    .addKeyValue("error", e.getMessage())
                    .log(LOGGER.isDebugEnabled() ? "Failed to decode Apicurio search response as JSON"
                            : "Failed to decode Apicurio search response as JSON, increase log level to DEBUG for stacktrace");
            throw new UncheckedIOException("Failed to decode Apicurio version search response as JSON", e);
        }
    }

    private static VersionsResult firstVersionOrThrow(long globalId, VersionSearchResults results) {
        if (results.versions().isEmpty()) {
            throw new UnknownGlobalIdException(globalId);
        }
        var v = results.versions().get(0);
        return new VersionsResult(v.globalId(), v.groupId(), v.artifactId(), v.version(), v.contentId());
    }

    @VisibleForTesting
    HttpClient getHttpClient() {
        return httpClient;
    }
}
