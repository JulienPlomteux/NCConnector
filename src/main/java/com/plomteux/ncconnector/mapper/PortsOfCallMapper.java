package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.PortsOfCallEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface PortsOfCallMapper {
    PortsOfCallMapper INSTANCE = Mappers.getMapper(PortsOfCallMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cruiseDetailsEntity", ignore = true)
    PortsOfCallEntity toPortsOfCallEntity(String portsOfCall);
}
