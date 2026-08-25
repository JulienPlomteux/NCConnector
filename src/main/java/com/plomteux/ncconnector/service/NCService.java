package com.plomteux.ncconnector.service;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.mapper.CruiseDetailsMapper;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.model.Sailings;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class NCService {

    private static final int MAX_ATTEMPTS = 3;

    private final RestTemplate restTemplate;
    private final CruiseDetailsMapper cruiseDetailsMapper;
    private final CruiseDetailsRepository cruiseDetailsRepository;

    @Value("${ncl.api.endpoint.itinaries}")
    private String NCL_API_ENDPOINT_ITINARIES;

    @Value("${ncl.api.retry.backoff.millis:5000}")
    private long retryBackoffMillis;

    public ResponseEntity<List<CruiseDetails>> getAllCruisesDetails() {
        ResponseEntity<List<CruiseDetails>> cruiseDetailsResponse = fetchCruiseDetailsWithRetry();
        List<CruiseDetails> cruiseDetailsList = cruiseDetailsResponse.getBody();
        if (cruiseDetailsList == null) {
            return cruiseDetailsResponse;
        }
        applyTotalPrices(cruiseDetailsList);
        saveCruiseDetailsListInDataBase(cruiseDetailsList);
        return cruiseDetailsResponse;
    }

    private ResponseEntity<List<CruiseDetails>> fetchCruiseDetailsWithRetry() {
        HttpStatusCode lastStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                return restTemplate.exchange(
                        this.NCL_API_ENDPOINT_ITINARIES,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<>() {
                        }
                );
            } catch (HttpClientErrorException e) {
                log.warn("HTTP client error occurred while retrieving cruise details: {} - {}", e.getStatusCode(), e.getMessage());
                return ResponseEntity.status(e.getStatusCode()).build();
            } catch (HttpServerErrorException e) {
                lastStatus = e.getStatusCode();
                log.error("HTTP server error occurred while retrieving cruise details (attempt {}/{}): {} - {}", attempt, MAX_ATTEMPTS, e.getStatusCode(), e.getMessage(), e);
            } catch (RestClientException e) {
                log.error("An error occurred while retrieving cruise details (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage(), e);
            }
            if (attempt < MAX_ATTEMPTS) {
                sleepBeforeRetry(attempt);
            }
        }
        return ResponseEntity.status(lastStatus).build();
    }

    private void sleepBeforeRetry(int attempt) {
        long millis = retryBackoffMillis * attempt;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void saveCruiseDetailsListInDataBase(List<CruiseDetails> cruiseDetailsList) {
        List<CruiseDetailsEntity> entities = cruiseDetailsList.stream()
                .map(cruiseDetailsMapper::toCruiseDetailsEntity)
                .toList();
        cruiseDetailsRepository.saveAllAndFlush(entities);
    }

    private void applyTotalPrices(List<CruiseDetails> cruiseDetailsList) {
        for (CruiseDetails cruiseDetails : cruiseDetailsList) {
            for (Sailings sailing : cruiseDetails.getSailings()) {
                for (var pricing : sailing.getPricing()) {
                    BigDecimal combinedPrice = pricing.getCombinedPrice();
                    if (combinedPrice != null) {
                        pricing.setTotalPrice(combinedPrice);
                    }
                }
            }
        }
    }
}
