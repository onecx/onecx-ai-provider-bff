package org.tkit.onecx.ai.bff.rs.controllers;

import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.ai.bff.rs.mappers.AgentMapper;
import org.tkit.onecx.ai.bff.rs.mappers.ExceptionMapper;
import org.tkit.onecx.ai.bff.rs.mappers.ToolMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.AgentInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.AgentApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class AgentRestController implements AgentApiService {
    @Inject
    @RestClient
    AgentInternalApi agentInternalApi;

    @Inject
    AgentMapper agentMapper;

    @Inject
    ToolMapper toolMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createAgent(CreateAgentRequestDTO createAgentRequestDTO) {
        CreateAgentRequestInternal createAgentRequest = agentMapper.mapCreate(createAgentRequestDTO);
        return consume(agentInternalApi.createAgent(createAgentRequest), createResponse -> {
            if (createResponse.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                return passThroughError(createResponse);
            }
            var createdAgent = createResponse.readEntity(AgentInternal.class);
            return Response.status(createResponse.getStatus()).entity(agentMapper.map(createdAgent)).build();
        });
    }

    @Override
    public Response deleteAgent(String id) {
        try (Response response = agentInternalApi.deleteAgent(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findAgentBySearchCriteria(AgentSearchCriteriaDTO agentSearchCriteriaDTO) {
        AgentSearchCriteriaInternal searchCriteria = agentMapper.mapCriteria(agentSearchCriteriaDTO);
        try (Response searchResponse = agentInternalApi.findAgentBySearchCriteria(searchCriteria)) {
            AgentPageResultInternal agentPageResult = searchResponse.readEntity(AgentPageResultInternal.class);
            return Response.status(searchResponse.getStatus()).entity(agentMapper.mapPageResult(agentPageResult)).build();
        }
    }

    @Override
    public Response createAgentConfigurationVersion(String id,
            CreateAgentConfigurationVersionDTO createAgentConfigurationVersionDTO) {
        var request = agentMapper.mapCreateConfigurationVersion(createAgentConfigurationVersionDTO);
        return consume(agentInternalApi.createAgentConfigurationVersion(id, request), response -> {
            if (response.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                return passThroughError(response);
            }
            var version = response.readEntity(AgentConfigurationVersionInternal.class);
            return Response.status(response.getStatus()).entity(agentMapper.map(version)).build();
        });
    }

    @Override
    public Response getAgentConfigurationVersion(String id, String versionId) {
        return consume(agentInternalApi.getAgentConfigurationVersion(id, versionId), response -> {
            if (response.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                return passThroughError(response);
            }
            var configuration = response.readEntity(AgentConfigurationInternal.class);
            return Response.status(response.getStatus()).entity(agentMapper.map(configuration)).build();
        });
    }

    @Override
    public Response activateAgentConfigurationVersion(String id, String versionId) {
        return consume(agentInternalApi.activateAgentConfigurationVersion(id, versionId), response -> {
            if (response.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                return passThroughError(response);
            }
            var version = response.readEntity(AgentConfigurationVersionInternal.class);
            return Response.status(response.getStatus()).entity(agentMapper.map(version)).build();
        });
    }

    /**
     * Runs the handler on the upstream response and always closes it. Used instead of try-with-resources, which
     * javac instruments with an extra null check on the close path that cannot be reached by tests.
     */
    private static Response consume(Response response, Function<Response, Response> handler) {
        try {
            return handler.apply(response);
        } finally {
            response.close();
        }
    }

    private static Response passThroughError(Response response) {
        var body = response.readEntity(String.class);
        var mediaType = response.getMediaType() != null ? response.getMediaType() : MediaType.APPLICATION_JSON_TYPE;
        return Response.status(response.getStatus()).type(mediaType).entity(body).build();
    }

    @Override
    public Response getAgentConfigurationVersions(String id) {
        return consume(agentInternalApi.getAgentConfigurationVersions(id), response -> {
            if (response.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
                return passThroughError(response);
            }
            var internalList = response.readEntity(AgentConfigurationVersionListInternal.class);
            var result = new AgentConfigurationVersionListDTO();
            var versions = internalList.getVersions();
            result.setVersions(versions != null
                    ? versions.stream().map(agentMapper::map).toList()
                    : List.of());
            return Response.ok(result).build();
        });
    }

    @Override
    public Response getAgent(String id) {
        try (Response response = agentInternalApi.getAgent(id)) {
            AgentInternal agent = response.readEntity(AgentInternal.class);
            return Response.status(response.getStatus()).entity(agentMapper.map(agent)).build();
        }
    }

    @Override
    public Response updateAgent(String id, UpdateAgentRequestDTO updateAgentRequestDTO) {
        UpdateAgentRequestInternal updateAgentRequest = agentMapper.mapUpdate(updateAgentRequestDTO);
        try (Response updateResponse = agentInternalApi.updateAgent(id, updateAgentRequest)) {
            var responseEntity = updateResponse.readEntity(AgentInternal.class);
            return Response.status(updateResponse.getStatus()).entity(agentMapper.map(responseEntity)).build();
        }
    }

    @Override
    public Response getAgentMcpToolRules(String agentId, String toolId) {
        Response.ResponseBuilder responseBuilder = null;
        try (Response response = agentInternalApi.getAgentMcpToolRules(agentId, toolId)) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                responseBuilder = Response.status(response.getStatus());
            } else {
                var rules = response.readEntity(AgentMcpToolRuleListInternal.class);
                responseBuilder = Response.ok(toolMapper.mapAgentRules(rules));
            }
            return responseBuilder.build();
        }
    }

    @Override
    public Response createAgentMcpToolRule(String agentId, String toolId,
            List<CreateAgentMcpToolRuleRequestDTO> createAgentMcpToolRuleRequestDTOs) {
        var request = toolMapper.mapCreateAgentRules(createAgentMcpToolRuleRequestDTOs);
        Response.ResponseBuilder responseBuilder = null;
        try (Response response = agentInternalApi.createAgentMcpToolRule(agentId, toolId, request)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                responseBuilder = Response.status(response.getStatus());
            } else {
                var rules = response.readEntity(AgentMcpToolRuleListInternal.class);
                responseBuilder = Response.status(Response.Status.CREATED).entity(toolMapper.mapAgentRules(rules));
            }
            return responseBuilder.build();
        }
    }

    @Override
    public Response updateAgentMcpToolRule(String agentId, String toolId,
            List<UpdateAgentMcpToolRuleRequestDTO> updateAgentMcpToolRuleRequestDTOs) {
        var request = toolMapper.mapUpdateAgentRules(updateAgentMcpToolRuleRequestDTOs);
        Response.ResponseBuilder responseBuilder = null;
        try (Response response = agentInternalApi.updateAgentMcpToolRule(agentId, toolId, request)) {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                responseBuilder = Response.status(response.getStatus());
            } else {
                var rules = response.readEntity(AgentMcpToolRuleListInternal.class);
                responseBuilder = Response.ok(toolMapper.mapAgentRules(rules));
            }
            return responseBuilder.build();
        }
    }

    @Override
    public Response deleteAgentMcpToolRule(String agentId, String toolId, String ruleId) {
        try (Response response = agentInternalApi.deleteAgentMcpToolRule(agentId, toolId, ruleId)) {
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
