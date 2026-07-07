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
import org.tkit.onecx.ai.bff.rs.mappers.SkillMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.SkillInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.SkillApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class SkillRestController implements SkillApiService {
    @Inject
    @RestClient
    SkillInternalApi skillInternalApi;

    @Inject
    SkillMapper skillMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response createSkill(CreateSkillRequestDTO createSkillRequestDTO) {
        CreateSkillRequestInternal createSkillRequest = skillMapper.mapCreate(createSkillRequestDTO);
        try (Response createResponse = skillInternalApi.createSkill(createSkillRequest)) {
            var createdSkill = createResponse.readEntity(SkillInternal.class);
            return Response.status(createResponse.getStatus()).entity(skillMapper.map(createdSkill)).build();
        }
    }

    @Override
    public Response deleteSkillById(String id) {
        try (Response response = skillInternalApi.deleteSkillById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response findSkillByCriteria(SkillSearchCriteriaDTO skillSearchCriteriaDTO) {
        SkillSearchCriteriaInternal searchCriteria = skillMapper.mapCriteria(skillSearchCriteriaDTO);
        try (Response searchResponse = skillInternalApi.findSkillByCriteria(searchCriteria)) {
            SkillPageResultInternal skillPageResult = searchResponse.readEntity(SkillPageResultInternal.class);
            return Response.status(searchResponse.getStatus()).entity(skillMapper.mapPageResult(skillPageResult)).build();
        }
    }

    @Override
    public Response getSkillById(String id) {
        try (Response response = skillInternalApi.getSkillById(id)) {
            SkillInternal skill = response.readEntity(SkillInternal.class);
            return Response.status(response.getStatus()).entity(skillMapper.map(skill)).build();
        }
    }

    @Override
    public Response updateSkillById(String id, UpdateSkillRequestDTO updateSkillRequestDTO) {
        UpdateSkillRequestInternal updateSkillRequest = skillMapper.mapUpdate(updateSkillRequestDTO);
        try (Response updateResponse = skillInternalApi.updateSkillById(id, updateSkillRequest)) {
            var responseEntity = updateResponse.readEntity(SkillInternal.class);
            return Response.status(updateResponse.getStatus()).entity(skillMapper.map(responseEntity)).build();
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
