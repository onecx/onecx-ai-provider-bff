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
@TestHTTPEndpoint(ModelRestController.class)
class ModelRestControllerTest extends AbstractTest {

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
    void getModelById_200Test() {
        ModelInternal fakeData = new ModelInternal()
                .id("1").name("model1").modelIdentifier("llama2").modelConfig("{}")
                .communicationMode(CommunicationModeInternal.SYNC)
                .provider(new ProviderInternal().name("provider1"));

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/models/" + testId)
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
                .as(ModelDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
        Assertions.assertEquals(fakeData.getCommunicationMode().name(), response.getCommunicationMode().name());
        Assertions.assertEquals("provider1", response.getProvider().getName());
    }

    @Test
    void searchModelTest() {
        ModelSearchCriteriaInternal internalCriteria = new ModelSearchCriteriaInternal();
        internalCriteria.setName("model1");

        ModelSearchCriteriaDTO criteria = new ModelSearchCriteriaDTO();
        criteria.setName("model1");

        ModelInternal model1 = new ModelInternal().id("1").name("model1")
                .communicationMode(CommunicationModeInternal.STREAM);

        ModelPageResultInternal pageResult = new ModelPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(List.of(model1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/models/search")
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
                .as(ModelPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(pageResult.getSize(), results.getSize());
        Assertions.assertEquals(model1.getName(), results.getStream().get(0).getName());
    }

    @Test
    void createModelTest() {
        CreateModelRequestInternal createRequest = new CreateModelRequestInternal();
        createRequest.setName("New Model");
        createRequest.setModelIdentifier("llama2");
        createRequest.setModelConfig("{}");
        createRequest.setCommunicationMode(CommunicationModeInternal.ASYNC);
        createRequest.setProvider(new ProviderInternal().name("provider1"));

        ModelInternal responseModel = new ModelInternal();
        responseModel.setId("new-id");
        responseModel.setName("New Model");

        mockServerClient.when(
                request().withPath("/internal/models")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseModel)));

        CreateModelRequestDTO createDTO = new CreateModelRequestDTO();
        createDTO.setName("New Model");
        createDTO.setModelIdentifier("llama2");
        createDTO.setModelConfig("{}");
        createDTO.setCommunicationMode(CommunicationModeDTO.ASYNC);
        ProviderDTO providerDTO = new ProviderDTO();
        providerDTO.setName("provider1");
        createDTO.setProvider(providerDTO);

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
                .as(ModelDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseModel.getName(), response.getName());
    }

    @Test
    void updateModelTest() {
        UpdateModelRequestInternal updateRequest = new UpdateModelRequestInternal();
        updateRequest.setName("Updated Model");
        updateRequest.setModelIdentifier("llama3");
        updateRequest.setModificationCount(0);

        ModelInternal responseModel = new ModelInternal();
        responseModel.setId("1");
        responseModel.setName("Updated Model");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/models/" + testId)
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseModel)));

        UpdateModelRequestDTO updateDTO = new UpdateModelRequestDTO();
        updateDTO.setName("Updated Model");
        updateDTO.setModelIdentifier("llama3");
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
                .as(ModelDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseModel.getName(), response.getName());
    }

    @Test
    void updateModelTest_400_ConstraintException() {
        UpdateModelRequestDTO updateDTO = new UpdateModelRequestDTO();
        updateDTO.setName("Updated Model");

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
    void deleteModelTest() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/models/" + testId)
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
    void deleteModel_ClientException_Test() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/models/" + testId)
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
