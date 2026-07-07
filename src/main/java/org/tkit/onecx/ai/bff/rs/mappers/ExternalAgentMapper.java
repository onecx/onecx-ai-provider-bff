package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { AgentGroupMapper.class, OffsetDateTimeMapper.class })
public interface ExternalAgentMapper {

    CreateExternalAgentRequestInternal mapCreate(CreateExternalAgentRequestDTO createExternalAgentRequestDTO);

    UpdateExternalAgentRequestInternal mapUpdate(UpdateExternalAgentRequestDTO updateExternalAgentRequestDTO);

    ExternalAgentSearchCriteriaInternal mapCriteria(ExternalAgentSearchCriteriaDTO externalAgentSearchCriteriaDTO);

    @Mapping(target = "removeGroupsItem", ignore = true)
    ExternalAgentDTO map(ExternalAgentInternal externalAgent);

    @Mapping(target = "removeStreamItem", ignore = true)
    ExternalAgentPageResultDTO mapPageResult(ExternalAgentPageResultInternal externalAgentPageResult);
}
