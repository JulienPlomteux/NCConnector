package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.DestinationCodeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface DestinationCodeMapper {
    DestinationCodeMapper INSTANCE = Mappers.getMapper( DestinationCodeMapper.class );
    DestinationCodeEntity toDestinationCodeEntity(String destinationCode);
}
