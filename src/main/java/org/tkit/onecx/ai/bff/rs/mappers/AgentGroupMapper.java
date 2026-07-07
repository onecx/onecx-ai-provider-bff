package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface AgentGroupMapper {

    CreateAgentGroupRequestInternal mapCreate(CreateAgentGroupRequestDTO createAgentGroupRequestDTO);

    UpdateAgentGroupRequestInternal mapUpdate(UpdateAgentGroupRequestDTO updateAgentGroupRequestDTO);

    AgentGroupSearchCriteriaInternal mapCriteria(AgentGroupSearchCriteriaDTO agentGroupSearchCriteriaDTO);

    AgentGroupDTO map(AgentGroupInternal agentGroup);

    @Mapping(target = "removeStreamItem", ignore = true)
    AgentGroupPageResultDTO mapPageResult(AgentGroupPageResultInternal agentGroupPageResult);
}
