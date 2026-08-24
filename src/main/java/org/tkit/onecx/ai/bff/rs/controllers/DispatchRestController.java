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
import org.tkit.onecx.ai.bff.rs.mappers.DispatchMapper;
import org.tkit.onecx.ai.bff.rs.mappers.ExceptionMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.ai.management.bff.client.api.DispatchInternalApi;
import gen.org.tkit.onecx.ai.management.bff.client.model.ChatMessageInternal;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.DispatchApiService;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ChatRequestDTO;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ProblemDetailResponseDTO;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class DispatchRestController implements DispatchApiService {
    @Inject
    @RestClient
    DispatchInternalApi dispatchInternalApi;

    @Inject
    DispatchMapper dispatchMapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response chat(ChatRequestDTO chatRequestDTO) {
        var chatRequest = dispatchMapper.map(chatRequestDTO);
        try (Response response = dispatchInternalApi.chat(chatRequest)) {
            var message = response.readEntity(ChatMessageInternal.class);
            return Response.status(response.getStatus()).entity(dispatchMapper.map(message)).build();
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
