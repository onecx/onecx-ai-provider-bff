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
@TestHTTPEndpoint(ConfigurationRestController.class)
class ConfigurationControllerTest extends AbstractTest {

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
    void createConfiguration_Test() {
        CreateConfigurationRequestInternal createRequest = new CreateConfigurationRequestInternal();
        createRequest.setName("test config");
        createRequest.setDescription("desc");

        ConfigurationInternal createdConfiguration = new ConfigurationInternal().name("test config").description("desc");

        mockServerClient.when(
                request().withPath("/internal/configurations")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdConfiguration)));

        CreateConfigurationRequestDTO createDTO = new CreateConfigurationRequestDTO();
        createDTO.setName("test config");
        createDTO.setDescription("desc");

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
                .as(ConfigurationDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(createDTO.getName(), response.getName());
    }

    @Test
    void getConfigurationById_200Test() {
        ConfigurationInternal fakeData = new ConfigurationInternal().name("test config").description("desc");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/configurations/" + testId)
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
                .as(ConfigurationInternal.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
    }

    @Test
    void getConfigurationById_404Test() {
        String testId = "non-existent-id";

        mockServerClient.when(
                request().withPath("/internal/configurations/" + testId)
                        .withMethod(HttpMethod.GET))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get(testId)
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void deleteConfigurationTest() {
        String id = "1";
        mockServerClient.when(
                request().withPath("/internal/configurations/" + id)
                        .withMethod(HttpMethod.DELETE))
                .withId(MOCK_ID)
                .respond(httpRequest -> response().withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete(id)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void updateConfigurationTest() {
        UpdateConfigurationRequestInternal updateRequest = new UpdateConfigurationRequestInternal();
        updateRequest.setName("updated name");
        updateRequest.setModificationCount(0);

        ConfigurationInternal configurationInternal = new ConfigurationInternal().name("updated name");
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/configurations/" + testId)
                        .withBody(JsonBody.json(updateRequest))
                        .withMethod(HttpMethod.PUT))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(configurationInternal)));

        UpdateConfigurationRequestDTO updateDTO = new UpdateConfigurationRequestDTO();
        updateDTO.setName("updated name");
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
                .as(ConfigurationDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(updateDTO.getName(), response.getName());
    }

    @Test
    void updateConfiguration_400Test() {
        String testId = "1";

        UpdateConfigurationRequestDTO updateDTO = new UpdateConfigurationRequestDTO();
        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateDTO)
                .put(testId)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void updateConfiguration_404Test() {
        UpdateConfigurationRequestInternal updateRequest = new UpdateConfigurationRequestInternal();
        updateRequest.setName("updated name");
        updateRequest.setModificationCount(0);

        String testId = "non-existent-id";

        mockServerClient.when(
                request().withPath("/internal/configurations/" + testId)
                        .withMethod(HttpMethod.PUT).withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        UpdateConfigurationRequestDTO updateDTO = new UpdateConfigurationRequestDTO();
        updateDTO.setName("updated name");
        updateDTO.setModificationCount(0);

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateDTO)
                .put(testId)
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void searchConfigurationsTest() {
        ConfigurationSearchCriteriaInternal criteria = new ConfigurationSearchCriteriaInternal();
        criteria.setName("Test Configuration");

        ConfigurationAbstractInternal configuration = new ConfigurationAbstractInternal().name("Test Configuration");

        ConfigurationPageResultInternal response = new ConfigurationPageResultInternal();
        response.setNumber(0);
        response.setSize(10);
        response.setTotalPages(1L);
        response.setStream(java.util.List.of(configuration));
        response.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/configurations/search")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(criteria)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(response)));

        ConfigurationSearchCriteriaDTO criteriaDTO = new ConfigurationSearchCriteriaDTO().name("Test Configuration");
        var results = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(criteriaDTO)
                .post("/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .as(ConfigurationPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(1L, results.getTotalElements());
        Assertions.assertEquals("Test Configuration", results.getStream().get(0).getName());
    }

}