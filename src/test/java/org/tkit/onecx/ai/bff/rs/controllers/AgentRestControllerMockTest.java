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

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.bff.rs.AbstractTest;
import org.tkit.onecx.ai.bff.rs.controllers.AgentRestControllerTest.StubResponse;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.AgentInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

/**
 * Tests that replace the JAX-RS client with a CDI mock through {@link QuarkusMock}.
 * <p>
 * {@link QuarkusMock} needs CDI, which is not available in the packaged {@code *IT} run. This class is therefore
 * not extended by {@code AgentRestControllerTestIT}, and Failsafe does not pick it up.
 */
@QuarkusTest
@LogService
@TestHTTPEndpoint(AgentRestController.class)
class AgentRestControllerMockTest extends AbstractTest {

    KeycloakTestClient keycloakTestClient = new KeycloakTestClient();

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
                .body(AgentRestControllerTest.configurationVersionRequest())
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
                .body(AgentRestControllerTest.configurationVersionRequest())
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

    private void createAndExpectInternalServerError() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(AgentRestControllerTest.configurationVersionRequest())
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
}
