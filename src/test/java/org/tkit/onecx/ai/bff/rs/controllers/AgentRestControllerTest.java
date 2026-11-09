package org.tkit.onecx.ai.bff.rs.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import gen.org.tkit.onecx.ai.management.bff.client.api.AgentInternalApi;
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
            mockServerClient.clear(MOCK_ID + "-create");
            mockServerClient.clear(MOCK_ID + "-activate");
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
    void publishAgentConfigurationVersion_createsAndActivatesVersion() {
        String agentId = "1";
        String versionId = "version-2";
        var createRequest = new CreateAgentConfigurationVersionInternal().name("release-2");
        var createdVersion = new AgentConfigurationVersionInternal()
                .id(versionId)
                .name("release-2")
                .active(false);
        var activatedVersion = new AgentConfigurationVersionInternal()
                .id(versionId)
                .name("release-2")
                .active(true);

        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/configuration/versions")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(java.util.Map.of("name", createRequest.getName()))))
                .withId(MOCK_ID + "-create")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdVersion)));
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/configuration/versions/" + versionId + "/activate")
                        .withMethod(HttpMethod.PUT))
                .withId(MOCK_ID + "-activate")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(activatedVersion)));

        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        var publishedVersion = given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post(agentId + "/configuration/versions")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .as(AgentConfigurationVersionDTO.class);

        Assertions.assertEquals(versionId, publishedVersion.getId());
        Assertions.assertEquals("release-2", publishedVersion.getName());
        Assertions.assertTrue(publishedVersion.getActive());
    }

    @Test
    void publishAgentConfigurationVersion_preservesDuplicateNameProblem() {
        String agentId = "1";
        var duplicateProblem = java.util.Map.of(
                "errorCode", "DUPLICATE_AGENT_CONFIGURATION_VERSION_NAME",
                "detail", "Agent already has a configuration version named 'release-2'");

        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/configuration/versions")
                        .withMethod(HttpMethod.POST))
                .withId(MOCK_ID + "-create")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(duplicateProblem)));

        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        var problem = given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post(agentId + "/configuration/versions")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .as(ProblemDetailResponseDTO.class);

        Assertions.assertEquals("DUPLICATE_AGENT_CONFIGURATION_VERSION_NAME", problem.getErrorCode());
        Assertions.assertTrue(problem.getDetail().contains("release-2"));
    }

    @Test
    void publishAgentConfigurationVersion_propagatesActivationFailure() {
        String agentId = "1";
        String versionId = "version-2";
        var createdVersion = new AgentConfigurationVersionInternal()
                .id(versionId)
                .name("release-2")
                .active(false);
        var activationProblem = java.util.Map.of(
                "errorCode", "INVALID_AGENT_CONFIGURATION_DEPENDENCIES",
                "detail", "The configuration references an unavailable provider");

        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/configuration/versions")
                        .withMethod(HttpMethod.POST))
                .withId(MOCK_ID + "-create")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdVersion)));
        mockServerClient.when(
                request().withPath("/internal/agents/" + agentId + "/configuration/versions/" + versionId + "/activate")
                        .withMethod(HttpMethod.PUT))
                .withId(MOCK_ID + "-activate")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(activationProblem)));

        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        var problem = given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post(agentId + "/configuration/versions")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .extract()
                .as(ProblemDetailResponseDTO.class);

        Assertions.assertEquals("INVALID_AGENT_CONFIGURATION_DEPENDENCIES", problem.getErrorCode());
    }

    @Test
    void publishAgentConfigurationVersion_doesNotActivateWhenCreateStatusIsUnexpected() {
        mockServerClient.when(
                request().withPath("/internal/agents/1/configuration/versions")
                        .withMethod(HttpMethod.POST))
                .withId(MOCK_ID + "-create")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.TEXT_PLAIN)
                        .withBody("unexpected create status"));

        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void publishAgentConfigurationVersion_propagatesUnexpectedActivationStatus() {
        String versionId = "version-2";
        var createdVersion = new AgentConfigurationVersionInternal()
                .id(versionId)
                .name("release-2")
                .active(false);
        mockServerClient.when(
                request().withPath("/internal/agents/1/configuration/versions")
                        .withMethod(HttpMethod.POST))
                .withId(MOCK_ID + "-create")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdVersion)));
        mockServerClient.when(
                request().withPath("/internal/agents/1/configuration/versions/" + versionId + "/activate")
                        .withMethod(HttpMethod.PUT))
                .withId(MOCK_ID + "-activate")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void publishAgentConfigurationVersion_doesNotReportSuccessWhenActivationIsInactive() {
        String versionId = "version-2";
        var createdVersion = new AgentConfigurationVersionInternal()
                .id(versionId)
                .name("release-2")
                .active(false);
        mockServerClient.when(
                request().withPath("/internal/agents/1/configuration/versions")
                        .withMethod(HttpMethod.POST))
                .withId(MOCK_ID + "-create")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdVersion)));
        mockServerClient.when(
                request().withPath("/internal/agents/1/configuration/versions/" + versionId + "/activate")
                        .withMethod(HttpMethod.PUT))
                .withId(MOCK_ID + "-activate")
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdVersion)));

        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void publishAgentConfigurationVersion_closesBothResponses() {
        var agentInternalApi = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal()
                .id("version-2")
                .name("release-2")
                .active(true);
        when(agentInternalApi.createAgentConfigurationVersion(eq("1"), any()))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(agentInternalApi.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);

        var controller = new AgentRestController();
        controller.agentInternalApi = agentInternalApi;
        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        var response = controller.publishAgentConfigurationVersion("1", requestDTO);

        Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(createResponse).close();
        verify(activationResponse).close();
    }

    @Test
    void publishAgentConfigurationVersion_handlesNullCreateResponse() {
        var agentInternalApi = mock(AgentInternalApi.class);
        when(agentInternalApi.createAgentConfigurationVersion(eq("1"), any())).thenReturn(null);

        var controller = new AgentRestController();
        controller.agentInternalApi = agentInternalApi;
        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        Assertions.assertThrows(
                NullPointerException.class, () -> controller.publishAgentConfigurationVersion("1", requestDTO));
    }

    @Test
    void publishAgentConfigurationVersion_handlesNullActivationResponse() {
        var agentInternalApi = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var createdVersion = new AgentConfigurationVersionInternal()
                .id("version-2")
                .name("release-2")
                .active(false);
        when(agentInternalApi.createAgentConfigurationVersion(eq("1"), any())).thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(createdVersion);
        when(agentInternalApi.activateAgentConfigurationVersion("1", "version-2")).thenReturn(null);

        var controller = new AgentRestController();
        controller.agentInternalApi = agentInternalApi;
        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        Assertions.assertThrows(
                NullPointerException.class, () -> controller.publishAgentConfigurationVersion("1", requestDTO));
        verify(createResponse).close();
    }

    @Test
    void publishAgentConfigurationVersion_propagatesCloseFailureWhenReadAlsoFails() {
        var agentInternalApi = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var createdVersion = new AgentConfigurationVersionInternal()
                .id("version-2")
                .name("release-2")
                .active(false);
        var readFailure = new IllegalStateException("activation response could not be read");
        var closeFailure = new IllegalStateException("activation response could not be closed");
        when(agentInternalApi.createAgentConfigurationVersion(eq("1"), any()))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(createdVersion);
        when(agentInternalApi.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenThrow(readFailure);
        doThrow(closeFailure).when(activationResponse).close();

        var controller = new AgentRestController();
        controller.agentInternalApi = agentInternalApi;
        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        var thrown = Assertions.assertThrows(
                IllegalStateException.class, () -> controller.publishAgentConfigurationVersion("1", requestDTO));

        Assertions.assertSame(closeFailure, thrown);
        Assertions.assertEquals(0, thrown.getSuppressed().length);
        verify(createResponse).close();
        verify(activationResponse).close();
    }

    @Test
    void publishAgentConfigurationVersion_propagatesCreateCloseFailureWhenCreateReadAlsoFails() {
        var agentInternalApi = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var readFailure = new IllegalStateException("create response could not be read");
        var closeFailure = new IllegalStateException("create response could not be closed");
        when(agentInternalApi.createAgentConfigurationVersion(eq("1"), any())).thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenThrow(readFailure);
        doThrow(closeFailure).when(createResponse).close();

        var controller = new AgentRestController();
        controller.agentInternalApi = agentInternalApi;
        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        var thrown = Assertions.assertThrows(
                IllegalStateException.class, () -> controller.publishAgentConfigurationVersion("1", requestDTO));

        Assertions.assertSame(closeFailure, thrown);
        Assertions.assertEquals(0, thrown.getSuppressed().length);
        verify(createResponse).close();
    }

    @Test
    void publishAgentConfigurationVersion_propagatesActivationResponseCloseFailure() {
        var agentInternalApi = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal()
                .id("version-2")
                .name("release-2")
                .active(true);
        var closeFailure = new IllegalStateException("activation response could not be closed");
        when(agentInternalApi.createAgentConfigurationVersion(eq("1"), any())).thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(agentInternalApi.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        doThrow(closeFailure).when(activationResponse).close();

        var controller = new AgentRestController();
        controller.agentInternalApi = agentInternalApi;
        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        Assertions.assertSame(
                closeFailure,
                Assertions.assertThrows(
                        IllegalStateException.class,
                        () -> controller.publishAgentConfigurationVersion("1", requestDTO)));
        verify(createResponse).close();
        verify(activationResponse).close();
    }

    @Test
    void publishAgentConfigurationVersion_propagatesCreateResponseCloseFailure() {
        var agentInternalApi = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal()
                .id("version-2")
                .name("release-2")
                .active(true);
        var closeFailure = new IllegalStateException("create response could not be closed");
        when(agentInternalApi.createAgentConfigurationVersion(eq("1"), any())).thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(agentInternalApi.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        doThrow(closeFailure).when(createResponse).close();

        var controller = new AgentRestController();
        controller.agentInternalApi = agentInternalApi;
        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        Assertions.assertSame(
                closeFailure,
                Assertions.assertThrows(
                        IllegalStateException.class,
                        () -> controller.publishAgentConfigurationVersion("1", requestDTO)));
        verify(createResponse).close();
        verify(activationResponse).close();
    }

    @Test
    void publishAgentConfigurationVersion_requiresDedicatedPermission() {
        var requestDTO = new PublishAgentConfigurationVersionRequestDTO();
        requestDTO.setName("release-2");

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());
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
}
