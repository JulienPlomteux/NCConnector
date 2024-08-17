package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.model.Sailings;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Component
@Mapper(componentModel = "spring")
public interface SailingsMapper {
    Logger log = LoggerFactory.getLogger(SailingsMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cruiseDetailsEntity", ignore = true)
    @Mapping(target = "departureDate", ignore = true)
    @Mapping(target = "returnDate", ignore = true)
    SailingsEntity toSailingsEntity(Sailings sailings);

    @Mapping(target = "pricing", ignore = true)
    Sailings toSailings(SailingsEntity sailingsEntity);

    @BeforeMapping
    default void setCorrectDateFormatBefore(Sailings sailings, @MappingTarget SailingsEntity sailingsEntity) {
        sailingsEntity.setDepartureDate(fromEpochToLocalDate(sailings.getDepartureDate()));
        sailingsEntity.setReturnDate(fromEpochToLocalDate(sailings.getReturnDate()));
    }

    private LocalDate fromEpochToLocalDate(String dateString) {
        long timestamp = Long.parseLong(dateString);
        Date date = new Date(timestamp);
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    }
}
