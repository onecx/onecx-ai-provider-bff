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
@TestHTTPEndpoint(ScaffoldRestController.class)
class ScaffoldRestControllerTest extends AbstractTest {

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
    void getScaffoldById_200Test() {
        ScaffoldInternal fakeData = new ScaffoldInternal()
                .id("1").name("scaffold1").systemPrompt("prompt").sourceProduct("product1")
                .skills(List.of(new SkillInternal().name("skill1").instruction("do")));

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/scaffolds/" + testId)
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
                .as(ScaffoldDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
        Assertions.assertEquals("skill1", response.getSkills().get(0).getName());
    }

    @Test
    void searchScaffoldTest() {
        ScaffoldSearchCriteriaInternal internalCriteria = new ScaffoldSearchCriteriaInternal();
        internalCriteria.setName("scaffold1");

        ScaffoldSearchCriteriaDTO criteria = new ScaffoldSearchCriteriaDTO();
        criteria.setName("scaffold1");

        ScaffoldInternal scaffold1 = new ScaffoldInternal().id("1").name("scaffold1");

        ScaffoldPageResultInternal pageResult = new ScaffoldPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(List.of(scaffold1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/scaffolds/search")
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
                .as(ScaffoldPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(pageResult.getSize(), results.getSize());
        Assertions.assertEquals(scaffold1.getName(), results.getStream().get(0).getName());
    }

    @Test
    void createScaffoldTest() {
        CreateScaffoldRequestInternal createRequest = new CreateScaffoldRequestInternal();
        createRequest.setName("New Scaffold");
        createRequest.setSystemPrompt("prompt");
        createRequest.setSourceProduct("product1");
        createRequest.setSkills(List.of(new SkillInternal().name("skill1")));

        ScaffoldInternal responseScaffold = new ScaffoldInternal();
        responseScaffold.setId("new-id");
        responseScaffold.setName("New Scaffold");

        mockServerClient.when(
                request().withPath("/internal/scaffolds")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseScaffold)));

        CreateScaffoldRequestDTO createDTO = new CreateScaffoldRequestDTO();
        createDTO.setName("New Scaffold");
        createDTO.setSystemPrompt("prompt");
        createDTO.setSourceProduct("product1");
        SkillDTO skillDTO = new SkillDTO();
        skillDTO.setName("skill1");
        createDTO.setSkills(List.of(skillDTO));

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
                .as(ScaffoldDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseScaffold.getName(), response.getName());
    }

    @Test
    void updateScaffoldTest() {
        UpdateScaffoldRequestInternal updateRequest = new UpdateScaffoldRequestInternal();
        updateRequest.setName("Updated Scaffold");
        updateRequest.setSystemPrompt("prompt");
        updateRequest.setModificationCount(0);

        ScaffoldInternal responseScaffold = new ScaffoldInternal();
        responseScaffold.setId("1");
        responseScaffold.setName("Updated Scaffold");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/scaffolds/" + testId)
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseScaffold)));

        UpdateScaffoldRequestDTO updateDTO = new UpdateScaffoldRequestDTO();
        updateDTO.setName("Updated Scaffold");
        updateDTO.setSystemPrompt("prompt");
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
                .as(ScaffoldDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseScaffold.getName(), response.getName());
    }

    @Test
    void updateScaffoldTest_400_ConstraintException() {
        UpdateScaffoldRequestDTO updateDTO = new UpdateScaffoldRequestDTO();
        updateDTO.setName("Updated Scaffold");

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
    void deleteScaffoldTest() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/scaffolds/" + testId)
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
    void deleteScaffold_ClientException_Test() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/scaffolds/" + testId)
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
