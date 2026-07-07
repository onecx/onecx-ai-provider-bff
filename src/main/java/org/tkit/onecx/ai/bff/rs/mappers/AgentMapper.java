package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { ModelMapper.class, ScaffoldMapper.class, ToolMapper.class, AgentGroupMapper.class,
        OffsetDateTimeMapper.class })
public interface AgentMapper {

    CreateAgentRequestInternal mapCreate(CreateAgentRequestDTO createAgentRequestDTO);

    UpdateAgentRequestInternal mapUpdate(UpdateAgentRequestDTO updateAgentRequestDTO);

    AgentSearchCriteriaInternal mapCriteria(AgentSearchCriteriaDTO agentSearchCriteriaDTO);

    @Mapping(target = "removeToolsItem", ignore = true)
    @Mapping(target = "removeGroupsItem", ignore = true)
    AgentDTO map(AgentInternal agent);

    @Mapping(target = "removeStreamItem", ignore = true)
    AgentPageResultDTO mapPageResult(AgentPageResultInternal agentPageResult);
}
