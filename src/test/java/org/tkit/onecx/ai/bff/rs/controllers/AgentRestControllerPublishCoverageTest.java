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
import gen.org.tkit.onecx.ai.management.bff.client.model.CreateAgentConfigurationVersionInternal;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.PublishAgentConfigurationVersionRequestDTO;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@TestHTTPEndpoint(AgentRestController.class)
class AgentRestControllerPublishCoverageTest extends AbstractTest {

    private final KeycloakTestClient keycloakTestClient = new KeycloakTestClient();

    @Test
    void returnsCreatedWhenVersionIsActivated() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(true);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(request())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode());
    }

    @Test
    void propagatesUnexpectedCreateStatus() {
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
    void reportsCloseFailureAfterUnexpectedCreateStatus() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        doThrow(new IllegalStateException("could not close create response")).when(createResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void propagatesUnexpectedActivationStatus() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(false);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.NO_CONTENT.getStatusCode());
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(request())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void reportsCloseFailureAfterUnexpectedActivationStatus() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(false);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.NO_CONTENT.getStatusCode());
        doThrow(new IllegalStateException("could not close activation response")).when(activationResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void rejectsInactiveVersionResponse() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(false);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

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
    void reportsCloseFailureAfterInactiveVersionResponse() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(false);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        doThrow(new IllegalStateException("could not close activation response")).when(activationResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void handlesNullCreateResponseFromClient() {
        var api = mock(AgentInternalApi.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(null);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void handlesNullActivationResponseFromClient() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(false);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(null);
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void closesCreateResponseWhenActivationReadFails() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(false);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class))
                .thenThrow(new IllegalStateException("could not read activation response"));
        doThrow(new IllegalStateException("could not close activation response")).when(activationResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void closesActivationResponseCleanlyWhenActivationReadFails() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(false);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class))
                .thenThrow(new IllegalStateException("could not read activation response"));
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void closesCreateResponseWhenCreateReadFails() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class))
                .thenThrow(new IllegalStateException("could not read create response"));
        doThrow(new IllegalStateException("could not close create response")).when(createResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void closesCreateResponseCleanlyWhenCreateReadFails() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class))
                .thenThrow(new IllegalStateException("could not read create response"));
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void reportsCloseFailureFromActivationResponse() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(true);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        doThrow(new IllegalStateException("could not close activation response")).when(activationResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    @Test
    void reportsCloseFailureFromCreateResponse() {
        var api = mock(AgentInternalApi.class);
        var createResponse = mock(Response.class);
        var activationResponse = mock(Response.class);
        var version = new AgentConfigurationVersionInternal().id("version-2").name("release-2").active(true);
        when(api.createAgentConfigurationVersion(eq("1"), any(CreateAgentConfigurationVersionInternal.class)))
                .thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        when(api.activateAgentConfigurationVersion("1", "version-2")).thenReturn(activationResponse);
        when(activationResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(activationResponse.readEntity(AgentConfigurationVersionInternal.class)).thenReturn(version);
        doThrow(new IllegalStateException("could not close create response")).when(createResponse).close();
        QuarkusMock.installMockForType(api, AgentInternalApi.class, RestClient.LITERAL);

        publishAndExpectInternalServerError();
    }

    private void publishAndExpectInternalServerError() {
        given()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(request())
                .post("1/configuration/versions")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    private PublishAgentConfigurationVersionRequestDTO request() {
        var request = new PublishAgentConfigurationVersionRequestDTO();
        request.setName("release-2");
        return request;
    }
}
