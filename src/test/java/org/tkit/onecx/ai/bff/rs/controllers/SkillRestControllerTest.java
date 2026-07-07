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
@TestHTTPEndpoint(SkillRestController.class)
class SkillRestControllerTest extends AbstractTest {

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
    void getSkillById_200Test() {
        SkillInternal fakeData = new SkillInternal()
                .id("1").name("skill1").description("desc").instruction("do something");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/skills/" + testId)
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
                .as(SkillDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(fakeData.getName(), response.getName());
        Assertions.assertEquals(fakeData.getInstruction(), response.getInstruction());
    }

    @Test
    void searchSkillTest() {
        SkillSearchCriteriaInternal internalCriteria = new SkillSearchCriteriaInternal();
        internalCriteria.setName("skill1");

        SkillSearchCriteriaDTO criteria = new SkillSearchCriteriaDTO();
        criteria.setName("skill1");

        SkillInternal skill1 = new SkillInternal().id("1").name("skill1");

        SkillPageResultInternal pageResult = new SkillPageResultInternal();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalPages(1L);
        pageResult.setStream(List.of(skill1));
        pageResult.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/skills/search")
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
                .as(SkillPageResultDTO.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(pageResult.getSize(), results.getSize());
        Assertions.assertEquals(skill1.getName(), results.getStream().get(0).getName());
    }

    @Test
    void createSkillTest() {
        CreateSkillRequestInternal createRequest = new CreateSkillRequestInternal();
        createRequest.setName("New Skill");
        createRequest.setDescription("desc");
        createRequest.setInstruction("do something");

        SkillInternal responseSkill = new SkillInternal();
        responseSkill.setId("new-id");
        responseSkill.setName("New Skill");

        mockServerClient.when(
                request().withPath("/internal/skills")
                        .withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json(createRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseSkill)));

        CreateSkillRequestDTO createDTO = new CreateSkillRequestDTO();
        createDTO.setName("New Skill");
        createDTO.setDescription("desc");
        createDTO.setInstruction("do something");

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
                .as(SkillDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseSkill.getName(), response.getName());
    }

    @Test
    void updateSkillTest() {
        UpdateSkillRequestInternal updateRequest = new UpdateSkillRequestInternal();
        updateRequest.setName("Updated Skill");
        updateRequest.setInstruction("do more");
        updateRequest.setModificationCount(0);

        SkillInternal responseSkill = new SkillInternal();
        responseSkill.setId("1");
        responseSkill.setName("Updated Skill");

        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/skills/" + testId)
                        .withMethod(HttpMethod.PUT)
                        .withBody(JsonBody.json(updateRequest)))
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseSkill)));

        UpdateSkillRequestDTO updateDTO = new UpdateSkillRequestDTO();
        updateDTO.setName("Updated Skill");
        updateDTO.setInstruction("do more");
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
                .as(SkillDTO.class);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(responseSkill.getName(), response.getName());
    }

    @Test
    void updateSkillTest_400_ConstraintException() {
        UpdateSkillRequestDTO updateDTO = new UpdateSkillRequestDTO();
        updateDTO.setName("Updated Skill");

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
    void deleteSkillTest() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/skills/" + testId)
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
    void deleteSkill_ClientException_Test() {
        String testId = "1";
        mockServerClient.when(
                request().withPath("/internal/skills/" + testId)
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
