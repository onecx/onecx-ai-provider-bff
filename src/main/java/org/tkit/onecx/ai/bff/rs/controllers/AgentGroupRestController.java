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
import org.tkit.onecx.ai.bff.rs.mappers.AgentGroupMapper;
import org.tkit.onecx.ai.bff.rs.mappers.ExceptionMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.AgentGroupInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.AgentGroupApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class AgentGroupRestController implements AgentGroupApiService {
    @Inject
    @RestClient
    AgentGroupInternalApi agentGroupInternalApi;

    @Inject
    AgentGroupMapper agentGroupMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createAgentGroup(CreateAgentGroupRequestDTO createAgentGroupRequestDTO) {
        CreateAgentGroupRequestInternal createAgentGroupRequest = agentGroupMapper.mapCreate(createAgentGroupRequestDTO);
        try (Response createResponse = agentGroupInternalApi.createAgentGroup(createAgentGroupRequest)) {
            var createdAgentGroup = createResponse.readEntity(AgentGroupInternal.class);
            return Response.status(createResponse.getStatus()).entity(agentGroupMapper.map(createdAgentGroup)).build();
        }
    }

    @Override
    public Response deleteAgentGroupById(String id) {
        try (Response response = agentGroupInternalApi.deleteAgentGroupById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findAgentGroupByCriteria(AgentGroupSearchCriteriaDTO agentGroupSearchCriteriaDTO) {
        AgentGroupSearchCriteriaInternal searchCriteria = agentGroupMapper.mapCriteria(agentGroupSearchCriteriaDTO);
        try (Response searchResponse = agentGroupInternalApi.findAgentGroupByCriteria(searchCriteria)) {
            AgentGroupPageResultInternal agentGroupPageResult = searchResponse
                    .readEntity(AgentGroupPageResultInternal.class);
            return Response.status(searchResponse.getStatus()).entity(agentGroupMapper.mapPageResult(agentGroupPageResult))
                    .build();
        }
    }

    @Override
    public Response getAgentGroupById(String id) {
        try (Response response = agentGroupInternalApi.getAgentGroupById(id)) {
            AgentGroupInternal agentGroup = response.readEntity(AgentGroupInternal.class);
            return Response.status(response.getStatus()).entity(agentGroupMapper.map(agentGroup)).build();
        }
    }

    @Override
    public Response updateAgentGroupById(String id, UpdateAgentGroupRequestDTO updateAgentGroupRequestDTO) {
        UpdateAgentGroupRequestInternal updateAgentGroupRequest = agentGroupMapper.mapUpdate(updateAgentGroupRequestDTO);
        try (Response updateResponse = agentGroupInternalApi.updateAgentGroupById(id, updateAgentGroupRequest)) {
            var responseEntity = updateResponse.readEntity(AgentGroupInternal.class);
            return Response.status(updateResponse.getStatus()).entity(agentGroupMapper.map(responseEntity)).build();
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
