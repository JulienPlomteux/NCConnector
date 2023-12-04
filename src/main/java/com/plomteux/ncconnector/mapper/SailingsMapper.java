package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.model.Sailings;
import org.mapstruct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

@Component
@Mapper(componentModel = "spring")
public interface SailingsMapper {
    Logger log = LoggerFactory.getLogger(SailingsMapper.class);

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

//    @AfterMapping
//    default void mapTotalPriceToPricingEntities(Sailings sailings, @MappingTarget SailingsEntity sailingsEntity) {
//
//        sailings.getPricing().stream()
//                .filter(pricingEntity -> Objects.equals(pricingEntity.getStatus(), "AVAILABLE"))
//                .filter(pricingEntity -> pricingEntity.getCode() != null)
//                .forEach(pricingEntity -> {
//                    switch (pricingEntity.getCode()) {
//                        case "INSIDE" -> sailingsEntity.setInside(pricingEntity.getTotalPrice());
//                        case "OCEANVIEW" -> sailingsEntity.setOceanView(pricingEntity.getTotalPrice());
//                        case "BALCONY" -> sailingsEntity.setBalcony(pricingEntity.getTotalPrice());
//                        default -> log.error("Unknown pricing entity code: {}", pricingEntity.getCode());
//                    }
//                });
//    }

    private LocalDate fromEpochToLocalDate(String dateString) {
        long timestamp = Long.parseLong(dateString);
        Date date = new Date(timestamp);
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    }
}
