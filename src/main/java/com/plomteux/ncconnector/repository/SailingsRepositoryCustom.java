package com.plomteux.ncconnector.repository;

import com.plomteux.ncconnector.entity.SailingsEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SailingsRepositoryCustom {
    List<SailingsEntity> findSailingsByDepartureDateAndDestinationCode(LocalDate departureDate, String destinationCode);
    SailingsEntity findSailingsWithLowestPriceRoomType(BigDecimal sailId, String roomType);
    List<SailingsEntity> getSailingsPriceDrops(LocalDate from, LocalDate to, BigDecimal percentage, String roomType);
    List<SailingsEntity> getSailingsPricesBySailId(BigDecimal sailId);
    List<SailingsEntity> findCruise(LocalDate departureDate, LocalDate returnDate, String destinationCode, BigDecimal priceUpTo, BigDecimal priceFrom, BigDecimal daysAtSeaMin, BigDecimal daysAtSeaMax, String departurePort);
}