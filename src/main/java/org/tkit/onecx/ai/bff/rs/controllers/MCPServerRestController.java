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
import org.tkit.onecx.ai.bff.rs.mappers.MCPServerMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.McpServerInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.McpServerApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class MCPServerRestController implements McpServerApiService {
    @Inject
    @RestClient
    McpServerInternalApi mcpServerInternalApi;

    @Inject
    MCPServerMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createMCPServer(CreateMCPServerRequestDTO createMCPServerRequestDTO) {
        CreateMCPServerRequestInternal createMCPServerRequest = mapper.mapCreate(createMCPServerRequestDTO);
        try (Response createResponse = mcpServerInternalApi.createMCPServer(createMCPServerRequest)) {
            var createdMCPServer = createResponse.readEntity(MCPServerInternal.class);
            return Response.status(createResponse.getStatus()).entity(mapper.map(createdMCPServer)).build();
        }
    }

    @Override
    public Response deleteMCPServerById(String id) {
        try (Response response = mcpServerInternalApi.deleteMCPServerById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findMCPServerBySearchCriteria(MCPServerSearchCriteriaDTO mcPServerSearchCriteriaDTO) {
        MCPServerSearchCriteriaInternal searchCriteria = mapper.mapCriteria(mcPServerSearchCriteriaDTO);
        try (Response searchResponse = mcpServerInternalApi.findMCPServerByCriteria(searchCriteria)) {
            MCPServerPageResultInternal mcpServerPageResult = searchResponse
                    .readEntity(MCPServerPageResultInternal.class);

            return Response.status(searchResponse.getStatus()).entity(mapper.mapPageResult(mcpServerPageResult)).build();
        }
    }

    @Override
    public Response getMCPServerById(String id) {
        try (Response response = mcpServerInternalApi.getMCPServerById(id)) {
            MCPServerInternal mcpServer = response.readEntity(MCPServerInternal.class);
            return Response.status(response.getStatus()).entity(mapper.map(mcpServer)).build();
        }
    }

    @Override
    public Response updateMCPServerById(String id, UpdateMCPServerRequestDTO updateMCPServerRequestDTO) {
        UpdateMCPServerRequestInternal updateMCPServerRequest = mapper.mapUpdate(updateMCPServerRequestDTO);
        try (Response updateResponse = mcpServerInternalApi.updateMCPServerById(id,
                updateMCPServerRequest)) {
            var createdMcpServer = updateResponse.readEntity(MCPServerInternal.class);
            return Response.status(updateResponse.getStatus()).entity(mapper.map(createdMcpServer)).build();
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
