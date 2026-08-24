package org.tkit.onecx.ai.bff.rs.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.bff.rs.mappers.ExceptionMapper;
import org.tkit.onecx.ai.bff.rs.mappers.ToolMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.DangerPatternInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.DangerPatternInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.DangerPatternListInternal;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.DangerPatternApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.CreateDangerPatternRequestDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.UpdateDangerPatternRequestDTO;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class DangerPatternRestController implements DangerPatternApiService {

    @Inject
    @RestClient
    DangerPatternInternalApi dangerPatternInternalApi;

    @Inject
    ToolMapper toolMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response getDangerPatterns() {
        try (Response response = dangerPatternInternalApi.getDangerPatterns()) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                return Response.status(response.getStatus()).build();
            }
            var patterns = response.readEntity(DangerPatternListInternal.class);
            return Response.ok(toolMapper.mapPatterns(patterns)).build();
        }
    }

    @Override
    public Response createDangerPattern(CreateDangerPatternRequestDTO createDangerPatternRequestDTO) {
        var request = toolMapper.mapCreatePattern(createDangerPatternRequestDTO);
        try (Response response = dangerPatternInternalApi.createDangerPattern(request)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                return Response.status(response.getStatus()).build();
            }
            var pattern = response.readEntity(DangerPatternInternal.class);
            return Response.status(Response.Status.CREATED).entity(toolMapper.map(pattern)).build();
        }
    }

    @Override
    public Response updateDangerPattern(String id, UpdateDangerPatternRequestDTO updateDangerPatternRequestDTO) {
        var request = toolMapper.mapUpdatePattern(updateDangerPatternRequestDTO);
        try (Response response = dangerPatternInternalApi.updateDangerPattern(id, request)) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                return Response.status(response.getStatus()).build();
            }
            var pattern = response.readEntity(DangerPatternInternal.class);
            return Response.ok(toolMapper.map(pattern)).build();
        }
    }

    @Override
    public Response deleteDangerPattern(String id) {
        try (Response response = dangerPatternInternalApi.deleteDangerPattern(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @ServerExceptionMapper
    public Response restException(ClientWebApplicationException ex) {
        return exceptionMapper.clientException(ex);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraintException(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }
}
