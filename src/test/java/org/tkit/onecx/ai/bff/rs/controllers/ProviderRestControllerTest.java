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
@TestHTTPEndpoint(ProviderRestController.class)
class ProviderRestControllerTest extends AbstractTest {

    private static final String AI_PROVIDER_API_BASE_PATH = "/internal/ai/ai-providers";

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
    void getProviderById_200Test() {
        ProviderInternal fakeData = new ProviderInternal().name("provider1").modelName("llama2")
                .type(ProviderTypeInternal.OLLAMA);

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/providers/" + testId)
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
                .as(ProviderDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
        Assertions.assertEquals(fakeData.getModelName(), response.getModelName());
        Assertions.assertEquals(fakeData.getType().name(), response.getType().name());
    }

    @Test
    void searchProviderTest() {
        ProviderSearchCriteriaInternal requestDTO = new ProviderSearchCriteriaInternal();
        requestDTO.setName("Provider1");

        ProviderSearchCriteriaDTO criteria = new ProviderSearchCriteriaDTO();
        criteria.setName("Provider1");

        ProviderInternal provider1 = new ProviderInternal().name("Provider1").description("desc").modelName("model");

        ProviderPageResultInternal pageResult = new ProviderPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(java.util.List.of(provider1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/providers/search")
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
                .body(requestDTO)
                .post("/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(ProviderPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(pageResult.getSize(), results.getSize());
        Assertions.assertEquals(provider1.getName(), results.getStream().get(0).getName());
    }

    @Test
    void createProviderTest() {
        CreateProviderRequestInternal createRequest = new CreateProviderRequestInternal();
        createRequest.setName("New Provider");
        createRequest.setDescription("desc");
        createRequest.setLlmUrl("url");
        createRequest.setModelName("model");
        createRequest.setApiKey("key");
        createRequest.setType(ProviderTypeInternal.OLLAMA);

        ProviderInternal responseProvider = new ProviderInternal();
        responseProvider.setId("new-id");
        responseProvider.setName("New Provider");

        mockServerClient.when(
                request().withPath("/internal/providers")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseProvider)));

        CreateProviderRequestDTO createDTO = new CreateProviderRequestDTO();
        createDTO.setName("New Provider");
        createDTO.setDescription("desc");
        createDTO.setLlmUrl("url");
        createDTO.setModelName("model");
        createDTO.setApiKey("key");
        createDTO.setType(ProviderTypeDTO.OLLAMA);

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
                .as(ProviderDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseProvider.getName(), response.getName());
    }

    @Test
    void updateProviderTest() {

        UpdateProviderRequestInternal updateRequest = new UpdateProviderRequestInternal();
        updateRequest.setName("Updated Provider");
        updateRequest.setDescription("desc");
        updateRequest.setLlmUrl("url");
        updateRequest.setModelName("model");
        updateRequest.setApiKey("key");
        updateRequest.setModificationCount(0);

        ProviderInternal responseProvider = new ProviderInternal();
        responseProvider.setId("1");
        responseProvider.setName("Updated Provider");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/providers/" + testId)
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseProvider)));

        UpdateProviderRequestDTO updateDTO = new UpdateProviderRequestDTO();
        updateDTO.setName("Updated Provider");
        updateDTO.setDescription("desc");
        updateDTO.setLlmUrl("url");
        updateDTO.setModelName("model");
        updateDTO.setApiKey("key");
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
                .as(ProviderDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseProvider.getName(), response.getName());
    }

    @Test
    void updateProviderTest_400_ConstraintException() {

        UpdateProviderRequestDTO updateDTO = new UpdateProviderRequestDTO();
        updateDTO.setName("Updated Provider");
        updateDTO.setDescription("desc");
        updateDTO.setLlmUrl("url");
        updateDTO.setModelName("model");
        updateDTO.setApiKey("key");

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
    void deleteProviderTest() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/providers/" + testId)
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
    void deleteProvider_ClientException_Test() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/providers/" + testId)
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
