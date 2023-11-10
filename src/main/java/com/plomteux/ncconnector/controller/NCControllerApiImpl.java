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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@AllArgsConstructor
@RestController
public class NCControllerApiImpl implements NCControllerApi {
    private static final Logger logger = LoggerFactory.getLogger(NCControllerApiImpl.class);
    private final DateTimeFormatter dateTimeFormatter;
    private final NCService nCService;
    private final CruiseDetailsRepository cruiseDetailsRepository;
    private final CruiseDetailsMapper cruiseDetailsMapper;
    private final SailingsRepository sailingsRepository;
    private final SailingsMapper sailingsMapper;
    private final CruiseOverViewMapper cruiseOverViewMapper;

    @CrossOrigin
    @Override
    public ResponseEntity<Void> getCruiseDetails() {
        logger.debug("Received getCruiseDetails request");
        nCService.getAllCruisesDetails();
        return ResponseEntity.noContent().build();
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<String>> getDestinationCodes() {
        logger.debug("Received getDestinationCodes request");
        List<String> uniqueDestinationCodes = cruiseDetailsRepository.findUniqueDestinationCodes();
        return ResponseEntity.ok(uniqueDestinationCodes);
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<CruiseDetails>> getCruisesByDestinationCode(@PathVariable String destinationCode) {
        logger.debug("Received getCruisesByDestinationCode request");
        List<CruiseDetailsEntity> cruises = cruiseDetailsRepository.findByDestinationCode(destinationCode);
        return ResponseEntity.ok(cruises.stream().map(cruiseDetailsMapper::toCruiseDetails).toList());
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<Sailings>> getCruisePricesByCode(@PathVariable String code) {
        logger.debug("Received getCruisePricesByCode request");
        List<CruiseDetailsEntity> cruiseDetails = cruiseDetailsRepository.findByCode(code);
        List<SailingsEntity> sailings = cruiseDetails.stream()
                .map(CruiseDetailsEntity::getSailingsEntities)
                .flatMap(List::stream)
                .toList();
        return ResponseEntity.ok(sailings.stream().map(sailingsMapper::toSailings).toList());
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<Sailings>> getSailingsByDestinationAndDeparture(
            @RequestParam("departureDate") LocalDate departureDate,
            @RequestParam("destinationCode") String destinationCode) {
        logger.debug("Received getSailingsByDestinationAndDeparture request");
        List<SailingsEntity> sailings = sailingsRepository.findSailingsByDepartureDateAndDestinationCode(departureDate.format(dateTimeFormatter), destinationCode);
        return ResponseEntity.ok(sailings.stream().map(sailingsMapper::toSailings).toList());
    }

    @CrossOrigin
    @Override
    public ResponseEntity<Sailings> getBestSailingByPriceAndType(
            @RequestParam("sailId") BigDecimal sailId,
            @RequestParam("roomType") String roomType) {
        logger.debug("Received getBestSailingByPriceAndType request");
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
        logger.debug("Received getSailingsPriceDrops request");
        LocalDate fromDateParsed = fromDate != null ? fromDate : LocalDate.now().minusDays(1);
        LocalDate toDateParsed = toDate != null ? toDate : LocalDate.now();
        roomType = roomType != null ? roomType : "inside";
        List<Tuple> results = sailingsRepository.getSailingsPriceDrops(
                fromDateParsed.format(dateTimeFormatter),
                toDateParsed.format(dateTimeFormatter),
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
        logger.debug("Received getSailingsPricesByCode request");
        List<SailingsEntity> sailings = sailingsRepository.getSailingsPricesBySailId(sailId);
        return ResponseEntity.ok(sailings.stream().map(sailingsMapper::toSailings).toList());
    }
}
