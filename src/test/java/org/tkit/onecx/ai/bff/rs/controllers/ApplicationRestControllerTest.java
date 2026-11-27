package org.tkit.onecx.ai.bff.rs.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.ai.bff.rs.AbstractTest;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.bff.permission.client.model.ApplicationPageResultPermissionSvc;
import gen.org.tkit.onecx.ai.bff.permission.client.model.ApplicationPermissionSvc;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

@QuarkusTest
@LogService
@TestHTTPEndpoint(ApplicationRestController.class)
class ApplicationRestControllerTest extends AbstractTest {

    @InjectMockServerClient
    MockServerClient mockServerClient;

    KeycloakTestClient keycloakTestClient = new KeycloakTestClient();
    static final String MOCK_ID = "APPLICATION_MOCK";

    @AfterEach
    void resetMockserver() {
        try {
            mockServerClient.clear(MOCK_ID);
        } catch (Exception _) {
            // mockId not existing
        }
    }

    @Test
    void getApplications_mapsPermissionApplicationsTest() {
        var app = new ApplicationPermissionSvc();
        app.setProductName("onecx-product");
        app.setAppId("onecx-app");
        app.setName("OneCX App");
        var page = new ApplicationPageResultPermissionSvc();
        page.setStream(List.of(app));
        page.setTotalElements(1L);

        mockServerClient.when(
                request().withPath("/internal/applications/search").withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(page)));

        var result = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ApplicationInfoDTO[].class);

        assertThat(result).hasSize(1);
        assertThat(result[0].getProductName()).isEqualTo("onecx-product");
        assertThat(result[0].getAppId()).isEqualTo("onecx-app");
        assertThat(result[0].getName()).isEqualTo("OneCX App");
    }

    @Test
    void getApplications_fetchesAllPagesTest() {
        var first = new ApplicationPermissionSvc();
        first.setProductName("onecx-product");
        first.setAppId("onecx-app");
        first.setName("First");
        var second = new ApplicationPermissionSvc();
        second.setProductName("onecx-product");
        second.setAppId("onecx-app-2");
        second.setName("Second");
        var firstPage = new ApplicationPageResultPermissionSvc();
        firstPage.setStream(List.of(first));
        firstPage.setTotalElements(2L);
        var secondPage = new ApplicationPageResultPermissionSvc();
        secondPage.setStream(List.of(second));
        secondPage.setTotalElements(2L);

        mockServerClient.when(
                request().withPath("/internal/applications/search").withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json("{\"pageNumber\":0}", MatchType.ONLY_MATCHING_FIELDS)))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(firstPage)));
        mockServerClient.when(
                request().withPath("/internal/applications/search").withMethod(HttpMethod.POST)
                        .withBody(JsonBody.json("{\"pageNumber\":1}", MatchType.ONLY_MATCHING_FIELDS)))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(secondPage)));

        var result = given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().as(gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ApplicationInfoDTO[].class);

        assertThat(result).extracting(gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ApplicationInfoDTO::getName)
                .containsExactly("First", "Second");
    }

    @Test
    void getApplications_returnsBadGatewayWhenPermissionFailsTest() {
        mockServerClient.when(
                request().withPath("/internal/applications/search").withMethod(HttpMethod.POST))
                .withPriority(100)
                .withId(MOCK_ID)
                .respond(response().withStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakTestClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get()
                .then()
                .statusCode(Response.Status.BAD_GATEWAY.getStatusCode());
    }
}
