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
import org.tkit.onecx.ai.bff.rs.mappers.ScaffoldMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.ScaffoldInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.ScaffoldApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class ScaffoldRestController implements ScaffoldApiService {
    @Inject
    @RestClient
    ScaffoldInternalApi scaffoldInternalApi;

    @Inject
    ScaffoldMapper scaffoldMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createScaffold(CreateScaffoldRequestDTO createScaffoldRequestDTO) {
        CreateScaffoldRequestInternal createScaffoldRequest = scaffoldMapper.mapCreate(createScaffoldRequestDTO);
        try (Response createResponse = scaffoldInternalApi.createScaffold(createScaffoldRequest)) {
            var createdScaffold = createResponse.readEntity(ScaffoldInternal.class);
            return Response.status(createResponse.getStatus()).entity(scaffoldMapper.map(createdScaffold)).build();
        }
    }

    @Override
    public Response deleteScaffoldById(String id) {
        try (Response response = scaffoldInternalApi.deleteScaffoldById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findScaffoldByCriteria(ScaffoldSearchCriteriaDTO scaffoldSearchCriteriaDTO) {
        ScaffoldSearchCriteriaInternal searchCriteria = scaffoldMapper.mapCriteria(scaffoldSearchCriteriaDTO);
        try (Response searchResponse = scaffoldInternalApi.findScaffoldByCriteria(searchCriteria)) {
            ScaffoldPageResultInternal scaffoldPageResult = searchResponse.readEntity(ScaffoldPageResultInternal.class);
            return Response.status(searchResponse.getStatus()).entity(scaffoldMapper.mapPageResult(scaffoldPageResult))
                    .build();
        }
    }

    @Override
    public Response getScaffoldById(String id) {
        try (Response response = scaffoldInternalApi.getScaffoldById(id)) {
            ScaffoldInternal scaffold = response.readEntity(ScaffoldInternal.class);
            return Response.status(response.getStatus()).entity(scaffoldMapper.map(scaffold)).build();
        }
    }

    @Override
    public Response updateScaffoldById(String id, UpdateScaffoldRequestDTO updateScaffoldRequestDTO) {
        UpdateScaffoldRequestInternal updateScaffoldRequest = scaffoldMapper.mapUpdate(updateScaffoldRequestDTO);
        try (Response updateResponse = scaffoldInternalApi.updateScaffoldById(id, updateScaffoldRequest)) {
            var responseEntity = updateResponse.readEntity(ScaffoldInternal.class);
            return Response.status(updateResponse.getStatus()).entity(scaffoldMapper.map(responseEntity)).build();
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
