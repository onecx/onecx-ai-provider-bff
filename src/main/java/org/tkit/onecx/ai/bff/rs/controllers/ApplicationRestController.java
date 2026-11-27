package org.tkit.onecx.ai.bff.rs.controllers;

import java.util.ArrayList;
import java.util.Objects;

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

    static final int PAGE_SIZE = 1000;

    @Inject
    @RestClient
    ApplicationInternalApi applicationInternalApi;

    @Override
    public Response getApplications() {
        var result = new ArrayList<ApplicationInfoDTO>();
        ApplicationPageResultPermissionSvc page;
        int pageNumber = 0;
        try {
            do {
                page = searchPage(pageNumber++);
                if (page == null) {
                    return badGateway();
                }
                page.getStream().forEach(app -> {
                    var dto = new ApplicationInfoDTO();
                    dto.setProductName(app.getProductName());
                    dto.setAppId(app.getAppId());
                    dto.setName(app.getName());
                    result.add(dto);
                });
            } while (!page.getStream().isEmpty()
                    && result.size() < Objects.requireNonNullElse(page.getTotalElements(), 0L));
            return Response.ok(result).build();
        } catch (WebApplicationException | ProcessingException ex) {
            return badGateway();
        }
    }

    private ApplicationPageResultPermissionSvc searchPage(int pageNumber) {
        var criteria = new ApplicationSearchCriteriaPermissionSvc();
        criteria.setPageNumber(pageNumber);
        criteria.setPageSize(PAGE_SIZE);
        try (Response response = applicationInternalApi.searchApplications(criteria)) {
            return response.getStatus() == Response.Status.OK.getStatusCode()
                    ? response.readEntity(ApplicationPageResultPermissionSvc.class)
                    : null;
        }
    }

    private static Response badGateway() {
        return Response.status(Response.Status.BAD_GATEWAY).build();
    }
}
