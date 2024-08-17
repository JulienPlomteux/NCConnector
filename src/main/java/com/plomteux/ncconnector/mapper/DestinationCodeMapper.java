package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.DestinationCodeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface DestinationCodeMapper {
    DestinationCodeMapper INSTANCE = Mappers.getMapper(DestinationCodeMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cruiseDetailsEntity", ignore = true)
    DestinationCodeEntity toDestinationCodeEntity(String destinationCode);
}
