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
import org.tkit.onecx.ai.bff.rs.mappers.ExternalAgentMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.ExternalAgentInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.ExternalAgentApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class ExternalAgentRestController implements ExternalAgentApiService {
    @Inject
    @RestClient
    ExternalAgentInternalApi externalAgentInternalApi;

    @Inject
    ExternalAgentMapper externalAgentMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createExternalAgent(CreateExternalAgentRequestDTO createExternalAgentRequestDTO) {
        CreateExternalAgentRequestInternal createExternalAgentRequest = externalAgentMapper
                .mapCreate(createExternalAgentRequestDTO);
        try (Response createResponse = externalAgentInternalApi.createExternalAgent(createExternalAgentRequest)) {
            var createdExternalAgent = createResponse.readEntity(ExternalAgentInternal.class);
            return Response.status(createResponse.getStatus()).entity(externalAgentMapper.map(createdExternalAgent)).build();
        }
    }

    @Override
    public Response deleteExternalAgentById(String id) {
        try (Response response = externalAgentInternalApi.deleteExternalAgentById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findExternalAgentByCriteria(ExternalAgentSearchCriteriaDTO externalAgentSearchCriteriaDTO) {
        ExternalAgentSearchCriteriaInternal searchCriteria = externalAgentMapper.mapCriteria(externalAgentSearchCriteriaDTO);
        try (Response searchResponse = externalAgentInternalApi.findExternalAgentByCriteria(searchCriteria)) {
            ExternalAgentPageResultInternal externalAgentPageResult = searchResponse
                    .readEntity(ExternalAgentPageResultInternal.class);
            return Response.status(searchResponse.getStatus())
                    .entity(externalAgentMapper.mapPageResult(externalAgentPageResult)).build();
        }
    }

    @Override
    public Response getExternalAgentById(String id) {
        try (Response response = externalAgentInternalApi.getExternalAgentById(id)) {
            ExternalAgentInternal externalAgent = response.readEntity(ExternalAgentInternal.class);
            return Response.status(response.getStatus()).entity(externalAgentMapper.map(externalAgent)).build();
        }
    }

    @Override
    public Response updateExternalAgentById(String id, UpdateExternalAgentRequestDTO updateExternalAgentRequestDTO) {
        UpdateExternalAgentRequestInternal updateExternalAgentRequest = externalAgentMapper
                .mapUpdate(updateExternalAgentRequestDTO);
        try (Response updateResponse = externalAgentInternalApi.updateExternalAgentById(id, updateExternalAgentRequest)) {
            var responseEntity = updateResponse.readEntity(ExternalAgentInternal.class);
            return Response.status(updateResponse.getStatus()).entity(externalAgentMapper.map(responseEntity)).build();
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
