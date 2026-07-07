package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface SkillMapper {

    CreateSkillRequestInternal mapCreate(CreateSkillRequestDTO createSkillRequestDTO);

    UpdateSkillRequestInternal mapUpdate(UpdateSkillRequestDTO updateSkillRequestDTO);

    SkillSearchCriteriaInternal mapCriteria(SkillSearchCriteriaDTO skillSearchCriteriaDTO);

    SkillDTO map(SkillInternal skill);

    @Mapping(target = "removeStreamItem", ignore = true)
    SkillPageResultDTO mapPageResult(SkillPageResultInternal skillPageResult);
}
