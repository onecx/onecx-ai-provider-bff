package org.tkit.onecx.ai.bff.rs.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.bff.permission.client.api.ApplicationInternalApi;
import gen.org.tkit.onecx.ai.bff.permission.client.model.ApplicationPageResultPermissionSvc;
import gen.org.tkit.onecx.ai.bff.permission.client.model.ApplicationSearchCriteriaPermissionSvc;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.ApplicationApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ApplicationInfoDTO;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class ApplicationRestController implements ApplicationApiService {

    static final int MAX_APPLICATIONS = 1000;

    @Inject
    @RestClient
    ApplicationInternalApi applicationInternalApi;

    @Override
    public Response getApplications() {
        var criteria = new ApplicationSearchCriteriaPermissionSvc();
        criteria.setPageNumber(0);
        criteria.setPageSize(MAX_APPLICATIONS);
        try (Response response = applicationInternalApi.searchApplications(criteria)) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                return badGateway();
            }
            var page = response.readEntity(ApplicationPageResultPermissionSvc.class);
            var result = page.getStream().stream()
                    .map(app -> {
                        var dto = new ApplicationInfoDTO();
                        dto.setProductName(app.getProductName());
                        dto.setAppId(app.getAppId());
                        dto.setName(app.getName());
                        return dto;
                    })
                    .toList();
            return Response.ok(result).build();
        } catch (WebApplicationException | ProcessingException ex) {
            return badGateway();
        }
    }

    private static Response badGateway() {
        return Response.status(Response.Status.BAD_GATEWAY).build();
    }
}
