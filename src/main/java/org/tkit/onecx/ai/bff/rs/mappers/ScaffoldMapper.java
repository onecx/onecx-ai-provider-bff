package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { SkillMapper.class, OffsetDateTimeMapper.class })
public interface ScaffoldMapper {

    CreateScaffoldRequestInternal mapCreate(CreateScaffoldRequestDTO createScaffoldRequestDTO);

    UpdateScaffoldRequestInternal mapUpdate(UpdateScaffoldRequestDTO updateScaffoldRequestDTO);

    ScaffoldSearchCriteriaInternal mapCriteria(ScaffoldSearchCriteriaDTO scaffoldSearchCriteriaDTO);

    @Mapping(target = "removeSkillsItem", ignore = true)
    ScaffoldDTO map(ScaffoldInternal scaffold);

    @Mapping(target = "removeStreamItem", ignore = true)
    ScaffoldPageResultDTO mapPageResult(ScaffoldPageResultInternal scaffoldPageResult);
}
