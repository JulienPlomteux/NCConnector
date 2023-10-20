package com.plomteux.ncconnector.mapper;

import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.model.Sailings;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@Component
@Mapper(componentModel = "spring")
public interface SailingsMapper {
//    @Mapping(target = "pricingEntities", source = "pricing")
    SailingsEntity toSailingsEntity(Sailings sailings);

    @Mapping(target = "pricing", ignore = true)
    Sailings toSailings(SailingsEntity sailingsEntity);
//    List<PricingEntity> toPricingEntity(List<Pricing> pricing);

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
                    }
                });
    }

    @AfterMapping
    default void setCorrectDateFormat(Sailings sailings, @MappingTarget SailingsEntity sailingsEntity) {
        sailingsEntity.setDepartureDate(fromEpochToString(sailingsEntity.getDepartureDate()));
        sailingsEntity.setReturnDate(fromEpochToString(sailingsEntity.getReturnDate()));
    }

    private String fromEpochToString(String dateString){
        long timestamp = Long.parseLong(dateString);
        Date date = new Date(timestamp);
        return new SimpleDateFormat("yyyy-MM-dd").format(date);

    }
}
