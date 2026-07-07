package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { ProviderMapper.class, OffsetDateTimeMapper.class })
public interface ModelMapper {

    CreateModelRequestInternal mapCreate(CreateModelRequestDTO createModelRequestDTO);

    UpdateModelRequestInternal mapUpdate(UpdateModelRequestDTO updateModelRequestDTO);

    ModelSearchCriteriaInternal mapCriteria(ModelSearchCriteriaDTO modelSearchCriteriaDTO);

    ModelDTO map(ModelInternal model);

    @Mapping(target = "removeStreamItem", ignore = true)
    ModelPageResultDTO mapPageResult(ModelPageResultInternal modelPageResult);
}
