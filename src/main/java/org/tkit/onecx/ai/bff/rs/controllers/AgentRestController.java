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
import org.tkit.onecx.ai.bff.rs.mappers.AgentMapper;
import org.tkit.onecx.ai.bff.rs.mappers.ExceptionMapper;
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
    ExceptionMapper exceptionMapper;

    @Override
    public Response createAgent(CreateAgentRequestDTO createAgentRequestDTO) {
        CreateAgentRequestInternal createAgentRequest = agentMapper.mapCreate(createAgentRequestDTO);
        try (Response createResponse = agentInternalApi.createAgent(createAgentRequest)) {
            var createdAgent = createResponse.readEntity(AgentInternal.class);
            return Response.status(createResponse.getStatus()).entity(agentMapper.map(createdAgent)).build();
        }
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

    @ServerExceptionMapper
    public Response restException(ClientWebApplicationException ex) {
        return exceptionMapper.clientException(ex);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraintException(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }
}
