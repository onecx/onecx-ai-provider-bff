package org.tkit.onecx.ai.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.management.bff.client.model.*;
import gen.org.tkit.onecx.ai.management.bff.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ConfigurationMapper {

    @Mapping(target = "removeMcpServersItem", ignore = true)
    ConfigurationDTO map(ConfigurationInternal configuration);

    UpdateConfigurationRequestInternal mapUpdate(UpdateConfigurationRequestDTO updateConfigurationRequestDTO);

    ConfigurationSearchCriteriaInternal mapCriteria(ConfigurationSearchCriteriaDTO configurationSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    ConfigurationPageResultDTO mapPageResult(ConfigurationPageResultInternal aiContextPageResult);

    CreateConfigurationRequestInternal mapCreate(CreateConfigurationRequestDTO createConfigurationRequestDTO);
}
