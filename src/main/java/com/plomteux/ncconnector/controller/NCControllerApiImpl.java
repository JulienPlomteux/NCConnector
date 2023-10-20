package com.plomteux.ncconnector.controller;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.mapper.CruiseDetailsMapper;
import com.plomteux.ncconnector.mapper.SailingsMapper;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.model.Sailings;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import com.plomteux.ncconnector.service.NCService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
public class NCControllerApiImpl implements NCControllerApi {
    private NCService nCService;
    private static final Logger logger = LoggerFactory.getLogger(NCControllerApiImpl.class);
    private final CruiseDetailsRepository cruiseDetailsRepository;
    private final CruiseDetailsMapper cruiseDetailsMapper;
    private final SailingsMapper sailingsMapper;

    @Override
    public ResponseEntity<List<CruiseDetails>> getCruiseDetails() {
        logger.debug("Received request");
        return nCService.getAllCruisesDetails();
    }

    @Override
    public ResponseEntity<List<String>> getDestinationCodes() {
        List<String> uniqueDestinationCodes = cruiseDetailsRepository.findUniqueDestinationCodes();
        return ResponseEntity.ok(uniqueDestinationCodes);
    }
    @Override
    public ResponseEntity<List<CruiseDetails>> getCruisesByDestinationCode(@PathVariable String destinationCode) {
        List<CruiseDetailsEntity> cruises = cruiseDetailsRepository.findByDestinationCode(destinationCode);
        return ResponseEntity.ok(cruises.stream().map(cruiseDetailsMapper::toCruiseDetails).toList());
    }

    @Override
    public ResponseEntity<List<Sailings>> getCruisePricesByCode(@PathVariable String code) {
        CruiseDetailsEntity cruiseDetails = cruiseDetailsRepository.findByCode(code);
        List<SailingsEntity> sailings = cruiseDetails.getSailingsEntities();
        return ResponseEntity.ok(sailings.stream().map(sailingsMapper::toSailings).toList());
    }

    @Override
    public ResponseEntity<List<CruiseDetails>> getCruisesByDestinationAndDeparture(
            @RequestParam("departureDate") String departureDate,
            @RequestParam("destinationCode") String destinationCode) {
        List<CruiseDetailsEntity> cruises = cruiseDetailsRepository.findByDepartureDateAndDestinationCode(departureDate, destinationCode);
        return ResponseEntity.ok(cruises.stream().map(cruiseDetailsMapper::toCruiseDetails).toList());
    }
}
