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
@TestHTTPEndpoint(ToolRestController.class)
class ToolRestControllerTest extends AbstractTest {

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
    void getDiscoveredTools_200Test() {
        DiscoveredToolInfoInternal info = new DiscoveredToolInfoInternal()
                .name("importProposals")
                .description("Imports proposals")
                .autoDangerLevel(DangerLevelInternal.WARNING)
                .orphaned(false);
        DiscoveredToolInfoListInternal discovered = new DiscoveredToolInfoListInternal();
        discovered.setTools(List.of(info));

        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/tools/" + toolId + "/discovered-tools")
                        .withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(discovered)));

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post(toolId + "/discovered-tools")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(DiscoveredToolInfoListDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getTools().size());
        Assertions.assertEquals("importProposals", response.getTools().get(0).getName());
        Assertions.assertEquals(info.getAutoDangerLevel().name(), response.getTools().get(0).getAutoDangerLevel().name());
    }

    @Test
    void getDiscoveredTools_404Test() {
        String toolId = "missing";
        mockServerClient.when(
                request().withPath("/internal/tools/" + toolId + "/discovered-tools")
                        .withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .post(toolId + "/discovered-tools")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void getToolById_200Test() {
        ToolInternal fakeData = new ToolInternal()
                .id("1").name("tool1").description("desc").type(ToolTypeInternal.MCP)
                .url("http://tool").apiKey("key").executionPolicy(ExecutionPolicyInternal.ALWAYS_ASK)
                .authMode(AuthModeInternal.API_KEY);

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/tools/" + testId)
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
                .as(ToolDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
        Assertions.assertEquals(fakeData.getType().name(), response.getType().name());
        Assertions.assertEquals(fakeData.getExecutionPolicy().name(), response.getExecutionPolicy().name());
        Assertions.assertEquals(fakeData.getAuthMode().name(), response.getAuthMode().name());
    }

    @Test
    void searchToolTest() {
        ToolSearchCriteriaInternal internalCriteria = new ToolSearchCriteriaInternal();
        internalCriteria.setName("tool1");

        ToolSearchCriteriaDTO criteria = new ToolSearchCriteriaDTO();
        criteria.setName("tool1");

        ToolInternal tool1 = new ToolInternal().id("1").name("tool1").type(ToolTypeInternal.HTTP);

        ToolPageResultInternal pageResult = new ToolPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(List.of(tool1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/tools/search")
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
                .as(ToolPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(pageResult.getSize(), results.getSize());
        Assertions.assertEquals(tool1.getName(), results.getStream().get(0).getName());
    }

    @Test
    void createToolTest() {
        CreateToolRequestInternal createRequest = new CreateToolRequestInternal();
        createRequest.setName("New Tool");
        createRequest.setDescription("desc");
        createRequest.setType(ToolTypeInternal.MCP);
        createRequest.setUrl("http://tool");
        createRequest.setApiKey("key");
        createRequest.setExecutionPolicy(ExecutionPolicyInternal.NEVER_ASK);
        createRequest.setAuthMode(AuthModeInternal.OAUTH);

        ToolInternal responseTool = new ToolInternal();
        responseTool.setId("new-id");
        responseTool.setName("New Tool");

        mockServerClient.when(
                request().withPath("/internal/tools")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseTool)));

        CreateToolRequestDTO createDTO = new CreateToolRequestDTO();
        createDTO.setName("New Tool");
        createDTO.setDescription("desc");
        createDTO.setType(ToolTypeDTO.MCP);
        createDTO.setUrl("http://tool");
        createDTO.setApiKey("key");
        createDTO.setExecutionPolicy(ExecutionPolicyDTO.NEVER_ASK);
        createDTO.setAuthMode(AuthModeDTO.OAUTH);

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
                .as(ToolDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseTool.getName(), response.getName());
    }

    @Test
    void updateToolTest() {
        UpdateToolRequestInternal updateRequest = new UpdateToolRequestInternal();
        updateRequest.setName("Updated Tool");
        updateRequest.setType(ToolTypeInternal.CUSTOM);
        updateRequest.setModificationCount(0);

        ToolInternal responseTool = new ToolInternal();
        responseTool.setId("1");
        responseTool.setName("Updated Tool");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/tools/" + testId)
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseTool)));

        UpdateToolRequestDTO updateDTO = new UpdateToolRequestDTO();
        updateDTO.setName("Updated Tool");
        updateDTO.setType(ToolTypeDTO.CUSTOM);
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
                .as(ToolDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseTool.getName(), response.getName());
    }

    @Test
    void updateToolTest_400_ConstraintException() {
        UpdateToolRequestDTO updateDTO = new UpdateToolRequestDTO();
        updateDTO.setName("Updated Tool");

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
    void deleteToolTest() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/tools/" + testId)
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
    void deleteTool_ClientException_Test() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/tools/" + testId)
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
