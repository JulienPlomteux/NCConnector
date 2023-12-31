package com.plomteux.ncconnector.controller;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.mapper.CruiseDetailsMapper;
import com.plomteux.ncconnector.mapper.CruiseOverViewMapper;
import com.plomteux.ncconnector.mapper.SailingsMapper;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.model.CruiseOverView;
import com.plomteux.ncconnector.model.Sailings;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import com.plomteux.ncconnector.repository.SailingsRepository;
import com.plomteux.ncconnector.service.NCService;
import jakarta.persistence.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@RestController
@Slf4j
public class NCControllerApiImpl implements NCControllerApi {
    private final NCService nCService;
    private final CruiseDetailsRepository cruiseDetailsRepository;
    private final CruiseDetailsMapper cruiseDetailsMapper;
    private final SailingsRepository sailingsRepository;
    private final SailingsMapper sailingsMapper;
    private final CruiseOverViewMapper cruiseOverViewMapper;

    @CrossOrigin
    @Override
    public ResponseEntity<Void> getCruiseDetails() {
        log.debug("Received getCruiseDetails request");
        nCService.getAllCruisesDetails();
        return ResponseEntity.noContent().build();
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<String>> getDestinationCodes() {
        log.debug("Received getDestinationCodes request");
        List<String> uniqueDestinationCodes = cruiseDetailsRepository.findUniqueDestinationCodes();
        return ResponseEntity.ok(uniqueDestinationCodes);
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<CruiseDetails>> getCruisesByDestinationCode(@PathVariable String destinationCode) {
        log.debug("Received getCruisesByDestinationCode request");
        List<CruiseDetailsEntity> cruises = cruiseDetailsRepository.findByDestinationCode(destinationCode);
        return ResponseEntity.ok(cruises.stream().map(cruiseDetailsMapper::toCruiseDetails).toList());
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<Sailings>> getCruisePricesByCode(@PathVariable String code) {
        log.debug("Received getCruisePricesByCode request");
        List<CruiseDetailsEntity> cruiseDetails = cruiseDetailsRepository.findByCode(code);
        List<SailingsEntity> sailings = cruiseDetails.stream()
                .map(CruiseDetailsEntity::getSailingsEntities)
                .flatMap(List::stream)
                .toList();
        return ResponseEntity.ok(sailings.stream().map(sailingsMapper::toSailings).toList());
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<CruiseOverView>> getSailingsByDestinationAndDeparture(
            @RequestParam("departureDate") LocalDate departureDate,
            @RequestParam("destinationCode") String destinationCode) {
        log.debug("Received getSailingsByDestinationAndDeparture request");
        List<SailingsEntity> sailings = sailingsRepository.findSailingsByDepartureDateAndDestinationCode(departureDate, destinationCode);
        return ResponseEntity.ok(sailings.stream()
                .map(cruiseOverViewMapper::toCruiseOverView)
                .toList());
    }

    @CrossOrigin
    @Override
    public ResponseEntity<Sailings> getBestSailingByPriceAndType(
            @RequestParam("sailId") BigDecimal sailId,
            @RequestParam("roomType") String roomType) {
        log.debug("Received getBestSailingByPriceAndType request");
        SailingsEntity sailing = sailingsRepository.findSailingsWithLowestPriceRoomType(sailId, roomType);
        return ResponseEntity.ok(sailingsMapper.toSailings(sailing));
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<CruiseOverView>> getSailingsPriceDrops(
            @RequestParam("fromDate") LocalDate fromDate,
            @RequestParam("toDate") LocalDate toDate,
            @RequestParam("percentage") BigDecimal percentage,
            @RequestParam("roomType") String roomType) {
        LocalDate fromDateParsed = fromDate != null ? fromDate : LocalDate.now().minusDays(1);
        LocalDate toDateParsed = toDate != null ? toDate : LocalDate.now();
        roomType = roomType != null ? roomType : "inside";
        if (fromDateParsed.isAfter(toDateParsed)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage cannot be negative");
        }

        log.debug("Received getSailingsPriceDrops request");

        List<Tuple> results = sailingsRepository.getSailingsPriceDrops(
                fromDateParsed,
                toDateParsed,
                percentage,
                roomType
        );

        return ResponseEntity.ok(results.stream()
                .map(result -> {
                    SailingsEntity s = result.get(0, SailingsEntity.class);
                    s.setOldPrice(result.get(1, BigDecimal.class));
                    return s;
                })
                .map(cruiseOverViewMapper::toCruiseOverView)
                .toList());
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<Sailings>> getSailingsPricesBySailId(
            @RequestParam("sailId") BigDecimal sailId) {
        log.debug("Received getSailingsPricesByCode request");
        List<SailingsEntity> sailings = sailingsRepository.getSailingsPricesBySailId(sailId);
        return ResponseEntity.ok(sailings.stream().map(sailingsMapper::toSailings).toList());
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<CruiseOverView>> findCruise(
            @RequestParam("departureDate") LocalDate departureDate,
            @RequestParam("returnDate") LocalDate returnDate,
            @RequestParam("priceUpTo") BigDecimal priceUpTo,
            @RequestParam("priceFrom") BigDecimal priceFrom,
            @RequestParam("daysAtSeaMin") BigDecimal daysAtSeaMin,
            @RequestParam("daysAtSeaMax") BigDecimal daysAtSeaMax,
            @RequestParam("destinationCode") String destinationCode,
            @RequestParam @Size(min = 3, max = 3, message = "Departure port must be exactly 3 characters long") String departurePort) {
        log.debug("Received findCruise request");
        if (departureDate.isAfter(returnDate)) {
            throw new IllegalArgumentException("Departure date cannot be after return date");
        }
        if (priceUpTo.compareTo(priceFrom) < 0) {
            throw new IllegalArgumentException("Price up to cannot be less than price from");
        }
        if (daysAtSeaMax.compareTo(daysAtSeaMin) < 0) {
            throw new IllegalArgumentException("Days at sea max cannot be less than days at sea min");
        }
        List<SailingsEntity> sailings = sailingsRepository.findCruise(departureDate, returnDate, destinationCode, priceUpTo, priceFrom, daysAtSeaMin, daysAtSeaMax, departurePort);
        return ResponseEntity.ok(sailings.stream()
                .map(cruiseOverViewMapper::toCruiseOverView)
                .toList());
    }

}
