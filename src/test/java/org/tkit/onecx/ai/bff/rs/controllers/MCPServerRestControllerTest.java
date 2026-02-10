package org.tkit.onecx.ai.bff.rs.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

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
// Client model classes (for MockServer responses)
// BFF API DTOs (for RestAssured requests/responses)
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@LogService
@TestHTTPEndpoint(MCPServerRestController.class)
class MCPServerRestControllerTest extends AbstractTest {

    private static final String AI_KNOWLEDGE_BASE_API_BASE_PATH = "/internal/ai/ai-knowledgebases";

    @InjectMockServerClient
    MockServerClient mockServerClient;

    KeycloakTestClient keycloakTestClient = new KeycloakTestClient();
    static final String MOCK_ID = "MOCK";

    @AfterEach
    void resetMockserver() {
        try {
            mockServerClient.clear(MOCK_ID);
        } catch (Exception ex) {
            // mockId not existing
        }
    }

    @Test
    void getMCPServerById_200Test() {
        MCPServerInternal fakeData = new MCPServerInternal().name("mcp1").url("url");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/mcpServer/" + testId)
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
                .get("/" + testId)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(MCPServerDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
    }

    @Test
    void getMCPServerById_404Test() {
        String testId = "non-existent-id";

        mockServerClient.when(
                request().withPath("/internal/mcpServer/" + testId)
                        .withMethod(HttpMethod.GET))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/" + testId)
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void deleteMCPServerTest() {
        String id = "1";
        mockServerClient.when(
                request().withPath("/internal/mcpServer/" + id)
                        .withMethod(HttpMethod.DELETE))
                .withId(MOCK_ID)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete("/" + id)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void updateMCPServerTest() {

        UpdateMCPServerRequestInternal requestInternal = new UpdateMCPServerRequestInternal().name("Updated MCP")
                .description("desc").modificationCount(0);
        MCPServerInternal responseMCP = new MCPServerInternal().name("Updated MCP").description("desc");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/mcpServer/" + testId)
                        .withMethod(HttpMethod.PUT).withBody(JsonBody.json(requestInternal)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseMCP)));

        UpdateMCPServerRequestDTO requestDTO = new UpdateMCPServerRequestDTO().name("Updated MCP").description("desc")
                .modificationCount(0);
        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .put("/" + testId)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(MCPServerDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseMCP.getName(), response.getName());
    }

    @Test
    void updateMCPServer_400Test() {

        String testId = "1";
        UpdateMCPServerRequestDTO requestDTO = new UpdateMCPServerRequestDTO();
        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .put("/" + testId)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void updateMCPServer_404Test() {

        UpdateProviderRequestInternal requestInternal = new UpdateProviderRequestInternal().modelName("Updated MCP")
                .modificationCount(0);

        String testId = "non-existent-id";

        mockServerClient.when(
                request().withPath("/internal/mcpServer/" + testId)
                        .withMethod(HttpMethod.PUT).withBody(JsonBody.json(requestInternal)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        UpdateMCPServerRequestDTO updateDTO = new UpdateMCPServerRequestDTO();
        updateDTO.setName("Updated MCP");
        updateDTO.setModificationCount(0);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateDTO)
                .put("/" + testId)
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void createMCPServerTest() {

        CreateMCPServerRequestInternal createRequest = new CreateMCPServerRequestInternal();
        createRequest.setName("name");
        createRequest.setDescription("desc");
        createRequest.setUrl("url");

        MCPServerInternal responseMcp = new MCPServerInternal().name("name").description("desc").url("url");

        mockServerClient.when(
                request().withPath("/internal/mcpServer")
                        .withMethod(HttpMethod.POST).withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseMcp)));

        CreateMCPServerRequestDTO createDTO = new CreateMCPServerRequestDTO();
        createDTO.setName("name");
        createDTO.setDescription("desc");
        createDTO.setUrl("url");
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
                .as(MCPServerDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(createRequest.getName(), response.getName());
    }

    @Test
    void createMCPServer_400Test() {

        CreateMCPServerRequestInternal createRequest = new CreateMCPServerRequestInternal();
        createRequest.setName("abc");

        mockServerClient.when(
                request().withPath("/internal/mcpServer")
                        .withMethod(HttpMethod.POST).withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        CreateMCPServerRequestDTO createDTO = new CreateMCPServerRequestDTO();
        createDTO.setName("abc");

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(createDTO)
                .post()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void createMCPServerWithoutBody_ConstraintViolation400Test() {
        // Test constraintException by sending POST request without body
        var createResponseException = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post()
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .as(ProblemDetailResponseDTO.class);

        Assertions.assertNotNull(createResponseException);
        Assertions.assertEquals("CONSTRAINT_VIOLATIONS", createResponseException.getErrorCode());
    }

    @Test
    void searchMCPServersTest() {
        MCPServerSearchCriteriaInternal criteria = new MCPServerSearchCriteriaInternal();
        criteria.setName("Test MCP");

        MCPServerInternal base1 = new MCPServerInternal().name("Test MCP");

        MCPServerPageResultInternal pageResult = new MCPServerPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(java.util.List.of(base1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/mcpServer/search")
                        .withMethod(HttpMethod.POST))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(pageResult)));

        MCPServerSearchCriteriaDTO criteriaDTO = new MCPServerSearchCriteriaDTO();
        criteriaDTO.setName("Test MCP");
        var results = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(criteriaDTO)
                .post("/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(MCPServerPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(1, results.getTotalElements());
        Assertions.assertEquals(base1.getName(), results.getStream().get(0).getName());
    }
}
