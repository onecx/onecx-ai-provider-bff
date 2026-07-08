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

// Client model classes (for MockServer responses)
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
// BFF API DTOs (for RestAssured requests/responses)
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@LogService
@TestHTTPEndpoint(AgentRestController.class)
class AgentRestControllerTest extends AbstractTest {

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
    void getAgent_200Test() {
        AgentInternal fakeData = new AgentInternal()
                .id("1")
                .name("agent1")
                .description("desc")
                .additionalPrompt("prompt")
                .a2aEnabled(true)
                .status(AgentStatusInternal.LIVE)
                .model(new ModelInternal().name("model1").provider(new ProviderInternal().name("provider1")))
                .scaffold(new ScaffoldInternal().name("scaffold1").skills(List.of(new SkillInternal().name("skill1"))))
                .tools(List.of(new ToolInternal().name("tool1")))
                .groups(List.of(new AgentGroupInternal().name("group1")));

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + testId)
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
                .as(AgentDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
        Assertions.assertEquals(fakeData.getStatus().name(), response.getStatus().name());
        Assertions.assertEquals("model1", response.getModel().getName());
        Assertions.assertEquals("provider1", response.getModel().getProvider().getName());
        Assertions.assertEquals("scaffold1", response.getScaffold().getName());
        Assertions.assertEquals("skill1", response.getScaffold().getSkills().get(0).getName());
        Assertions.assertEquals("tool1", response.getTools().get(0).getName());
        Assertions.assertEquals("group1", response.getGroups().get(0).getName());
    }

    @Test
    void searchAgentTest() {
        AgentSearchCriteriaInternal internalCriteria = new AgentSearchCriteriaInternal();
        internalCriteria.setName("agent1");

        AgentSearchCriteriaDTO criteria = new AgentSearchCriteriaDTO();
        criteria.setName("agent1");

        AgentAbstractInternal agent1 = new AgentAbstractInternal()
                .id("1").name("agent1").description("desc").status(AgentStatusInternal.DRAFT).a2aEnabled(false);

        AgentPageResultInternal pageResult = new AgentPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(List.of(agent1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/agents/search")
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
                .as(AgentPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(pageResult.getSize(), results.getSize());
        Assertions.assertEquals(agent1.getName(), results.getStream().get(0).getName());
        Assertions.assertEquals(agent1.getStatus().name(), results.getStream().get(0).getStatus().name());
    }

    @Test
    void createAgentTest() {
        CreateAgentRequestInternal createRequest = new CreateAgentRequestInternal();
        createRequest.setName("New Agent");
        createRequest.setDescription("desc");
        createRequest.setAdditionalPrompt("prompt");
        createRequest.setA2aEnabled(true);
        createRequest.setStatus(AgentStatusInternal.DRAFT);

        AgentInternal responseAgent = new AgentInternal();
        responseAgent.setId("new-id");
        responseAgent.setName("New Agent");

        mockServerClient.when(
                request().withPath("/internal/agents")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseAgent)));

        CreateAgentRequestDTO createDTO = new CreateAgentRequestDTO();
        createDTO.setName("New Agent");
        createDTO.setDescription("desc");
        createDTO.setAdditionalPrompt("prompt");
        createDTO.setA2aEnabled(true);
        createDTO.setStatus(AgentStatusDTO.DRAFT);

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
                .as(AgentDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseAgent.getName(), response.getName());
    }

    @Test
    void updateAgentTest() {
        UpdateAgentRequestInternal updateRequest = new UpdateAgentRequestInternal();
        updateRequest.setName("Updated Agent");
        updateRequest.setDescription("desc");
        updateRequest.setStatus(AgentStatusInternal.LIVE);
        updateRequest.setModificationCount(0);

        AgentInternal responseAgent = new AgentInternal();
        responseAgent.setId("1");
        responseAgent.setName("Updated Agent");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + testId)
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseAgent)));

        UpdateAgentRequestDTO updateDTO = new UpdateAgentRequestDTO();
        updateDTO.setName("Updated Agent");
        updateDTO.setDescription("desc");
        updateDTO.setStatus(AgentStatusDTO.LIVE);
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
                .as(AgentDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseAgent.getName(), response.getName());
    }

    @Test
    void updateAgentTest_400_ConstraintException() {
        UpdateAgentRequestDTO updateDTO = new UpdateAgentRequestDTO();
        updateDTO.setName("Updated Agent");

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
    void deleteAgentTest() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + testId)
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
    void deleteAgent_ClientException_Test() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + testId)
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
