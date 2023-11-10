package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.PortsOfCallEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface PortsOfCallMapper {
    PortsOfCallMapper INSTANCE = Mappers.getMapper(PortsOfCallMapper.class);

    PortsOfCallEntity toPortsOfCallEntity(String portsOfCall);
}
