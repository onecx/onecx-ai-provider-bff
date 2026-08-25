package org.tkit.onecx.ai.bff.rs.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.bff.rs.mappers.ExceptionMapper;

import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.ProblemDetailResponseDTO;

class DangerPatternRestControllerUnitTest {

    @Test
    void constraintException_delegatesToExceptionMapper() {
        DangerPatternRestController controller = new DangerPatternRestController();
        ExceptionMapper exceptionMapper = mock(ExceptionMapper.class);
        var dto = new ProblemDetailResponseDTO();
        dto.setErrorCode("CONSTRAINT_VIOLATIONS");
        RestResponse<ProblemDetailResponseDTO> mapped = RestResponse.status(Response.Status.BAD_REQUEST, dto);
        when(exceptionMapper.constraint(any())).thenReturn(mapped);

        java.lang.reflect.Field f;
        try {
            f = DangerPatternRestController.class.getDeclaredField("exceptionMapper");
            f.setAccessible(true);
            f.set(controller, exceptionMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ConstraintViolationException ex = new ConstraintViolationException("validation failed", java.util.Set.of());
        RestResponse<ProblemDetailResponseDTO> result = controller.constraintException(ex);

        assertThat(result.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(result.getEntity()).isNotNull();
        assertThat(result.getEntity().getErrorCode()).isEqualTo("CONSTRAINT_VIOLATIONS");
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
