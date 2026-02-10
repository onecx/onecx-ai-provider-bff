package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface MCPServerMapper {

    CreateMCPServerRequestInternal mapCreate(CreateMCPServerRequestDTO createMCPServerRequestDTO);

    MCPServerDTO map(MCPServerInternal createdMCPServer);

    UpdateMCPServerRequestInternal mapUpdate(UpdateMCPServerRequestDTO updateMCPServerRequestDTO);

    MCPServerSearchCriteriaInternal mapCriteria(MCPServerSearchCriteriaDTO mcPServerSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    MCPServerPageResultDTO mapPageResult(MCPServerPageResultInternal mcpServerPageResult);
}
