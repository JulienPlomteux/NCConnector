package com.plomteux.ncconnector.controller;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.entity.SailingsEntity;
import com.plomteux.ncconnector.mapper.CruiseOverViewMapper;
import com.plomteux.ncconnector.mapper.SailingsMapper;
import com.plomteux.ncconnector.model.CruiseOverView;
import com.plomteux.ncconnector.model.Sailings;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import com.plomteux.ncconnector.repository.SailingsRepository;
import com.plomteux.ncconnector.service.NCService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
class NCControllerApiImplTest {
    @Mock
    private NCService nCService;
    @Mock
    private CruiseDetailsRepository cruiseDetailsRepository;
    @Mock
    private SailingsMapper sailingsMapper;
    @Mock
    private SailingsRepository sailingsRepository;
    @Mock
    private CruiseOverViewMapper cruiseOverViewMapper;
    @InjectMocks
    private NCControllerApiImpl ncController;
    @Captor
    ArgumentCaptor<LocalDate> dateCaptor;

    @Test
    public void handleException_ReturnsErrorResponse() {
        // Arrange
        Exception ex = new Exception("Test exception");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // Act
        ResponseEntity<Object> response = handler.handleException(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An internal server error occurred. Please try again later.", response.getBody());
    }
    @Test
    void findCruise_shouldThrowException() {
        // Arrange
        LocalDate departureDate = LocalDate.now();
        LocalDate returnDate = LocalDate.now().plusDays(7);
        BigDecimal priceUpTo = new BigDecimal("1000");
        BigDecimal priceFrom = new BigDecimal("500");
        BigDecimal daysAtSeaMin = new BigDecimal("2");
        BigDecimal daysAtSeaMax = new BigDecimal("5");
        String destinationCode = "DC";
        String departurePort = "DP";

        // Mock the findCruise method to throw an exception
        when(sailingsRepository.findCruise(departureDate, returnDate, destinationCode, priceUpTo, priceFrom, daysAtSeaMin, daysAtSeaMax, departurePort))
                .thenThrow(new RuntimeException("Test exception"));

        // Act and Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            ncController.findCruise(departureDate, returnDate, priceUpTo, priceFrom, daysAtSeaMin, daysAtSeaMax, destinationCode, departurePort);
        });

