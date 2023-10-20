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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.Null;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
class NCControllerApiImplTest {
    @Mock
    private NCService nCService;
    @Mock
    private CruiseDetailsRepository cruiseDetailsRepository;
    @Mock
    private CruiseDetailsMapper cruiseDetailsMapper;
    @Mock
    private SailingsMapper sailingsMapper;
    @Mock
    private SailingsRepository sailingsRepository;

    @Mock
    private CruiseOverViewMapper cruiseOverViewMapper;

    private NCControllerApiImpl ncController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ncController = new NCControllerApiImpl(nCService, cruiseDetailsRepository, cruiseDetailsMapper, sailingsRepository, sailingsMapper, cruiseOverViewMapper);
    }

    @Test
    void getCruiseDetails_shouldReturnNoContent() {
        // Mocking
        when(nCService.getAllCruisesDetails()).thenReturn(ResponseEntity.noContent().build());

        // Execution
        ResponseEntity<Void> response = ncController.getCruiseDetails();

        // Verification
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(nCService, times(1)).getAllCruisesDetails();
    }

    @Test
    void getDestinationCodes_shouldReturnListOfDestinationCodes() {
        // Mocking
        List<String> expectedDestinationCodes = Collections.singletonList("DEST1");
        when(cruiseDetailsRepository.findUniqueDestinationCodes()).thenReturn(expectedDestinationCodes);

        // Execution
        ResponseEntity<List<String>> response = ncController.getDestinationCodes();

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDestinationCodes, response.getBody());
        verify(cruiseDetailsRepository, times(1)).findUniqueDestinationCodes();
    }

    @Test
    void getCruisePricesByCode_shouldReturnListOfSailings() {
        // Mocking
        String code = "CODE1";
        List<CruiseDetailsEntity> cruiseDetailsEntities = new ArrayList<>();
        CruiseDetailsEntity cruiseDetailsEntity = new CruiseDetailsEntity();
        cruiseDetailsEntity.setSailingsEntities(Collections.singletonList(new SailingsEntity()));
        cruiseDetailsEntities.add(cruiseDetailsEntity);
        List<Sailings> expectedSailings = Collections.singletonList(new Sailings());
        when(cruiseDetailsRepository.findByCode(code)).thenReturn(cruiseDetailsEntities);
        when(sailingsMapper.toSailings(any(SailingsEntity.class))).thenReturn(expectedSailings.get(0));

        // Execution
        ResponseEntity<List<Sailings>> response = ncController.getCruisePricesByCode(code);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedSailings, response.getBody());
        verify(cruiseDetailsRepository, times(1)).findByCode(code);
        verify(sailingsMapper, times(1)).toSailings(any(SailingsEntity.class));
    }

    @Test
    void getSailingsByDestinationAndDeparture_shouldReturnListOfSailings() {
        // Mocking
        LocalDate departureDate = LocalDate.of(2023, 6, 15);
        String destinationCode = "DEST1";
        List<SailingsEntity> sailingsEntities = Collections.singletonList(new SailingsEntity());
        List<Sailings> expectedSailings = Collections.singletonList(new Sailings());
        when(sailingsRepository.findSailingsByDepartureDateAndDestinationCode(departureDate, destinationCode))
                .thenReturn(sailingsEntities);
        when(sailingsMapper.toSailings(any(SailingsEntity.class)))
                .thenReturn(expectedSailings.get(0));
        // Execution
        ResponseEntity<List<Sailings>> response =
                ncController.getSailingsByDestinationAndDeparture(departureDate, destinationCode);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedSailings, response.getBody());
        verify(sailingsRepository, times(1))
                .findSailingsByDepartureDateAndDestinationCode(departureDate, destinationCode);
        verify(sailingsMapper, times(1)).
                toSailings(any(SailingsEntity.class));
    }

    @Test
    void getBestSailingByPriceAndType_shouldReturnSailings() {
        // Mocking
        BigDecimal sailId = BigDecimal.valueOf(1100110);
        String roomType = "inside";
        SailingsEntity sailingsEntity = new SailingsEntity();
        Sailings expectedSailings = new Sailings();
        when(sailingsRepository.findSailingsWithLowestPriceRoomType(sailId, roomType))
                .thenReturn(sailingsEntity);
        when(sailingsMapper.toSailings(any(SailingsEntity.class)))
                .thenReturn(expectedSailings);

        // Execution
        ResponseEntity<Sailings> response =
                ncController.getBestSailingByPriceAndType(sailId, roomType);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedSailings, response.getBody());
        verify(sailingsRepository, times(1))
                .findSailingsWithLowestPriceRoomType(sailId, roomType);
        verify(sailingsMapper, times(1)).
                toSailings(any(SailingsEntity.class));
    }

    @Test
    void getSailingsPriceDrops_shouldReturnCruiseOverView() {
        // Mocking
        BigDecimal percentage = new BigDecimal("0.10");
        LocalDate fromDate = LocalDate.of(2023, 10, 7);
        LocalDate toDate = LocalDate.of(2023, 10, 8);
        String roomType = "inside";

        List<Tuple> sailingsTuples = new ArrayList<>();
        Tuple sailingTuple = mock(Tuple.class);
        sailingsTuples.add(sailingTuple);

        when(sailingsRepository.getSailingsPriceDrops(anyString(), anyString(), eq(percentage), anyString()))
                .thenReturn(sailingsTuples);
        when(sailingTuple.get(0, SailingsEntity.class)).thenReturn(new SailingsEntity());
        when(sailingTuple.get(1, BigDecimal.class)).thenReturn(new BigDecimal("100.00"));
        when(cruiseOverViewMapper.toCruiseOverView(any(SailingsEntity.class)))
                .thenReturn(new CruiseOverView());

        // Execution
        ResponseEntity<List<CruiseOverView>> response = ncController.getSailingsPriceDrops(fromDate, toDate, percentage, roomType);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
        verify(sailingsRepository, times(1)).getSailingsPriceDrops(
                fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                percentage,
                roomType
        );
        verify(cruiseOverViewMapper, times(1)).toCruiseOverView(any(SailingsEntity.class));
    }

    @Test
    void getSailingsPricesBySailId_shouldReturnSailings() {
        // Mocking
        BigDecimal sailId = new BigDecimal("123");

        List<SailingsEntity> sailingsEntities = new ArrayList<>();
        SailingsEntity sailingEntity = new SailingsEntity();
        sailingsEntities.add(sailingEntity);

        when(sailingsRepository.getSailingsPricesBySailId(sailId))
                .thenReturn(sailingsEntities);
        when(sailingsMapper.toSailings(any(SailingsEntity.class)))
                .thenReturn(new Sailings());

        // Execution
        ResponseEntity<List<Sailings>> response = ncController.getSailingsPricesBySailId(sailId);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(sailingsRepository, times(1)).getSailingsPricesBySailId(sailId);
        verify(sailingsMapper, times(1)).toSailings(any(SailingsEntity.class));
    }

}

