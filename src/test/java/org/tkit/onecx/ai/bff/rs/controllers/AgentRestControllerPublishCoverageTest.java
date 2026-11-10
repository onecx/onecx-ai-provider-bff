package org.tkit.onecx.ai.bff.rs.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.bff.rs.AbstractTest;

import gen.org.tkit.onecx.ai.management.bff.client.api.AgentInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.AgentConfigurationVersionInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.AgentConfigurationVersionListInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.CreateAgentConfigurationVersionInternal;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.CreateAgentConfigurationVersionDTO;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@TestHTTPEndpoint(AgentRestController.class)
class AgentRestControllerPublishCoverageTest extends AbstractTest {

    private final KeycloakTestClient keycloakTestClient = new KeycloakTestClient();

    // --- createAgentConfigurationVersion coverage ---

    @Test
    void createPropagatesUnexpectedStatus() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(request())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void createReportsCloseFailureAfterUnexpectedStatus() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        doThrow(new IllegalStateException("could not close create response")).when(createResponse).close();
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
        var createResponse = mock(Response.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class))
                .thenThrow(new IllegalStateException("could not read create response"));
        doThrow(new IllegalStateException("could not close create response")).when(createResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        createAndExpectInternalServerError();
    }

    @Test
    void createClosesResponseCleanlyWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class))
                .thenThrow(new IllegalStateException("could not read create response"));
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        createAndExpectInternalServerError();
    }

    // --- activateAgentConfigurationVersion coverage ---

    @Test
    void activatePropagatesUnexpectedStatus() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = mock(Response.class);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.NO_CONTENT.getStatusCode());
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
        var activationResponse = mock(Response.class);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.NO_CONTENT.getStatusCode());
        doThrow(new IllegalStateException("could not close activation response")).when(activationResponse).close();
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
        var activationResponse = mock(Response.class);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class))
                .thenThrow(new IllegalStateException("could not read activation response"));
        doThrow(new IllegalStateException("could not close activation response")).when(activationResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        activateAndExpectInternalServerError();
    }

    @Test
    void activateClosesResponseCleanlyWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = mock(Response.class);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class))
                .thenThrow(new IllegalStateException("could not read activation response"));
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        activateAndExpectInternalServerError();
    }

    @Test
    void activateReportsCloseFailureFromResponse() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(true);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        doThrow(new IllegalStateException("could not close activation response")).when(activationResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        activateAndExpectInternalServerError();
    }

    // --- getAgentConfigurationVersions (list) coverage ---

    @Test
    void listReturnsVersions() {
        var api = mock(AgentInternalApi.class);
        var listResponse = mock(Response.class);
        var version1 = new AgentConfigurationVersionInternal().id("v1").name("release-1").active(false);
        var version2 = new AgentConfigurationVersionInternal().id("v2").name("release-2").active(true);
        var internalList = new AgentConfigurationVersionListInternal()
                .addVersionsItem(version1)
                .addVersionsItem(version2);
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        when(listResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(listResponse.readEntity(AgentConfigurationVersionListInternal.class)).thenReturn(internalList);
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

        org.junit.jupiter.api.Assertions.assertEquals(2, result.getList("versions").size());
    }

    @Test
    void listHandlesNullVersionsList() {
        var api = mock(AgentInternalApi.class);
        var listResponse = mock(Response.class);
        var internalList = new AgentConfigurationVersionListInternal();
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        when(listResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(listResponse.readEntity(AgentConfigurationVersionListInternal.class)).thenReturn(internalList);
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

        org.junit.jupiter.api.Assertions.assertEquals(0, result.getList("versions").size());
    }

    @Test
    void listPropagatesNonOkStatus() {
        var api = mock(AgentInternalApi.class);
        var listResponse = mock(Response.class);
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        when(listResponse.getStatus()).thenReturn(Response.Status.NOT_FOUND.getStatusCode());
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
        var listResponse = mock(Response.class);
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        when(listResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(listResponse.readEntity(AgentConfigurationVersionListInternal.class))
                .thenThrow(new IllegalStateException("could not read list response"));
        doThrow(new IllegalStateException("could not close list response")).when(listResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        listAndExpectInternalServerError();
    }

    @Test
    void listClosesResponseCleanlyWhenReadFails() {
        var api = mock(AgentInternalApi.class);
        var listResponse = mock(Response.class);
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        when(listResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(listResponse.readEntity(AgentConfigurationVersionListInternal.class))
                .thenThrow(new IllegalStateException("could not read list response"));
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        listAndExpectInternalServerError();
    }

    @Test
    void listReportsCloseFailureFromResponse() {
        var api = mock(AgentInternalApi.class);
        var listResponse = mock(Response.class);
        var internalList = new AgentConfigurationVersionListInternal()
                .addVersionsItem(new AgentConfigurationVersionInternal().id("v1").name("r1").active(true));
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        when(listResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(listResponse.readEntity(AgentConfigurationVersionListInternal.class)).thenReturn(internalList);
        doThrow(new IllegalStateException("could not close list response")).when(listResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        listAndExpectInternalServerError();
    }

    // --- error pass-through for get version and list versions ---

    @Test
    void getVersionPassesThroughErrorBody() {
        var api = mock(AgentInternalApi.class);
        var getResponse = mock(Response.class);
        when(api.getAgentConfigurationVersion("1", "version-2")).thenReturn(getResponse);
        when(getResponse.getStatus()).thenReturn(Response.Status.NOT_FOUND.getStatusCode());
        when(getResponse.readEntity(String.class)).thenReturn("{\"errorCode\":\"NOT_FOUND\"}");
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions/version-2")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body("errorCode", org.hamcrest.Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void listPassesThroughErrorBody() {
        var api = mock(AgentInternalApi.class);
        var listResponse = mock(Response.class);
        when(api.getAgentConfigurationVersions("1")).thenReturn(listResponse);
        when(listResponse.getStatus()).thenReturn(Response.Status.CONFLICT.getStatusCode());
        when(listResponse.readEntity(String.class)).thenReturn("{\"errorCode\":\"CONFLICT\"}");
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("1/configuration/versions")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode())
                .body("errorCode", org.hamcrest.Matchers.equalTo("CONFLICT"));
    }

    @Test
    void createPassesThroughDuplicateNameProblemDetail() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CONFLICT.getStatusCode());
        when(createResponse.readEntity(String.class))
                .thenReturn("{\"errorCode\":\"DUPLICATE_NAME\",\"detail\":\"Version name already exists\"}");
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(request())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.CONFLICT.getStatusCode())
                .body("errorCode", org.hamcrest.Matchers.equalTo("DUPLICATE_NAME"))
                .body("detail", org.hamcrest.Matchers.equalTo("Version name already exists"));
    }

    @Test
    void activatePassesThroughErrorProblemDetail() {
        var api = mock(AgentInternalApi.class);
        var activationResponse = mock(Response.class);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.NOT_FOUND.getStatusCode());
        when(activationResponse.readEntity(String.class))
                .thenReturn("{\"errorCode\":\"NOT_FOUND\",\"detail\":\"Version not found\"}");
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .put("1/configuration/versions/version-2/activate")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .body("errorCode", org.hamcrest.Matchers.equalTo("NOT_FOUND"))
                .body("detail", org.hamcrest.Matchers.equalTo("Version not found"));
    }

    // --- helpers ---

    private void createAndExpectInternalServerError() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(request())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void createRequiresWritePermission() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(USER))
                .header(APM_HEADER_PARAM, USER)
                .contentType(APPLICATION_JSON)
                .body(request())
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

    private CreateAgentConfigurationVersionDTO request() {
        var request = new CreateAgentConfigurationVersionDTO();
        request.setName("release-2");
        return request;
    }
}
