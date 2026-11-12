package org.tkit.onecx.ai.bff.rs.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.ai.bff.rs.AbstractTest;
import org.tkit.quarkus.log.cdi.LogService;

// Client model classes (for MockServer responses)
import gen.org.tkit.onecx.ai.management.bff.client.api.AgentInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
// BFF API DTOs (for RestAssured requests/responses)
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
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
    void createAgentDuplicateNamePassesThroughProblemDetail() {
        CreateAgentRequestInternal createRequest = new CreateAgentRequestInternal();
        createRequest.setName("Duplicate Agent");
        createRequest.setStatus(AgentStatusInternal.DRAFT);
        createRequest.setA2aEnabled(false);

        mockServerClient.when(
                request().withPath("/internal/agents")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CONFLICT.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"errorCode\":\"DUPLICATE_NAME\",\"detail\":\"Agent name already exists\"}"));

        CreateAgentRequestDTO createDTO = new CreateAgentRequestDTO();
        createDTO.setName("Duplicate Agent");
        createDTO.setStatus(AgentStatusDTO.DRAFT);
        createDTO.setA2aEnabled(false);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(createDTO)
                .post()
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode())
                .body("errorCode", equalTo("DUPLICATE_NAME"))
                .body("detail", equalTo("Agent name already exists"));
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

    @Test
    void getAgentMcpToolRules_200Test() {
        AgentMcpToolRuleInternal rule = new AgentMcpToolRuleInternal()
                .id("r1").toolName("getProposal").allowed(ToolPermissionInternal.ALLOW);
        AgentMcpToolRuleListInternal rules = new AgentMcpToolRuleListInternal();
        rules.setRules(List.of(rule));

        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(rules)));

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(AgentMcpToolRuleListDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.getRules().size());
        Assertions.assertEquals("getProposal", response.getRules().get(0).getToolName());
        Assertions.assertEquals(rule.getAllowed().name(), response.getRules().get(0).getAllowed().name());
    }

    @Test
    void createAgentMcpToolRule_201Test() {
        AgentMcpToolRuleInternal created = new AgentMcpToolRuleInternal()
                .id("r1").toolName("deleteProposal").allowed(ToolPermissionInternal.DENY);
        AgentMcpToolRuleListInternal createdList = new AgentMcpToolRuleListInternal().addRulesItem(created);

        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdList)));

        CreateAgentMcpToolRuleRequestDTO request = new CreateAgentMcpToolRuleRequestDTO();
        request.setToolName("deleteProposal");
        request.setAllowed(ToolPermissionDTO.DENY);

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(request))
                .post(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .as(AgentMcpToolRuleListDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("r1", response.getRules().get(0).getId());
        Assertions.assertEquals(created.getAllowed().name(), response.getRules().get(0).getAllowed().name());
    }

    @Test
    void updateAgentMcpToolRule_404Test() {
        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.PUT))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        UpdateAgentMcpToolRuleRequestDTO request = new UpdateAgentMcpToolRuleRequestDTO();
        request.setId("missing");
        request.setModificationCount(0);
        request.setAllowed(ToolPermissionDTO.ALLOW);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(request))
                .put(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void deleteAgentMcpToolRule_204Test() {
        String agentId = "a1";
        String toolId = "t1";
        String ruleId = "r1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules/" + ruleId)
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
                .delete(agentId + "/tools/" + toolId + "/mcp-tool-rules/" + ruleId)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void getAgentMcpToolRules_404Test() {
        String agentId = "a1";
        String toolId = "missing";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void createAgentMcpToolRule_400Test() {
        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        CreateAgentMcpToolRuleRequestDTO request = new CreateAgentMcpToolRuleRequestDTO();
        request.setToolName("deleteProposal");
        request.setAllowed(ToolPermissionDTO.DENY);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(request))
                .post(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void updateAgentMcpToolRule_200Test() {
        AgentMcpToolRuleInternal updated = new AgentMcpToolRuleInternal()
                .id("r1").toolName("deleteProposal").allowed(ToolPermissionInternal.ALLOW);

        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.PUT))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(new AgentMcpToolRuleListInternal().addRulesItem(updated))));

        UpdateAgentMcpToolRuleRequestDTO request = new UpdateAgentMcpToolRuleRequestDTO();
        request.setId("r1");
        request.setModificationCount(0);
        request.setAllowed(ToolPermissionDTO.ALLOW);

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(request))
                .put(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(AgentMcpToolRuleListDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("r1", response.getRules().get(0).getId());
        Assertions.assertEquals(updated.getAllowed().name(), response.getRules().get(0).getAllowed().name());
    }

    @Test
    void deleteAgentMcpToolRule_400Test() {
        String agentId = "a1";
        String toolId = "t1";
        String ruleId = "r1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules/" + ruleId)
                        .withMethod(HttpMethod.DELETE))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete(agentId + "/tools/" + toolId + "/mcp-tool-rules/" + ruleId)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void getAgentMcpToolRules_204Test() {
        // 204 is a 2xx code — REST client does NOT throw, so the if-branch is reached
        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void createAgentMcpToolRule_204Test() {
        // 204 is a 2xx code — REST client does NOT throw, so the if-branch is reached
        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        CreateAgentMcpToolRuleRequestDTO request = new CreateAgentMcpToolRuleRequestDTO();
        request.setToolName("deleteProposal");
        request.setAllowed(ToolPermissionDTO.DENY);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(request))
                .post(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void updateAgentMcpToolRule_204Test() {
        // 204 is a 2xx code — REST client does NOT throw, so the if-branch is reached
        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.PUT))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        UpdateAgentMcpToolRuleRequestDTO request = new UpdateAgentMcpToolRuleRequestDTO();
        request.setId("r1");
        request.setModificationCount(0);
        request.setAllowed(ToolPermissionDTO.ALLOW);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(request))
                .put(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void getAgentMcpToolRules_200_invalidBody_throwsInTryWithResources() {
        // 200 with text/plain body → readEntity fails inside try-with-resources
        // → close() called with exception pending → covers exception branch of try-with-resources
        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.GET))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.TEXT_PLAIN)
                        .withBody("not-json"));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void createAgentMcpToolRule_201_invalidBody_throwsInTryWithResources() {
        // 201 with text/plain body → readEntity fails inside try-with-resources
        // → close() called with exception pending → covers exception branch of try-with-resources
        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.TEXT_PLAIN)
                        .withBody("not-json"));

        CreateAgentMcpToolRuleRequestDTO request = new CreateAgentMcpToolRuleRequestDTO();
        request.setToolName("getProposal");
        request.setAllowed(ToolPermissionDTO.ALLOW);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(request))
                .post(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void updateAgentMcpToolRule_200_invalidBody_throwsInTryWithResources() {
        // 200 with text/plain body → readEntity fails inside try-with-resources
        // → close() called with exception pending → covers exception branch of try-with-resources
        String agentId = "a1";
        String toolId = "t1";
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/tools/" + toolId + "/mcp-tool-rules")
                        .withMethod(HttpMethod.PUT))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.TEXT_PLAIN)
                        .withBody("not-json"));

        UpdateAgentMcpToolRuleRequestDTO request = new UpdateAgentMcpToolRuleRequestDTO();
        request.setId("r1");
        request.setModificationCount(0);
        request.setAllowed(ToolPermissionDTO.ALLOW);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(request))
                .put(agentId + "/tools/" + toolId + "/mcp-tool-rules")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void createAgentConfigurationVersionTest() {
        String agentId = "a1";
        CreateAgentConfigurationVersionInternal request = new CreateAgentConfigurationVersionInternal()
                .name("baseline");
        AgentConfigurationVersionInternal createdVersion = new AgentConfigurationVersionInternal()
                .id("version-1")
                .name("baseline")
                .active(true);

        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/configuration/versions")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(request)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdVersion)));

        var response = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(new CreateAgentConfigurationVersionDTO().name("baseline"))
                .post(agentId + "/configuration/versions")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .as(AgentConfigurationVersionDTO.class);

        Assertions.assertEquals("version-1", response.getId());
        Assertions.assertEquals("baseline", response.getName());
        Assertions.assertTrue(response.getActive());
    }

    @Test
    void getAgentConfigurationVersionTest() {
        String agentId = "a1";
        String versionId = "version-1";
        AgentConfigurationInternal configuration = new AgentConfigurationInternal().schemaVersion("1.0");

        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/configuration/versions/" + versionId)
                        .withMethod(HttpMethod.GET))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(configuration)));

        var result = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get(agentId + "/configuration/versions/" + versionId)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(AgentConfigurationDTO.class);

        Assertions.assertEquals("1.0", result.getSchemaVersion());
    }

    @Test
    void activateAgentConfigurationVersionTest() {
        String agentId = "a1";
        String versionId = "version-1";
        AgentConfigurationVersionInternal activatedVersion = new AgentConfigurationVersionInternal()
                .id(versionId)
                .name("baseline")
                .active(true);

        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/configuration/versions/" + versionId + "/activate")
                        .withMethod(HttpMethod.PUT))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(activatedVersion)));

        var result = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .put(agentId + "/configuration/versions/" + versionId + "/activate")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(AgentConfigurationVersionDTO.class);

        Assertions.assertEquals(versionId, result.getId());
        Assertions.assertEquals("baseline", result.getName());
        Assertions.assertTrue(result.getActive());
    }

    @Test
    void createPropagatesUnexpectedStatus() {
        var api = mock(AgentInternalApi.class);
        var createResponse = new StubResponse(Response.Status.OK.getStatusCode());
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(configurationVersionRequest())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void createReportsCloseFailureAfterUnexpectedStatus() {
        var api = mock(AgentInternalApi.class);
        var createResponse = new StubResponse(Response.Status.OK.getStatusCode())
                .closeFails(new IllegalStateException("could not close create response"));
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        createAndExpectInternalServerError();
    }

    @Test
    void createHandlesNullResponseFromClient() {
        var api = mock(AgentInternalApi.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(null);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        createAndExpectInternalServerError();
    }

    @Test
    void createClosesResponseWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var createResponse = new StubResponse(Response.Status.CREATED.getStatusCode())
                .readFails(new IllegalStateException("could not read create response"))
                .closeFails(new IllegalStateException("could not close create response"));
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        createAndExpectInternalServerError();
    }

    @Test
    void createClosesResponseCleanlyWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var createResponse = new StubResponse(Response.Status.CREATED.getStatusCode())
                .readFails(new IllegalStateException("could not read create response"));
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        createAndExpectInternalServerError();
        Assertions.assertEquals(1, createResponse.closeCount());
    }

    @Test
    void activatePropagatesUnexpectedStatus() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = new StubResponse(Response.Status.NO_CONTENT.getStatusCode());
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .put("1/configuration/versions/version-2/activate")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void activateReportsCloseFailureAfterUnexpectedStatus() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = new StubResponse(Response.Status.NO_CONTENT.getStatusCode())
                .closeFails(new IllegalStateException("could not close activation response"));
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        activateAndExpectInternalServerError();
    }

    @Test
    void activateHandlesNullResponseFromClient() {
        var api = mock(AgentInternalApi.class);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(null);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        activateAndExpectInternalServerError();
    }

    @Test
    void activateClosesResponseWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = new StubResponse(Response.Status.OK.getStatusCode())
                .readFails(new IllegalStateException("could not read activation response"))
                .closeFails(new IllegalStateException("could not close activation response"));
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        activateAndExpectInternalServerError();
    }

    @Test
    void activateClosesResponseCleanlyWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = new StubResponse(Response.Status.OK.getStatusCode())
                .readFails(new IllegalStateException("could not read activation response"));
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        activateAndExpectInternalServerError();
        Assertions.assertEquals(1, activationResponse.closeCount());
    }

    @Test
    void activateReportsCloseFailureFromResponse() {
        var api = mock(AgentInternalApi.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(true);
        var activationResponse = new StubResponse(Response.Status.OK.getStatusCode())
                .entity(version)
                .closeFails(new IllegalStateException("could not close activation response"));
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        activateAndExpectInternalServerError();
    }

    @Test
    void listReturnsVersions() {
        var api = mock(AgentInternalApi.class);
        var version1 = new AgentConfigurationVersionInternal().id("v1").name("release-1").active(false);
        var version2 = new AgentConfigurationVersionInternal().id("v2").name("release-2").active(true);
        var internalList = new AgentConfigurationVersionListInternal()
                .addVersionsItem(version1)
                .addVersionsItem(version2);
        var listResponse = new StubResponse(Response.Status.OK.getStatusCode()).entity(internalList);
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        var result = given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .jsonPath();

        Assertions.assertEquals(2, result.getList("versions").size());
    }

    @Test
    void listHandlesNullVersionsList() {
        var api = mock(AgentInternalApi.class);
        var internalList = new AgentConfigurationVersionListInternal();
        internalList.setVersions(null);
        var listResponse = new StubResponse(Response.Status.OK.getStatusCode()).entity(internalList);
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        var result = given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .jsonPath();

        Assertions.assertEquals(0, result.getList("versions").size());
    }

    @Test
    void listPropagatesNonOkStatus() {
        var api = mock(AgentInternalApi.class);
        var listResponse = new StubResponse(Response.Status.NOT_FOUND.getStatusCode());
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void listHandlesNullResponseFromClient() {
        var api = mock(AgentInternalApi.class);
        when(api.getAgentConfigurationVersions("1")).thenReturn(null);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        listAndExpectInternalServerError();
    }

    @Test
    void listClosesResponseWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var listResponse = new StubResponse(Response.Status.OK.getStatusCode())
                .readFails(new IllegalStateException("could not read list response"))
                .closeFails(new IllegalStateException("could not close list response"));
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        listAndExpectInternalServerError();
    }

    @Test
    void listClosesResponseCleanlyWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var listResponse = new StubResponse(Response.Status.OK.getStatusCode())
                .readFails(new IllegalStateException("could not read list response"));
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        listAndExpectInternalServerError();
        Assertions.assertEquals(1, listResponse.closeCount());
    }

    @Test
    void listPassesThroughNonJsonErrorBodyWithUpstreamMediaType() {
        var api = mock(AgentInternalApi.class);
        var listResponse = new StubResponse(Response.Status.BAD_GATEWAY.getStatusCode())
                .mediaType(TEXT_PLAIN_TYPE)
                .entity("upstream unavailable");
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions")
                .then()
                .statusCode(Response.Status.BAD_GATEWAY.getStatusCode())
                .contentType(TEXT_PLAIN)
                .body(equalTo("upstream unavailable"));
    }

    @Test
    void listReportsCloseFailureFromResponse() {
        var api = mock(AgentInternalApi.class);
        var internalList = new AgentConfigurationVersionListInternal()
                .addVersionsItem(new AgentConfigurationVersionInternal().id("v1").name("r1").active(true));
        var listResponse = new StubResponse(Response.Status.OK.getStatusCode())
                .entity(internalList)
                .closeFails(new IllegalStateException("could not close list response"));
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        listAndExpectInternalServerError();
    }

    @Test
    void getVersionPassesThroughErrorBody() {
        var api = mock(AgentInternalApi.class);
        var getResponse = new StubResponse(Response.Status.NOT_FOUND.getStatusCode())
                .entity("{\"errorCode\":\"NOT_FOUND\"}");
        when(api.getAgentConfigurationVersion("1", "version-2")).thenReturn(getResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions/version-2")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body("errorCode", equalTo("NOT_FOUND"));
    }

    @Test
    void listPassesThroughErrorBody() {
        var api = mock(AgentInternalApi.class);
        var listResponse = new StubResponse(Response.Status.CONFLICT.getStatusCode())
                .entity("{\"errorCode\":\"CONFLICT\"}");
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode())
                .body("errorCode", equalTo("CONFLICT"));
    }

    @Test
    void createPassesThroughDuplicateNameProblemDetail() {
        var api = mock(AgentInternalApi.class);
        var createResponse = new StubResponse(Response.Status.CONFLICT.getStatusCode())
                .entity("{\"errorCode\":\"DUPLICATE_NAME\",\"detail\":\"Version name already exists\"}");
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(configurationVersionRequest())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode())
                .body("errorCode", equalTo("DUPLICATE_NAME"))
                .body("detail", equalTo("Version name already exists"));
    }

    @Test
    void activatePassesThroughErrorProblemDetail() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = new StubResponse(Response.Status.NOT_FOUND.getStatusCode())
                .entity("{\"errorCode\":\"NOT_FOUND\",\"detail\":\"Version not found\"}");
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .put("1/configuration/versions/version-2/activate")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body("errorCode", equalTo("NOT_FOUND"))
                .body("detail", equalTo("Version not found"));
    }

    @Test
    void createAgentPassesThroughDuplicateNameProblemDetail() {
        var api = mock(AgentInternalApi.class);
        var createResponse = new StubResponse(Response.Status.CONFLICT.getStatusCode())
                .mediaType(APPLICATION_JSON_TYPE)
                .entity("{\"errorCode\":\"DUPLICATE_NAME\",\"detail\":\"Agent name already exists\"}");
        when(api.createAgent(any(CreateAgentRequestInternal.class))).thenReturn(createResponse);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        var createDTO = new CreateAgentRequestDTO();
        createDTO.setName("Duplicate Agent");
        createDTO.setStatus(AgentStatusDTO.DRAFT);
        createDTO.setA2aEnabled(false);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(createDTO)
                .post()
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode())
                .body("errorCode", equalTo("DUPLICATE_NAME"))
                .body("detail", equalTo("Agent name already exists"));
    }

    @Test
    void createRequiresWritePermission() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(configurationVersionRequest())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    void activateRequiresWritePermission() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .put("1/configuration/versions/version-2/activate")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
    }

    private void createAndExpectInternalServerError() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(configurationVersionRequest())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private void activateAndExpectInternalServerError() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .put("1/configuration/versions/version-2/activate")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private void listAndExpectInternalServerError() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private CreateAgentConfigurationVersionDTO configurationVersionRequest() {
        var request = new CreateAgentConfigurationVersionDTO();
        request.setName("release-2");
        return request;
    }

    /**
     * Plain JAX-RS {@link Response} stub. Mockito cannot mock {@link Response} in the packaged IT run,
     * because the static factories and mock creation need the JAX-RS RuntimeDelegate lookup.
     */
    private static final class StubResponse extends Response {
        private final int status;
        private jakarta.ws.rs.core.MediaType mediaType;
        private Object entity;
        private RuntimeException readFailure;
        private RuntimeException closeFailure;
        private int closeCount;

        StubResponse(int status) {
            this.status = status;
        }

        StubResponse mediaType(jakarta.ws.rs.core.MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        StubResponse entity(Object entity) {
            this.entity = entity;
            return this;
        }

        StubResponse readFails(RuntimeException failure) {
            this.readFailure = failure;
            return this;
        }

        StubResponse closeFails(RuntimeException failure) {
            this.closeFailure = failure;
            return this;
        }

        int closeCount() {
            return closeCount;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public StatusType getStatusInfo() {
            return Status.fromStatusCode(status);
        }

        @Override
        public Object getEntity() {
            return entity;
        }

        @Override
        public <T> T readEntity(Class<T> type) {
            if (readFailure != null) {
                throw readFailure;
            }
            return type.cast(entity);
        }

        @Override
        public <T> T readEntity(GenericType<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T readEntity(Class<T> type, Annotation[] annotations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T readEntity(GenericType<T> type, Annotation[] annotations) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasEntity() {
            return entity != null;
        }

        @Override
        public boolean bufferEntity() {
            return false;
        }

        @Override
        public void close() {
            closeCount++;
            if (closeFailure != null) {
                throw closeFailure;
            }
        }

        @Override
        public jakarta.ws.rs.core.MediaType getMediaType() {
            return mediaType;
        }

        @Override
        public Locale getLanguage() {
            return null;
        }

        @Override
        public int getLength() {
            return -1;
        }

        @Override
        public Set<String> getAllowedMethods() {
            return Set.of();
        }

        @Override
        public Map<String, NewCookie> getCookies() {
            return Map.of();
        }

        @Override
        public EntityTag getEntityTag() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }

        @Override
        public Date getLastModified() {
            return null;
        }

        @Override
        public URI getLocation() {
            return null;
        }

        @Override
        public Set<Link> getLinks() {
            return Set.of();
        }

        @Override
        public boolean hasLink(String relation) {
            return false;
        }

        @Override
        public Link getLink(String relation) {
            return null;
        }

        @Override
        public Link.Builder getLinkBuilder(String relation) {
            return null;
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
            return new MultivaluedHashMap<>();
        }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() {
            return new MultivaluedHashMap<>();
        }

        @Override
        public String getHeaderString(String name) {
            return null;
        }
    }
}
