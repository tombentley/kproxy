/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.filter.apicurio;

import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class ApicurioSchemaRegistryClientTest {

    private static final Duration TIMEOUT = Duration.ofMillis(500);
    private static WireMockServer server;
    private ApicurioSchemaRegistryClient client;

    @BeforeAll
    static void initMockServer() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
    }

    @AfterAll
    static void shutdownMockServer() {
        server.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        client = new ApicurioSchemaRegistryClient(URI.create(server.baseUrl()), TIMEOUT, null);
    }

    @AfterEach
    void afterEach() {
        server.resetAll();
    }

    @Test
    void resolvesVersionsForKnownGlobalId() {
        // Given
        String response = """
                {"versions": [{"groupId": "com.myco", "artifactId": "orders", "version": "1", "globalId": 100, "contentId": 42}]}
                """;
        server.stubFor(get(urlPathEqualTo("/apis/registry/v3/search/versions"))
                .withQueryParam("globalId", com.github.tomakehurst.wiremock.client.WireMock.equalTo("100"))
                .withQueryParam("limit", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(response)));

        // When / Then
        assertThat(client.versionsForGlobalId(new GlobalId(100L)))
                .succeedsWithin(Duration.ofSeconds(5))
                .satisfies(result -> {
                    assertThat(result.groupId()).isEqualTo("com.myco");
                    assertThat(result.artifactId()).isEqualTo("orders");
                    assertThat(result.version()).isEqualTo("1");
                    assertThat(result.globalId()).isEqualTo(new GlobalId(100L));
                    assertThat(result.contentId()).isEqualTo(new ContentId(42L));
                });
    }

    @Test
    void failsWhenGlobalIdHasNoVersions() {
        // Given
        String response = """
                {"versions": []}
                """;
        server.stubFor(get(urlPathEqualTo("/apis/registry/v3/search/versions"))
                .withQueryParam("globalId", com.github.tomakehurst.wiremock.client.WireMock.equalTo("999"))
                .withQueryParam("limit", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(response)));

        // When / Then
        assertThat(client.versionsForGlobalId(new GlobalId(999L)))
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableThat()
                .withCauseInstanceOf(UnknownGlobalIdException.class)
                .withMessageContaining("globalId '999' is not found");
    }

    @Test
    void failsWhenGlobalIdSearchReturnsServerError() {
        // Given
        server.stubFor(get(urlPathEqualTo("/apis/registry/v3/search/versions"))
                .withQueryParam("globalId", com.github.tomakehurst.wiremock.client.WireMock.equalTo("100"))
                .withQueryParam("limit", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1"))
                .willReturn(aResponse().withStatus(500)));

        // When / Then
        assertThat(client.versionsForGlobalId(new GlobalId(100L)))
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableThat()
                .withCauseInstanceOf(SchemaRegistryClientException.class)
                .withMessageContaining("500");
    }

    @Test
    void resolvesContentForKnownContentId() {
        // Given
        byte[] contentBytes = { 0x01, 0x02, 0x03, 0x04 };
        server.stubFor(get(urlEqualTo("/apis/registry/v3/ids/contentIds/62?returnArtifactType=true"))
                .willReturn(aResponse().withBody(contentBytes)));

        // When / Then
        assertThat(client.contentById(new ContentId(62L)))
                .succeedsWithin(Duration.ofSeconds(5))
                .satisfies(result -> {
                    assertThat(result.contentId()).isEqualTo(new ContentId(62L));
                    assertThat(result.contentBytes()).isEqualTo(contentBytes);
                });
    }

    @Test
    void failsWhenContentIdNotFound() {
        // Given
        server.stubFor(get(urlEqualTo("/apis/registry/v3/ids/contentIds/404?returnArtifactType=true"))
                .willReturn(aResponse().withStatus(404)));

        // When / Then
        assertThat(client.contentById(new ContentId(404L)))
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableThat()
                .withCauseInstanceOf(UnknownContentIdException.class)
                .withMessageContaining("contentId '404' is not found");
    }

    @Test
    void failsWhenContentLookupReturnsServerError() {
        // Given
        server.stubFor(get(urlEqualTo("/apis/registry/v3/ids/contentIds/62?returnArtifactType=true"))
                .willReturn(aResponse().withStatus(500)));

        // When / Then
        assertThat(client.contentById(new ContentId(62L)))
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableThat()
                .withCauseInstanceOf(SchemaRegistryClientException.class)
                .withMessageContaining("500");
    }

    @Test
    void doesNotBlockWhileRegistryResponseIsDelayed() {
        // Given
        var longTimeoutClient = new ApicurioSchemaRegistryClient(URI.create(server.baseUrl()), Duration.ofSeconds(10), null);
        byte[] contentBytes = { 0x05, 0x06 };
        server.stubFor(get(urlEqualTo("/apis/registry/v3/ids/contentIds/62?returnArtifactType=true"))
                .willReturn(aResponse().withBody(contentBytes).withFixedDelay(100)));

        // When
        var stage = longTimeoutClient.contentById(new ContentId(62L));

        // Then - immediately assert it's not done
        assertThat(stage.toCompletableFuture()).isNotDone();

        // And eventually it succeeds
        assertThat(stage)
                .succeedsWithin(Duration.ofSeconds(5))
                .satisfies(result -> assertThat(result.contentBytes()).isEqualTo(contentBytes));
    }

    @Test
    void acceptsBaseUrlWithoutPath() {
        // Given
        var testClient = new ApicurioSchemaRegistryClient(URI.create(server.baseUrl()), TIMEOUT, null);
        server.stubFor(get(urlEqualTo("/apis/registry/v3/ids/contentIds/1?returnArtifactType=true"))
                .willReturn(aResponse().withBody(new byte[]{ 0x01 })));

        // When / Then
        assertThat(testClient.contentById(new ContentId(1L)))
                .succeedsWithin(Duration.ofSeconds(5));
    }

    @Test
    void acceptsBaseUrlWithTrailingSlash() {
        // Given
        var testClient = new ApicurioSchemaRegistryClient(URI.create(server.baseUrl() + "/"), TIMEOUT, null);
        server.stubFor(get(urlEqualTo("/apis/registry/v3/ids/contentIds/1?returnArtifactType=true"))
                .willReturn(aResponse().withBody(new byte[]{ 0x01 })));

        // When / Then
        assertThat(testClient.contentById(new ContentId(1L)))
                .succeedsWithin(Duration.ofSeconds(5));
    }

    @Test
    void acceptsBaseUrlWithPathPrefix() {
        // Given
        var testClient = new ApicurioSchemaRegistryClient(URI.create(server.baseUrl() + "/registry-prefix"), TIMEOUT, null);
        server.stubFor(get(urlEqualTo("/registry-prefix/apis/registry/v3/ids/contentIds/1?returnArtifactType=true"))
                .willReturn(aResponse().withBody(new byte[]{ 0x01 })));

        // When / Then
        assertThat(testClient.contentById(new ContentId(1L)))
                .succeedsWithin(Duration.ofSeconds(5));
    }

    @Test
    void appliesConnectionTimeout() {
        var uri = URI.create("http://test:8080");
        var testClient = new ApicurioSchemaRegistryClient(uri, TIMEOUT, null);
        assertThat(testClient.getHttpClient().connectTimeout()).hasValue(TIMEOUT);
    }

    @Test
    void malformedJsonResponseCausesFailure() {
        // Given
        server.stubFor(get(urlPathEqualTo("/apis/registry/v3/search/versions"))
                .withQueryParam("globalId", com.github.tomakehurst.wiremock.client.WireMock.equalTo("100"))
                .withQueryParam("limit", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1"))
                .willReturn(aResponse().withBody("not json")));

        // When / Then
        assertThat(client.versionsForGlobalId(new GlobalId(100L)))
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableThat()
                .withCauseInstanceOf(UncheckedIOException.class)
                .withMessageContaining("Failed to decode Apicurio version search response as JSON");
    }
}
