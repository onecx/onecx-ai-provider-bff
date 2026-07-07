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
import org.tkit.onecx.ai.bff.rs.mappers.ModelMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.ModelInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.ModelApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class ModelRestController implements ModelApiService {
    @Inject
    @RestClient
    ModelInternalApi modelInternalApi;

    @Inject
    ModelMapper modelMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createModel(CreateModelRequestDTO createModelRequestDTO) {
        CreateModelRequestInternal createModelRequest = modelMapper.mapCreate(createModelRequestDTO);
        try (Response createResponse = modelInternalApi.createModel(createModelRequest)) {
            var createdModel = createResponse.readEntity(ModelInternal.class);
            return Response.status(createResponse.getStatus()).entity(modelMapper.map(createdModel)).build();
        }
    }

    @Override
    public Response deleteModelById(String id) {
        try (Response response = modelInternalApi.deleteModelById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findModelByCriteria(ModelSearchCriteriaDTO modelSearchCriteriaDTO) {
        ModelSearchCriteriaInternal searchCriteria = modelMapper.mapCriteria(modelSearchCriteriaDTO);
        try (Response searchResponse = modelInternalApi.findModelByCriteria(searchCriteria)) {
            ModelPageResultInternal modelPageResult = searchResponse.readEntity(ModelPageResultInternal.class);
            return Response.status(searchResponse.getStatus()).entity(modelMapper.mapPageResult(modelPageResult)).build();
        }
    }

    @Override
    public Response getModelById(String id) {
        try (Response response = modelInternalApi.getModelById(id)) {
            ModelInternal model = response.readEntity(ModelInternal.class);
            return Response.status(response.getStatus()).entity(modelMapper.map(model)).build();
        }
    }

    @Override
    public Response updateModelById(String id, UpdateModelRequestDTO updateModelRequestDTO) {
        UpdateModelRequestInternal updateModelRequest = modelMapper.mapUpdate(updateModelRequestDTO);
        try (Response updateResponse = modelInternalApi.updateModelById(id, updateModelRequest)) {
            var responseEntity = updateResponse.readEntity(ModelInternal.class);
            return Response.status(updateResponse.getStatus()).entity(modelMapper.map(responseEntity)).build();
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
