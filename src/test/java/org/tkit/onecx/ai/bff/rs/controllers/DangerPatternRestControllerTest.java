package org.tkit.onecx.ai.bff.rs.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.ai.bff.rs.AbstractTest;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.model.DangerLevelInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.DangerPatternInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.DangerPatternListInternal;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.CreateDangerPatternRequestDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.DangerLevelDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.DangerPatternDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.DangerPatternListDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.UpdateDangerPatternRequestDTO;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@LogService
@TestHTTPEndpoint(DangerPatternRestController.class)
class DangerPatternRestControllerTest extends AbstractTest {

    @InjectMockServerClient
    MockServerClient mockServerClient;

    KeycloakTestClient keycloakTestClient = new KeycloakTestClient();
    static final String MOCK_ID = "MOCK";

    @AfterEach
    void resetMockserver() {
        try {
            mockServerClient.clear(MOCK_ID);
        } catch (Exception _) {
            // mockId not existing
        }
    }

    @Test
    void getDangerPatterns_200Test() {
        DangerPatternInternal pattern = new DangerPatternInternal()
                .id("p1").pattern("delete").dangerLevel(DangerLevelInternal.DANGEROUS);
        DangerPatternListInternal patterns = new DangerPatternListInternal();
        patterns.setPatterns(List.of(pattern));

        mockServerClient.when(
                request().withPath("/internal/danger-patterns")
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(patterns)));

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DangerPatternListDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getPatterns().size());
        Assertions.assertEquals("delete", response.getPatterns().get(0).getPattern());
        Assertions.assertEquals(pattern.getDangerLevel().name(), response.getPatterns().get(0).getDangerLevel().name());
    }

    @Test
    void createDangerPattern_201Test() {
        DangerPatternInternal created = new DangerPatternInternal()
                .id("p1").pattern("purge").dangerLevel(DangerLevelInternal.DANGEROUS);

        mockServerClient.when(
                request().withPath("/internal/danger-patterns")
                        .withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(created)));

        CreateDangerPatternRequestDTO request = new CreateDangerPatternRequestDTO();
        request.setPattern("purge");
        request.setDangerLevel(DangerLevelDTO.DANGEROUS);

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(request)
                .post()
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .as(DangerPatternDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("p1", response.getId());
        Assertions.assertEquals(created.getDangerLevel().name(), response.getDangerLevel().name());
    }

    @Test
    void updateDangerPattern_404Test() {
        String id = "missing";
        mockServerClient.when(
                request().withPath("/internal/danger-patterns/" + id)
                        .withMethod(HttpMethod.PUT))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        UpdateDangerPatternRequestDTO request = new UpdateDangerPatternRequestDTO();
        request.setModificationCount(0);
        request.setPattern("purge");
        request.setDangerLevel(DangerLevelDTO.WARNING);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(request)
                .put(id)
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void deleteDangerPattern_204Test() {
        String id = "p1";
        mockServerClient.when(
                request().withPath("/internal/danger-patterns/" + id)
                        .withMethod(HttpMethod.DELETE))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete(id)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }
}
