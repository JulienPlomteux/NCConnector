package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.model.Sailings;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

@Component
@Mapper(componentModel = "spring")
public interface SailingsMapper {
    Logger log = LoggerFactory.getLogger(SailingsMapper.class);
    SailingsEntity toSailingsEntity(Sailings sailings);

    @Mapping(target = "pricing", ignore = true)
    Sailings toSailings(SailingsEntity sailingsEntity);

    @AfterMapping
    default void mapTotalPriceToPricingEntities(Sailings sailings, @MappingTarget SailingsEntity sailingsEntity) {
        sailings.getPricing().stream()
                .filter(pricingEntity -> Objects.equals(pricingEntity.getStatus(), "AVAILABLE"))
                .filter(pricingEntity -> pricingEntity.getCode() != null)
                .forEach(pricingEntity -> {
                    switch (pricingEntity.getCode()) {
                        case "STUDIO" -> sailingsEntity.setStudio(pricingEntity.getTotalPrice());
                        case "INSIDE" -> sailingsEntity.setInside(pricingEntity.getTotalPrice());
                        case "OCEANVIEW" -> sailingsEntity.setOceanView(pricingEntity.getTotalPrice());
                        case "BALCONY" -> sailingsEntity.setBalcony(pricingEntity.getTotalPrice());
                        case "MINISUITE" -> sailingsEntity.setMiniSuite(pricingEntity.getTotalPrice());
                        case "SUITE" -> sailingsEntity.setSuite(pricingEntity.getTotalPrice());
                        case "HAVEN" -> sailingsEntity.setHaven(pricingEntity.getTotalPrice());
                        case "SPA" -> sailingsEntity.setSpa(pricingEntity.getTotalPrice());
                        default -> log.error("Unknown pricing entity code: {}", pricingEntity.getCode());
                    }
                });
    }

    @AfterMapping
    default void setCorrectDateFormat(Sailings sailings, @MappingTarget SailingsEntity sailingsEntity) {
        sailingsEntity.setDepartureDate(fromEpochToString(sailingsEntity.getDepartureDate()));
        sailingsEntity.setReturnDate(fromEpochToString(sailingsEntity.getReturnDate()));
    }

    private String fromEpochToString(String dateString) {
        long timestamp = Long.parseLong(dateString);
        Date date = new Date(timestamp);
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return localDate.format(DateTimeFormatter.ISO_DATE);
    }
}
