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
import org.tkit.onecx.ai.bff.rs.mappers.ProviderMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.ProviderInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.ProviderApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.CreateProviderRequestDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ProviderSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.UpdateProviderRequestDTO;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class ProviderRestController implements ProviderApiService {
    @Inject
    @RestClient
    ProviderInternalApi providerInternalApi;

    @Inject
    ProviderMapper providerMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createProvider(CreateProviderRequestDTO createProviderRequestDTO) {
        CreateProviderRequestInternal createProviderRequest = providerMapper.mapCreate(createProviderRequestDTO);
        try (Response createResponse = providerInternalApi.createProvider(createProviderRequest)) {
            var createAiContext = createResponse.readEntity(ProviderInternal.class);
            return Response.status(createResponse.getStatus()).entity(providerMapper.map(createAiContext)).build();
        }
    }

    @Override
    public Response deleteProviderById(String id) {
        try (Response response = providerInternalApi.deleteProvider(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findProviderBySearchCriteria(ProviderSearchCriteriaDTO providerSearchCriteriaDTO) {
        ProviderSearchCriteriaInternal searchCriteria = providerMapper.mapCriteria(providerSearchCriteriaDTO);
        try (Response searchResponse = providerInternalApi.findProviderBySearchCriteria(searchCriteria)) {
            ProviderPageResultInternal providerPageResult = searchResponse
                    .readEntity(ProviderPageResultInternal.class);
            return Response.status(searchResponse.getStatus()).entity(providerMapper.mapPageResult(providerPageResult)).build();
        }
    }

    @Override
    public Response getProviderById(String id) {
        try (Response response = providerInternalApi.getProvider(id)) {
            ProviderInternal provider = response.readEntity(ProviderInternal.class);
            return Response.status(response.getStatus()).entity(providerMapper.map(provider)).build();
        }
    }

    @Override
    public Response updateProviderById(String id, UpdateProviderRequestDTO updateProviderRequestDTO) {
        UpdateProviderRequestInternal updateProviderRequest = providerMapper
                .mapUpdate(updateProviderRequestDTO);
        try (Response updateResponse = providerInternalApi.updateProvider(id,
                updateProviderRequest)) {
            var responseEntity = updateResponse.readEntity(ProviderInternal.class);
            return Response.status(updateResponse.getStatus()).entity(providerMapper.map(responseEntity)).build();
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
