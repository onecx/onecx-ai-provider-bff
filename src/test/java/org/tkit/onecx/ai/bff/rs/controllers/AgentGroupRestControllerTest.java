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
@TestHTTPEndpoint(AgentGroupRestController.class)
class AgentGroupRestControllerTest extends AbstractTest {

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
    void getAgentGroupById_200Test() {
        AgentGroupInternal fakeData = new AgentGroupInternal()
                .id("1").name("group1").description("desc").routingInstructions("route")
                .orchestrationMode(AgentGroupOrchestrationModeInternal.SEQUENTIAL)
                .responseStrategy(AgentGroupResponseStrategyInternal.LAST);

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/agentGroups/" + testId)
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
                .as(AgentGroupDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
        Assertions.assertEquals(fakeData.getOrchestrationMode().name(), response.getOrchestrationMode().name());
        Assertions.assertEquals(fakeData.getResponseStrategy().name(), response.getResponseStrategy().name());
    }

    @Test
    void searchAgentGroupTest() {
        AgentGroupSearchCriteriaInternal internalCriteria = new AgentGroupSearchCriteriaInternal();
        internalCriteria.setName("group1");

        AgentGroupSearchCriteriaDTO criteria = new AgentGroupSearchCriteriaDTO();
        criteria.setName("group1");

        AgentGroupInternal group1 = new AgentGroupInternal().id("1").name("group1")
                .orchestrationMode(AgentGroupOrchestrationModeInternal.PARALLEL);

        AgentGroupPageResultInternal pageResult = new AgentGroupPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(List.of(group1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/agentGroups/search")
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
                .as(AgentGroupPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(pageResult.getSize(), results.getSize());
        Assertions.assertEquals(group1.getName(), results.getStream().get(0).getName());
    }

    @Test
    void createAgentGroupTest() {
        CreateAgentGroupRequestInternal createRequest = new CreateAgentGroupRequestInternal();
        createRequest.setName("New Group");
        createRequest.setDescription("desc");
        createRequest.setRoutingInstructions("route");
        createRequest.setOrchestrationMode(AgentGroupOrchestrationModeInternal.LEAD_DELEGATES);
        createRequest.setResponseStrategy(AgentGroupResponseStrategyInternal.SUMMARY);

        AgentGroupInternal responseGroup = new AgentGroupInternal();
        responseGroup.setId("new-id");
        responseGroup.setName("New Group");

        mockServerClient.when(
                request().withPath("/internal/agentGroups")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseGroup)));

        CreateAgentGroupRequestDTO createDTO = new CreateAgentGroupRequestDTO();
        createDTO.setName("New Group");
        createDTO.setDescription("desc");
        createDTO.setRoutingInstructions("route");
        createDTO.setOrchestrationMode(AgentGroupOrchestrationModeDTO.LEAD_DELEGATES);
        createDTO.setResponseStrategy(AgentGroupResponseStrategyDTO.SUMMARY);

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
                .as(AgentGroupDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseGroup.getName(), response.getName());
    }

    @Test
    void updateAgentGroupTest() {
        UpdateAgentGroupRequestInternal updateRequest = new UpdateAgentGroupRequestInternal();
        updateRequest.setName("Updated Group");
        updateRequest.setOrchestrationMode(AgentGroupOrchestrationModeInternal.SUPERVISOR_ROUTED);
        updateRequest.setModificationCount(0);

        AgentGroupInternal responseGroup = new AgentGroupInternal();
        responseGroup.setId("1");
        responseGroup.setName("Updated Group");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/agentGroups/" + testId)
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseGroup)));

        UpdateAgentGroupRequestDTO updateDTO = new UpdateAgentGroupRequestDTO();
        updateDTO.setName("Updated Group");
        updateDTO.setOrchestrationMode(AgentGroupOrchestrationModeDTO.SUPERVISOR_ROUTED);
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
                .as(AgentGroupDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseGroup.getName(), response.getName());
    }

    @Test
    void updateAgentGroupTest_400_ConstraintException() {
        UpdateAgentGroupRequestDTO updateDTO = new UpdateAgentGroupRequestDTO();
        updateDTO.setName("Updated Group");

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
    void deleteAgentGroupTest() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/agentGroups/" + testId)
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
    void deleteAgentGroup_ClientException_Test() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/agentGroups/" + testId)
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
