package com.plomteux.ncconnector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.mapper.CruiseDetailsMapper;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
class NCServiceTest {
    @Value("${ncl.api.endpoint.itinaries}")
    private String NCL_API_ENDPOINT_ITINARIES;

    @Value("${ncl.fees.multiplier}")
    private BigDecimal FEES_MULTIPLIER;

    @Value("${ncl.api.endpoint.prices}")
    private String NCL_API_ENDPOINT_PRICES;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CruiseDetailsRepository cruiseDetailsRepository;
    @Mock
    private CruiseDetailsMapper cruiseDetailsMapper;

    private NCService ncService;
    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        ncService = NCService.builder()
                .cruiseDetailsRepository(cruiseDetailsRepository)
                .FEES_MULTIPLIER(FEES_MULTIPLIER)
                .NCL_API_ENDPOINT_ITINARIES(NCL_API_ENDPOINT_ITINARIES)
                .NCL_API_ENDPOINT_PRICES(NCL_API_ENDPOINT_PRICES)
                .FEES_MULTIPLIER(FEES_MULTIPLIER)
                .restTemplate(restTemplate)
                .cruiseDetailsMapper(cruiseDetailsMapper)
                .build();
    }
    @Test
    void testGetAllCruiseDetails_Success() {
        // Mocking
        List<CruiseDetails> mockCruiseDetailsList = new ArrayList<>();
        ResponseEntity<List<CruiseDetails>> mockResponseEntity = ResponseEntity.ok(mockCruiseDetailsList);
        when(restTemplate.exchange(
                eq(NCL_API_ENDPOINT_ITINARIES),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<CruiseDetails>>() {})
        )).thenReturn(mockResponseEntity);
        // Execution
        ResponseEntity<List<CruiseDetails>> result = ncService.getAllCruisesDetails();
        // Verification
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockCruiseDetailsList, result.getBody());
        verify(cruiseDetailsRepository, times(1)).saveAllAndFlush(anyList());
    }
    @Test
    void testGetAllCruiseDetails_ClientError() {
        // Mocking
        HttpStatus clientErrorStatus = HttpStatus.BAD_REQUEST;
        String errorMessage = "Client Error Message";
        when(restTemplate.exchange(
                eq(NCL_API_ENDPOINT_ITINARIES),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<CruiseDetails>>() {})
        )).thenThrow(new HttpClientErrorException(clientErrorStatus, errorMessage));
        // Execution
        ResponseEntity<List<CruiseDetails>> result = ncService.getAllCruisesDetails();

        // Verification
        assertEquals(clientErrorStatus, result.getStatusCode());
        verify(cruiseDetailsRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void testGetAllCruiseDetails_ServerError() {
        // Mocking
        HttpStatus serverErrorStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = "Server Error Message";

        when(restTemplate.exchange(
                eq(NCL_API_ENDPOINT_ITINARIES),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(new ParameterizedTypeReference<List<CruiseDetails>>() {})
        )).thenThrow(new HttpServerErrorException(serverErrorStatus, errorMessage));

        // Execution
        ResponseEntity<List<CruiseDetails>> result = ncService.getAllCruisesDetails();

        // Verification
        assertEquals(serverErrorStatus, result.getStatusCode());
        verify(cruiseDetailsRepository, never()).saveAllAndFlush(anyList());
    }


    @Test
    void testSaveCruiseDetailsListInDataBase() {
        // Mocking
        List<CruiseDetails> cruiseDetailsList = new ArrayList<>();
        // Execution
        when(cruiseDetailsMapper.toCruiseDetailsEntity(any())).thenReturn(new CruiseDetailsEntity());
        ncService.saveCruiseDetailsListInDataBase(cruiseDetailsList);
        // Verification
        verify(cruiseDetailsRepository, times(1)).saveAllAndFlush(anyList());
    }
    @Test
    void testFetchTotalPrices() {
        // Mocking
        List<CruiseDetails> cruiseDetailsList = new ArrayList<>();
        CruiseDetails cruise1 = new CruiseDetails();
        cruiseDetailsList.add(cruise1);
        // Execution
        ResponseEntity<JsonNode> mockResponseEntity = ResponseEntity.ok(null);
        when(restTemplate.postForEntity(anyString(), anyList(), eq(JsonNode.class))).thenReturn(mockResponseEntity);

        Map<String, BigDecimal> result = ncService.fetchTotalPrices(cruiseDetailsList);
        // Verification
        assertNotNull(result);

    }
}
