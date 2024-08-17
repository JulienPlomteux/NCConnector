package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.entity.DestinationCodeEntity;
import com.plomteux.ncconnector.entity.PortsOfCallEntity;
import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.model.CruiseOverView;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import static com.plomteux.ncconnector.util.ProductLinkBuilder.buildProductViewLink;

@Mapper(componentModel = "spring", uses = {SailingsMapper.class, CruiseDetailsMapper.class})
public interface CruiseOverViewMapper {

    @Mapping(target = "destinations", ignore = true)
    @Mapping(target = "portsOfCall", ignore = true)
    @Mapping(target = "productViewLink", ignore = true)
    @Mapping(target = "duration", source = "sailingsEntity.cruiseDetailsEntity.duration")
    @Mapping(target = "embarkationPort", source = "sailingsEntity.cruiseDetailsEntity.embarkationPortCode")
    @Mapping(target = "priceDrop", source = "sailingsEntity.oldPrice")
    CruiseOverView toCruiseOverView(SailingsEntity sailingsEntity);

    @AfterMapping
    default void addedMapping(SailingsEntity sailingsEntity, @MappingTarget CruiseOverView cruiseOverView) {
        CruiseDetailsEntity cruiseDetailsEntity = sailingsEntity.getCruiseDetailsEntity();
        cruiseOverView.setEmbarkationPort(PortCodeMapper.getCityName(cruiseDetailsEntity.getEmbarkationPortCode()));
        cruiseOverView.setDestinations(
                cruiseDetailsEntity.getDestinationsEntities().stream()
                        .map(DestinationCodeEntity::getDestinationCode)
                        .map(PortCodeMapper::getCityName)
                        .toList()
        );
        cruiseOverView.setPortsOfCall(
                cruiseDetailsEntity.getPortsOfCallEntities().stream()
                        .map(PortsOfCallEntity::getPortsOfCall)
                        .map(PortCodeMapper::getCityName)
                        .toList()
        );
        cruiseOverView.setProductViewLink(
                buildProductViewLink(
                        sailingsEntity.getCruiseDetailsEntity().getCode(),
                        sailingsEntity.getSailId(),
                        sailingsEntity.getDepartureDate())
        );
    }
}
