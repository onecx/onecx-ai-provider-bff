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
import org.tkit.onecx.ai.bff.rs.mappers.ConfigurationMapper;
import org.tkit.onecx.ai.bff.rs.mappers.ExceptionMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.ConfigurationInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.ConfigurationInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.ConfigurationPageResultInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.ConfigurationSearchCriteriaInternal;
import gen.org.tkit.onecx.ai.management.bff.client.model.UpdateConfigurationRequestInternal;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.ConfigurationApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class ConfigurationRestController implements ConfigurationApiService {
    @Inject
    @RestClient
    ConfigurationInternalApi configurationInternalApi;

    @Inject
    ConfigurationMapper configurationMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createConfiguration(CreateConfigurationRequestDTO createConfigurationRequestDTO) {
        try (Response response = configurationInternalApi
                .createConfiguration(configurationMapper.mapCreate(createConfigurationRequestDTO))) {
            var createdConfiguration = response.readEntity(ConfigurationInternal.class);
            return Response.status(response.getStatus()).entity(configurationMapper.map(createdConfiguration)).build();
        }
    }

    @Override
    public Response deleteConfigurationById(String id) {
        try (Response response = configurationInternalApi.deleteConfiguration(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findConfigurationBySearchCriteria(ConfigurationSearchCriteriaDTO configurationSearchCriteriaDTO) {
        ConfigurationSearchCriteriaInternal searchCriteria = configurationMapper.mapCriteria(configurationSearchCriteriaDTO);
        try (Response searchResponse = configurationInternalApi.findConfigurationBySearchCriteria(searchCriteria)) {
            ConfigurationPageResultInternal aiContextPageResult = searchResponse
                    .readEntity(ConfigurationPageResultInternal.class);
            return Response.status(searchResponse.getStatus()).entity(configurationMapper.mapPageResult(aiContextPageResult))
                    .build();
        }
    }

    @Override
    public Response getConfigurationById(String id) {
        try (Response response = configurationInternalApi.getConfiguration(id)) {
            ConfigurationInternal configuration = response.readEntity(ConfigurationInternal.class);
            ConfigurationDTO configurationDTO = configurationMapper.map(configuration);
            return Response.status(response.getStatus()).entity(configurationDTO).build();
        }
    }

    @Override
    public Response updateConfigurationById(String id, UpdateConfigurationRequestDTO updateConfigurationRequestDTO) {
        UpdateConfigurationRequestInternal updateConfigurationRequest = configurationMapper
                .mapUpdate(updateConfigurationRequestDTO);
        try (Response updateResponse = configurationInternalApi.updateConfiguration(id,
                updateConfigurationRequest)) {
            var responseEntity = updateResponse.readEntity(ConfigurationInternal.class);
            return Response.status(updateResponse.getStatus()).entity(configurationMapper.map(responseEntity)).build();
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
