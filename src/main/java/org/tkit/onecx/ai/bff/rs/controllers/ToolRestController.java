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

import gen.org.tkit.onecx.ai.management.bff.client.api.ToolInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.ToolApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class ToolRestController implements ToolApiService {
    @Inject
    @RestClient
    ToolInternalApi toolInternalApi;

    @Inject
    ToolMapper toolMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createTool(CreateToolRequestDTO createToolRequestDTO) {
        CreateToolRequestInternal createToolRequest = toolMapper.mapCreate(createToolRequestDTO);
        try (Response createResponse = toolInternalApi.createTool(createToolRequest)) {
            var createdTool = createResponse.readEntity(ToolInternal.class);
            return Response.status(createResponse.getStatus()).entity(toolMapper.map(createdTool)).build();
        }
    }

    @Override
    public Response deleteToolById(String id) {
        try (Response response = toolInternalApi.deleteToolById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findToolByCriteria(ToolSearchCriteriaDTO toolSearchCriteriaDTO) {
        ToolSearchCriteriaInternal searchCriteria = toolMapper.mapCriteria(toolSearchCriteriaDTO);
        try (Response searchResponse = toolInternalApi.findToolByCriteria(searchCriteria)) {
            ToolPageResultInternal toolPageResult = searchResponse.readEntity(ToolPageResultInternal.class);
            return Response.status(searchResponse.getStatus()).entity(toolMapper.mapPageResult(toolPageResult)).build();
        }
    }

    @Override
    public Response getToolById(String id) {
        try (Response response = toolInternalApi.getToolById(id)) {
            ToolInternal tool = response.readEntity(ToolInternal.class);
            return Response.status(response.getStatus()).entity(toolMapper.map(tool)).build();
        }
    }

    @Override
    public Response updateToolById(String id, UpdateToolRequestDTO updateToolRequestDTO) {
        UpdateToolRequestInternal updateToolRequest = toolMapper.mapUpdate(updateToolRequestDTO);
        try (Response updateResponse = toolInternalApi.updateToolById(id, updateToolRequest)) {
            var responseEntity = updateResponse.readEntity(ToolInternal.class);
            return Response.status(updateResponse.getStatus()).entity(toolMapper.map(responseEntity)).build();
        }
    }

    @Override
    public Response getDiscoveredTools(String toolId, String agentId) {
        Response.ResponseBuilder responseBuilder = null;
        try (Response response = toolInternalApi.getDiscoveredTools(toolId, agentId)) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                responseBuilder = Response.status(response.getStatus());
            } else {
                var discovered = response.readEntity(DiscoveredToolInfoListInternal.class);
                responseBuilder = Response.ok(toolMapper.mapDiscovered(discovered));
            }
            return responseBuilder.build();
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
