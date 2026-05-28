package org.tkit.onecx.ai.bff.rs.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.ai.bff.rs.AbstractTest;
import org.tkit.onecx.ai.bff.rs.AiProviderConfig;

import gen.org.tkit.onecx.ai.management.bff.client.model.ProviderHealthStatusInternal;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ProviderHealthStatusRequestDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ProviderHealthStatusResponseDTO;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.smallrye.config.Config;
import io.smallrye.config.SmallRyeConfig;

@QuarkusTest
@TestHTTPEndpoint(ProviderRestController.class)
class ProviderRestControllerCacheTest extends AbstractTest {

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @InjectMock
    AiProviderConfig providerConfig;

    @Inject
    Config config;

    KeycloakTestClient keycloakTestClient = new KeycloakTestClient();

    public static class ConfigProducer {

        @Inject
        Config config;

        @Produces
        @ApplicationScoped
        @Mock
        AiProviderConfig config() {
            return config.unwrap(SmallRyeConfig.class).getConfigMapping(AiProviderConfig.class);
        }
    }

    @BeforeEach
    void beforeEach() {
        var tmp = config.unwrap(SmallRyeConfig.class).getConfigMapping(AiProviderConfig.class);

        Mockito.when(providerConfig.healthCheck()).thenReturn(new AiProviderConfig.HealthCheck() {

            @Override
            public boolean cacheEnabled() {
                return true;
            }

            @Override
            public String cacheExpireAfter() {
                return tmp.healthCheck().cacheExpireAfter();
            }

            @Override
            public boolean cacheMetricsEnabled() {
                return tmp.healthCheck().cacheMetricsEnabled();
            }

        });
    }

    @Test
    void getHealthCheckTest() {
        String firstId = "cache-multi-1";
        String secondId = "cache-multi-2";

        ProviderHealthStatusRequestDTO requestDTO = new ProviderHealthStatusRequestDTO();
        requestDTO.setProviderIds(java.util.List.of(firstId, secondId));

        ProviderHealthStatusInternal firstStatus = new ProviderHealthStatusInternal();
        firstStatus.setStatus(ProviderHealthStatusInternal.StatusEnum.HEALTHY);
        ProviderHealthStatusInternal secondStatus = new ProviderHealthStatusInternal();
        secondStatus.setStatus(ProviderHealthStatusInternal.StatusEnum.UNHEALTHY);

        // create mock rest endpoint for permission svc
        mockServerClient.when(
                request().withPath("/internal/providers/" + firstId + "/health")
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId("mockHealthCheck1")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(firstStatus)));

        mockServerClient.when(
                request().withPath("/internal/providers/" + secondId + "/health")
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId("mockHealthCheck2")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(secondStatus)));

        var firstOutput = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("/health")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(ProviderHealthStatusResponseDTO.class);

        var secondOutput = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("/health")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(ProviderHealthStatusResponseDTO.class);

        Assertions.assertNotNull(firstOutput);
        Assertions.assertNotNull(firstOutput.getProviderHealthStatuses());
        Assertions.assertEquals(2, firstOutput.getProviderHealthStatuses().size());
        Assertions.assertEquals(firstId, firstOutput.getProviderHealthStatuses().get(0).getProviderId());
        Assertions.assertEquals(secondId, firstOutput.getProviderHealthStatuses().get(1).getProviderId());
        Assertions.assertEquals(firstStatus.getStatus().toString(),
                firstOutput.getProviderHealthStatuses().get(0).getStatus().toString());
        Assertions.assertEquals(secondStatus.getStatus().toString(),
                firstOutput.getProviderHealthStatuses().get(1).getStatus().toString());

        Assertions.assertNotNull(secondOutput);
        Assertions.assertNotNull(secondOutput.getProviderHealthStatuses());
        Assertions.assertEquals(2, secondOutput.getProviderHealthStatuses().size());
        Assertions.assertEquals(firstId, secondOutput.getProviderHealthStatuses().get(0).getProviderId());
        Assertions.assertEquals(secondId, secondOutput.getProviderHealthStatuses().get(1).getProviderId());
        Assertions.assertEquals(firstStatus.getStatus().toString(),
                secondOutput.getProviderHealthStatuses().get(0).getStatus().toString());
        Assertions.assertEquals(secondStatus.getStatus().toString(),
                secondOutput.getProviderHealthStatuses().get(1).getStatus().toString());

        mockServerClient.verify(
                request().withPath("/internal/providers/" + firstId + "/health").withMethod(HttpMethod.GET),
                exactly(1));
        mockServerClient.verify(
                request().withPath("/internal/providers/" + secondId + "/health").withMethod(HttpMethod.GET),
                exactly(1));

        mockServerClient.clear("mockHealthCheck1");
        mockServerClient.clear("mockHealthCheck2");
    }
}
