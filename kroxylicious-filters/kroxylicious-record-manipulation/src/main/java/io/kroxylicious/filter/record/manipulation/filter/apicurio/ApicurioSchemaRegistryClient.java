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

import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.ContentId;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.ContentResult;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.GavCoordinates;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.GlobalId;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.SchemaRegistryClient;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.SchemaRegistryClientException;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.ContentNotFoundException;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.UnknownGlobalIdException;
import io.kroxylicious.filter.record.manipulation.filter.apicurio.api.VersionsResult;
import io.kroxylicious.proxy.config.tls.InsecureTls;
import io.kroxylicious.proxy.config.tls.Tls;
import io.kroxylicious.proxy.tag.VisibleForTesting;

import edu.umd.cs.findbugs.annotations.Nullable;

public class ApicurioSchemaRegistryClient implements SchemaRegistryClient, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApicurioSchemaRegistryClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final URI apiBaseUri;
    private final Duration timeout;
    private final HttpClient httpClient;

    public ApicurioSchemaRegistryClient(URI registryUrl, Duration timeout, @Nullable Tls tls) {
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

    private URI contentUri(ContentId contentId) {
        return apiBaseUri.resolve("ids/contentIds/" + contentId.value() + "?returnArtifactType=true");
    }

    private URI contentUri(GavCoordinates coordinates) {
        // GET /apis/registry/v3/groups/{groupId}/artifacts/{artifactId}/versions/{version}/content
        // TODO we need to quote the coordinates before we use them to construct a path
        return apiBaseUri.resolve(
                "groups/" + coordinates.groupId()
                + "/artifacts/" + coordinates.artifactId()
                + "/versions/" + coordinates.version()
                + "/content?returnArtifactType=true");
    }

    private URI searchVersionsUri(GlobalId globalId) {
        return apiBaseUri.resolve("search/versions?globalId=" + globalId.value() + "&limit=1");
    }

    private HttpClient createClient(@Nullable Tls tls) {
        var builder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(timeout);
        if (tls != null && tls.trust() instanceof InsecureTls(boolean insecure) && insecure) {
            LOGGER.atDebug().log("Skipping TLS certificate verification - insecure mode");
        }
        return builder.build();
    }

    @Override
    public CompletionStage<ContentResult> contentById(ContentId contentId) {
        try {
            var request = HttpRequest.newBuilder(contentUri(contentId)).timeout(timeout).GET().build();
            return send(request)
                    .thenApply(response -> checkContentStatus(contentId, response))
                    .thenApply(response -> new ContentResult(
                            response.headers().firstValue("X-Registry-ArtifactType").orElse(null),
                            response.body()));
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new SchemaRegistryClientException("Failed to build request for content identified by " + contentId, e));
        }
    }


    @Override
    public CompletionStage<ContentResult> contentByGav(GavCoordinates coordinates) {
        try {
            var request = HttpRequest.newBuilder(contentUri(coordinates)).timeout(timeout).GET().build();
            return send(request)
                    .thenApply(response -> checkContentStatus(coordinates, response))
                    .thenApply(response -> new ContentResult(
                            response.headers().firstValue("X-Registry-ArtifactType").orElse(null),
                            response.body()));
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new SchemaRegistryClientException("Failed to build request for content identified by " + coordinates, e));
        }
    }


    @Override
    public CompletionStage<VersionsResult> versionsForGlobalId(GlobalId globalId) {
        try {
            var request = HttpRequest.newBuilder(searchVersionsUri(globalId)).timeout(timeout).GET().build();
            return send(request)
                    .thenApply(response -> checkSearchStatus(globalId, response))
                    .thenApply(HttpResponse::body)
                    .thenApply(ApicurioSchemaRegistryClient::decodeVersionSearchResults)
                    .thenApply(results -> firstVersionOrThrow(globalId, results));
        }
        catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new SchemaRegistryClientException("Failed to build request for globalId " + globalId.value(), e));
        }
    }

    private CompletableFuture<HttpResponse<byte[]>> send(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpResponse<byte[]> checkContentStatus(Record identifier, HttpResponse<byte[]> response) {
        if (response.statusCode() == 404) {
            LOGGER.atWarn()
                    .addKeyValue("identifiedBy", identifier)
                    .addKeyValue("requestUrl", response.request().uri())
                    .addKeyValue("statusCode", 404)
                    .log("Schema content not found in Apicurio registry");
            throw new ContentNotFoundException(identifier);
        }
        if (response.statusCode() != 200) {
            LOGGER.atWarn()
                    .addKeyValue("identifiedBy", identifier)
                    .addKeyValue("requestUrl", response.request().uri())
                    .addKeyValue("statusCode", response.statusCode())
                    .log("Failed to retrieve schema content from Apicurio registry");
            throw new SchemaRegistryClientException("Failed to retrieve content identified by %s, HTTP status code %d.".formatted(identifier, response.statusCode()));
        }
        return response;
    }

    private static HttpResponse<byte[]> checkSearchStatus(GlobalId globalId, HttpResponse<byte[]> response) {
        if (response.statusCode() != 200) {
            LOGGER.atWarn()
                    .addKeyValue("globalId", globalId.value())
                    .addKeyValue("requestUrl", response.request().uri())
                    .addKeyValue("statusCode", response.statusCode())
                    .log("Failed to search Apicurio registry for globalId");
            throw new SchemaRegistryClientException("Failed to search for globalId %d, HTTP status code %d.".formatted(globalId.value(), response.statusCode()));
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

    private static VersionsResult firstVersionOrThrow(GlobalId globalId, VersionSearchResults results) {
        if (results.versions().isEmpty()) {
            throw new UnknownGlobalIdException(globalId);
        }
        var v = results.versions().get(0);
        return new VersionsResult(globalId, v.groupId(), v.artifactId(), v.version(), new ContentId(v.contentId()));
    }

    @VisibleForTesting
    HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
