package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.entity.DestinationCodeEntity;
import com.plomteux.ncconnector.entity.PortsOfCallEntity;
import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.model.Sailings;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {SailingsMapper.class})
public interface CruiseDetailsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "destinationsEntities", ignore = true)
    @Mapping(target = "portsOfCallEntities", ignore = true)
    @Mapping(target = "sailingsEntities", source = "sailings")
    CruiseDetailsEntity toCruiseDetailsEntity(CruiseDetails cruiseDetails);

    @Mapping(target = "destinationCodes", ignore = true)
    @Mapping(target = "portsOfCall", ignore = true)
    @Mapping(target = "embarkationPort", ignore = true)
    @Mapping(target = "sailings", source = "sailingsEntities")
    CruiseDetails toCruiseDetails(CruiseDetailsEntity cruiseDetailsEntity);

    List<SailingsEntity> toSailingsEntities(List<Sailings> sailingsList);

    @AfterMapping
    default void addedMapping(CruiseDetails cruiseDetails, @MappingTarget CruiseDetailsEntity cruiseDetailsEntity) {
        cruiseDetailsEntity.setDestinationsEntities(
                cruiseDetails.getDestinationCodes().stream()
                        .map(DestinationCodeMapper.INSTANCE::toDestinationCodeEntity)
                        .toList());
        cruiseDetailsEntity.setPortsOfCallEntities(
                cruiseDetails.getPortsOfCall().stream()
                        .map(PortsOfCallMapper.INSTANCE::toPortsOfCallEntity)
                        .toList());
        cruiseDetailsEntity.setEmbarkationPortCode(cruiseDetails.getEmbarkationPort().getCode());
        cruiseDetailsEntity.getDestinationsEntities().forEach(destinationCodeEntity -> destinationCodeEntity.setCruiseDetailsEntity(cruiseDetailsEntity));
        cruiseDetailsEntity.getPortsOfCallEntities().forEach(portsOfCallEntity -> portsOfCallEntity.setCruiseDetailsEntity(cruiseDetailsEntity));
        cruiseDetailsEntity.getSailingsEntities().forEach(sailingsEntity -> sailingsEntity.setCruiseDetailsEntity(cruiseDetailsEntity));
    }

    @AfterMapping
    default void addedMapping(CruiseDetailsEntity cruiseDetailsEntity, @MappingTarget CruiseDetails cruiseDetails) {
        cruiseDetails.setDestinationCodes(
                cruiseDetailsEntity.getDestinationsEntities().stream()
                        .map(DestinationCodeEntity::getDestinationCode)
                        .toList());
        cruiseDetails.setPortsOfCall(
                cruiseDetailsEntity.getPortsOfCallEntities().stream()
                        .map(PortsOfCallEntity::getPortsOfCall)
                        .toList());
        cruiseDetails.setEmbarkationPortCode(PortCodeMapper.getCityName(cruiseDetailsEntity.getEmbarkationPortCode()));
    }
}
