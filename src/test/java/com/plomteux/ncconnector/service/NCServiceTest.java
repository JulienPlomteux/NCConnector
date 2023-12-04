package com.plomteux.ncconnector.service;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.mapper.CruiseDetailsMapper;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Value("${ncl.thread.sleep.time}")
    private Long NCL_THREAD_SLEEP_TIME;
    @Value("${ncl.prevent.sleep.time}")
    private Integer NCL_PREVENT_SLEEP_TIME;
    @Value("${ncl.forbidden.sleep.time}")
    private Integer NCL_FORBIDDEN_SLEEP_TIME;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CruiseDetailsRepository cruiseDetailsRepository;
    @Mock
    private CruiseDetailsMapper cruiseDetailsMapper;
    @InjectMocks
    private NCService ncService;

    @BeforeEach
    void setup() {

        ReflectionTestUtils.setField(ncService, "NCL_API_ENDPOINT_ITINARIES", NCL_API_ENDPOINT_ITINARIES);
        ReflectionTestUtils.setField(ncService, "FEES_MULTIPLIER", FEES_MULTIPLIER);
        ReflectionTestUtils.setField(ncService, "NCL_API_ENDPOINT_PRICES", NCL_API_ENDPOINT_PRICES);
        ReflectionTestUtils.setField(ncService, "NCL_THREAD_SLEEP_TIME", NCL_THREAD_SLEEP_TIME);
        ReflectionTestUtils.setField(ncService, "NCL_PREVENT_SLEEP_TIME", NCL_PREVENT_SLEEP_TIME);
        ReflectionTestUtils.setField(ncService, "NCL_FORBIDDEN_SLEEP_TIME", NCL_FORBIDDEN_SLEEP_TIME);
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
        cruiseDetailsList.add(new CruiseDetails());
        CruiseDetailsEntity cruiseDetailsEntity = new CruiseDetailsEntity();
        when(cruiseDetailsMapper.toCruiseDetailsEntity(any())).thenReturn(cruiseDetailsEntity);
        // Execution
        ncService.saveCruiseDetailsListInDataBase(cruiseDetailsList);
        // Verification
        verify(cruiseDetailsRepository, times(1)).saveAllAndFlush(anyList());
    }
}
