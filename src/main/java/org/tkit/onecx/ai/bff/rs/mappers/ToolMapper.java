package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ToolMapper {

    CreateToolRequestInternal mapCreate(CreateToolRequestDTO createToolRequestDTO);

    UpdateToolRequestInternal mapUpdate(UpdateToolRequestDTO updateToolRequestDTO);

    ToolSearchCriteriaInternal mapCriteria(ToolSearchCriteriaDTO toolSearchCriteriaDTO);

    ToolDTO map(ToolInternal tool);

    @Mapping(target = "removeStreamItem", ignore = true)
    ToolPageResultDTO mapPageResult(ToolPageResultInternal toolPageResult);
}
