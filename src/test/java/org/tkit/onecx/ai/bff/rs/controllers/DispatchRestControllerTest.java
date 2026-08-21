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
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@LogService
@TestHTTPEndpoint(DispatchRestController.class)
class DispatchRestControllerTest extends AbstractTest {

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
    void chatTest() {
        var message = new ChatMessageDTO();
        message.setConversationId("conversation-1");
        message.setType(ChatMessageDTO.TypeEnum.USER);
        message.setMessage("hello");

        var request = new ChatRequestDTO();
        request.setChatMessage(message);

        ChatMessageInternal fakeData = new ChatMessageInternal()
                .conversationId("conversation-1")
                .type(ChatMessageInternal.TypeEnum.ASSISTANT)
                .message("ok");

        mockServerClient.when(
                request().withPath("/internal/dispatch/chat")
                        .withMethod(HttpMethod.POST))
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
                .body(request)
                .post()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(ChatMessageDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getMessage(), response.getMessage());
        Assertions.assertEquals(fakeData.getType().name(), response.getType().name());
        Assertions.assertEquals(fakeData.getConversationId(), response.getConversationId());
    }
}
