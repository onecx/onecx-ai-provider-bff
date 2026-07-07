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

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@LogService
@TestHTTPEndpoint(ExternalAgentRestController.class)
class ExternalAgentRestControllerTest extends AbstractTest {

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
    void getExternalAgentById_200Test() {
        ExternalAgentInternal fakeData = new ExternalAgentInternal()
                .id("1").name("ext1").description("desc").discoveryUrl("http://ext").apiKey("key")
                .authMode(AuthModeInternal.API_KEY).enabled(true)
                .groups(List.of(new AgentGroupInternal().name("group1")));

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/externalAgents/" + testId)
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(fakeData)));

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get(testId)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(ExternalAgentDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
        Assertions.assertEquals(fakeData.getAuthMode().name(), response.getAuthMode().name());
        Assertions.assertEquals("group1", response.getGroups().get(0).getName());
    }

    @Test
    void searchExternalAgentTest() {
        ExternalAgentSearchCriteriaInternal internalCriteria = new ExternalAgentSearchCriteriaInternal();
        internalCriteria.setName("ext1");

        ExternalAgentSearchCriteriaDTO criteria = new ExternalAgentSearchCriteriaDTO();
        criteria.setName("ext1");

        ExternalAgentInternal ext1 = new ExternalAgentInternal().id("1").name("ext1").enabled(true);

        ExternalAgentPageResultInternal pageResult = new ExternalAgentPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(List.of(ext1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/externalAgents/search")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(criteria)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(pageResult)));

        var results = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(internalCriteria)
                .post("/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(ExternalAgentPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(pageResult.getSize(), results.getSize());
        Assertions.assertEquals(ext1.getName(), results.getStream().get(0).getName());
    }

    @Test
    void createExternalAgentTest() {
        CreateExternalAgentRequestInternal createRequest = new CreateExternalAgentRequestInternal();
        createRequest.setName("New Ext");
        createRequest.setDescription("desc");
        createRequest.setDiscoveryUrl("http://ext");
        createRequest.setApiKey("key");
        createRequest.setAuthMode(AuthModeInternal.OAUTH);
        createRequest.setEnabled(true);
        createRequest.setGroupIds(List.of("group-1"));

        ExternalAgentInternal responseExt = new ExternalAgentInternal();
        responseExt.setId("new-id");
        responseExt.setName("New Ext");

        mockServerClient.when(
                request().withPath("/internal/externalAgents")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseExt)));

        CreateExternalAgentRequestDTO createDTO = new CreateExternalAgentRequestDTO();
        createDTO.setName("New Ext");
        createDTO.setDescription("desc");
        createDTO.setDiscoveryUrl("http://ext");
        createDTO.setApiKey("key");
        createDTO.setAuthMode(AuthModeDTO.OAUTH);
        createDTO.setEnabled(true);
        createDTO.setGroupIds(List.of("group-1"));

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(createDTO)
                .post()
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .as(ExternalAgentDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseExt.getName(), response.getName());
    }

    @Test
    void updateExternalAgentTest() {
        UpdateExternalAgentRequestInternal updateRequest = new UpdateExternalAgentRequestInternal();
        updateRequest.setName("Updated Ext");
        updateRequest.setEnabled(false);
        updateRequest.setGroupIds(List.of("group-1"));
        updateRequest.setModificationCount(0);

        ExternalAgentInternal responseExt = new ExternalAgentInternal();
        responseExt.setId("1");
        responseExt.setName("Updated Ext");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/externalAgents/" + testId)
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseExt)));

        UpdateExternalAgentRequestDTO updateDTO = new UpdateExternalAgentRequestDTO();
        updateDTO.setName("Updated Ext");
        updateDTO.setEnabled(false);
        updateDTO.setGroupIds(List.of("group-1"));
        updateDTO.setModificationCount(0);

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateDTO)
                .put(testId)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(ExternalAgentDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseExt.getName(), response.getName());
    }

    @Test
    void updateExternalAgentTest_400_ConstraintException() {
        UpdateExternalAgentRequestDTO updateDTO = new UpdateExternalAgentRequestDTO();
        updateDTO.setName("Updated Ext");

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateDTO)
                .put("1")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void deleteExternalAgentTest() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/externalAgents/" + testId)
                        .withMethod(HttpMethod.DELETE))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete(testId)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void deleteExternalAgent_ClientException_Test() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/externalAgents/" + testId)
                        .withMethod(HttpMethod.DELETE))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete(testId)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
