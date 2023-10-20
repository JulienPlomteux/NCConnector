package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.entity.DestinationCodeEntity;
import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.model.CruiseOverView;
import com.plomteux.ncconnector.model.Sailings;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring", uses = {SailingsMapper.class, CruiseDetailsMapper.class, CruiseOverViewMapper.class})
public interface CruiseOverViewMapper {

    @Mapping(target = "duration", source = "sailingsEntity.cruiseDetailsEntity.duration")
    @Mapping(target = "embarkationPortCode", source = "sailingsEntity.cruiseDetailsEntity.embarkationPortCode")
    @Mapping(target = "guestCount", source = "sailingsEntity.cruiseDetailsEntity.guestCount")
    @Mapping(target = "price", source = "sailingsEntity.oldPrice")
    CruiseOverView toCruiseOverView(SailingsEntity sailingsEntity);

    @AfterMapping
    default void addedMapping(SailingsEntity sailingsEntity, @MappingTarget CruiseOverView cruiseOverView) {
        CruiseDetailsEntity cruiseDetailsEntity = sailingsEntity.getCruiseDetailsEntity();
        cruiseOverView.setDestinationCodes(
                cruiseDetailsEntity.getDestinationsEntities().stream()
                .map(DestinationCodeEntity::getDestinationCode)
                .toList());
    }
}
