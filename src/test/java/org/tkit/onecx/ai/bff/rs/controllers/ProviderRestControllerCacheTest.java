package org.tkit.onecx.ai.bff.rs.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

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
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ProviderHealthStatusDTO;
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
        String testId = "1";

        ProviderHealthStatusInternal status = new ProviderHealthStatusInternal()
                .status(ProviderHealthStatusInternal.StatusEnum.HEALTHY);

        // create mock rest endpoint for permission svc
        mockServerClient.when(
                request().withPath("/internal/providers/" + testId + "/health")
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId("mockHealthCheck")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(status)));

        var output = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .get(testId + "/health")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(ProviderHealthStatusDTO.class);

        Assertions.assertNotNull(output);
        Assertions.assertEquals(status.getStatus().toString(), output.getStatus().toString());
        mockServerClient.clear("mockHealthCheck");
    }
}
