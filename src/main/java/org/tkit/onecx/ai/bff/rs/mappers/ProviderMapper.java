package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ProviderMapper {

    CreateProviderRequestInternal mapCreate(CreateProviderRequestDTO createProviderRequestDTO);

    ProviderDTO map(ProviderInternal createAiContext);

    UpdateProviderRequestInternal mapUpdate(UpdateProviderRequestDTO updateProviderRequestDTO);

    ProviderSearchCriteriaInternal mapCriteria(ProviderSearchCriteriaDTO providerSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    ProviderPageResultDTO mapPageResult(ProviderPageResultInternal providerPageResult);
}