        // Verify that the exception message is correct
        assertEquals("Test exception", exception.getMessage());
    }
    @Test
    void findCruise_shouldCallRepositoryWithCorrectParameters() {
        // Arrange
        LocalDate departureDate = LocalDate.now();
        LocalDate returnDate = LocalDate.now().plusDays(7);
        BigDecimal priceUpTo = new BigDecimal("1000");
        BigDecimal priceFrom = new BigDecimal("500");
        BigDecimal daysAtSeaMin = new BigDecimal("2");
        BigDecimal daysAtSeaMax = new BigDecimal("5");
        String destinationCode = "DC";
        String departurePort = "DP";

        // Act
        ncController.findCruise(departureDate, returnDate, priceUpTo, priceFrom, daysAtSeaMin, daysAtSeaMax, destinationCode, departurePort);

        // Assert
        verify(sailingsRepository).findCruise(dateCaptor.capture(), dateCaptor.capture(), anyString(), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), anyString());
        List<LocalDate> capturedDates = dateCaptor.getAllValues();
        assertEquals(departureDate, capturedDates.get(0));
        assertEquals(returnDate, capturedDates.get(1));
    }
    @Test
    public void findCruise_shouldReturnCruiseOverView() {
        // Arrange
        LocalDate departureDate = LocalDate.now();
        LocalDate returnDate = LocalDate.now().plusDays(7);
        BigDecimal priceUpTo = new BigDecimal("1000");
        BigDecimal priceFrom = new BigDecimal("500");
        BigDecimal daysAtSeaMin = new BigDecimal("2");
        BigDecimal daysAtSeaMax = new BigDecimal("5");
        String destinationCode = "DC";
        String departurePort = "DP";

        SailingsEntity sailingsEntity = new SailingsEntity();
        when(sailingsRepository.findCruise(departureDate, returnDate, destinationCode, priceUpTo, priceFrom, daysAtSeaMin, daysAtSeaMax, departurePort))
                .thenReturn(Collections.singletonList(sailingsEntity));

        CruiseOverView cruiseOverView = new CruiseOverView();
        when(cruiseOverViewMapper.toCruiseOverView(sailingsEntity)).thenReturn(cruiseOverView);

        // Act
        ResponseEntity<List<CruiseOverView>> response = ncController.findCruise(departureDate, returnDate, priceUpTo, priceFrom, daysAtSeaMin, daysAtSeaMax, destinationCode, departurePort);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.singletonList(cruiseOverView), response.getBody());
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
    void getBestSailingByPriceAndType_shouldReturnSailings() {
        // Mocking
        BigDecimal sailId = BigDecimal.valueOf(1100110);
        String roomType = "inside";
        SailingsEntity sailingsEntity = new SailingsEntity();
        Sailings expectedSailings = new Sailings();
        when(sailingsRepository.findSailingsWithLowestPriceRoomType(sailId, roomType)).thenReturn(sailingsEntity);
        when(sailingsMapper.toSailings(any(SailingsEntity.class))).thenReturn(expectedSailings);

        // Execution
        ResponseEntity<Sailings> response = ncController.getBestSailingByPriceAndType(sailId, roomType);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedSailings, response.getBody());
        verify(sailingsRepository, times(1)).findSailingsWithLowestPriceRoomType(sailId, roomType);
        verify(sailingsMapper, times(1)).toSailings(any(SailingsEntity.class));
    }

    @Test
    void getSailingsPriceDrops_shouldReturnCruiseOverView() {
        // Mocking
        BigDecimal percentage = new BigDecimal("0.10");
        LocalDate fromDate = LocalDate.of(2023, 9, 7);
        LocalDate toDate = LocalDate.of(2023, 10, 8);
        String roomType = "inside";

        SailingsEntity sailingsEntity = new SailingsEntity();
        List<SailingsEntity> sailingsEntities = Collections.singletonList(sailingsEntity);

        when(sailingsRepository.getSailingsPriceDrops(fromDate, toDate, percentage, roomType)).thenReturn(sailingsEntities);

        CruiseOverView expectedCruiseOverView = new CruiseOverView();
        when(cruiseOverViewMapper.toCruiseOverView(any(SailingsEntity.class))).thenReturn(expectedCruiseOverView);

        // Execution
        ResponseEntity<List<CruiseOverView>> response = ncController.getSailingsPriceDrops(fromDate, toDate, percentage, roomType);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.singletonList(expectedCruiseOverView), response.getBody());
        verify(sailingsRepository, times(1)).getSailingsPriceDrops(fromDate, toDate, percentage, roomType);
        verify(cruiseOverViewMapper, times(1)).toCruiseOverView(any(SailingsEntity.class));
    }

    @Test
    void getSailingsByDestinationAndDeparture_shouldReturnListOfCruiseOverViewMapper() {
        // Mocking
        LocalDate departureDate = LocalDate.of(2023, 6, 15);
        String destinationCode = "DEST1";

        List<SailingsEntity> sailingsEntities = Collections.singletonList(new SailingsEntity());
        when(sailingsRepository.findSailingsByDepartureDateAndDestinationCode(departureDate, destinationCode)).thenReturn(sailingsEntities);

        CruiseOverView expectedCruiseOverView = new CruiseOverView();
        when(cruiseOverViewMapper.toCruiseOverView(any(SailingsEntity.class))).thenReturn(expectedCruiseOverView);

        // Execution
        ResponseEntity<List<CruiseOverView>> response = ncController.getSailingsByDestinationAndDeparture(departureDate, destinationCode);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Collections.singletonList(expectedCruiseOverView), response.getBody());
        verify(sailingsRepository).findSailingsByDepartureDateAndDestinationCode(dateCaptor.capture(), anyString());
        assertEquals(departureDate, dateCaptor.getValue());
    }

    @Test
    void getSailingsPricesBySailId_shouldReturnSailings() {
        // Mocking
        BigDecimal sailId = new BigDecimal("123");

        List<SailingsEntity> sailingsEntities = new ArrayList<>();
        SailingsEntity sailingEntity = new SailingsEntity();
        sailingsEntities.add(sailingEntity);

        when(sailingsRepository.getSailingsPricesBySailId(sailId)).thenReturn(sailingsEntities);
        when(sailingsMapper.toSailings(any(SailingsEntity.class))).thenReturn(new Sailings());

        // Execution
        ResponseEntity<List<Sailings>> response = ncController.getSailingsPricesBySailId(sailId);

        // Verification
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(sailingsRepository, times(1)).getSailingsPricesBySailId(sailId);
        verify(sailingsMapper, times(1)).toSailings(any(SailingsEntity.class));
    }
}

