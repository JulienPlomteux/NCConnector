package com.plomteux.ncconnector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.mapper.CruiseDetailsMapper;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.model.Sailings;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;


@Service
@AllArgsConstructor
@Builder
public class NCService {
    private static final Logger logger = LoggerFactory.getLogger(NCService.class);

    private final RestTemplate restTemplate;
    private final CruiseDetailsMapper cruiseDetailsMapper;
    private final CruiseDetailsRepository cruiseDetailsRepository;

    @Value("${ncl.api.endpoint.itinaries}")
    private String NCL_API_ENDPOINT_ITINARIES;

    @Value("${ncl.fees.multiplier}")
    private BigDecimal FEES_MULTIPLIER;

    @Value("${ncl.api.endpoint.prices}")
    private String NCL_API_ENDPOINT_PRICES;

    public ResponseEntity<List<CruiseDetails>> getAllCruisesDetails() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<List<CruiseDetails>> cruiseDetailsResponse;
        try {
            cruiseDetailsResponse = restTemplate.exchange(
                    this.NCL_API_ENDPOINT_ITINARIES,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );
        } catch (HttpClientErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            logger.warn("HTTP client error occurred while retrieving cruise details: {} - {}", statusCode, e.getMessage());
            return ResponseEntity.status(statusCode).build();
        } catch (HttpServerErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            logger.error("HTTP server error occurred while retrieving cruise details: {} - {}", statusCode, e.getMessage(), e);
            return ResponseEntity.status(statusCode).build();
        } catch (Exception e) {
            logger.error("An error occurred while retrieving cruise details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        List<CruiseDetails> cruiseDetailsList = Objects.requireNonNull(cruiseDetailsResponse.getBody());
        Map<String, BigDecimal> totalPriceMap = fetchTotalPrices(cruiseDetailsList);
        for (CruiseDetails cruiseDetails : cruiseDetailsList) {
            BigDecimal totalPrice = totalPriceMap.getOrDefault(cruiseDetails.getCode(), BigDecimal.ZERO);
            setTotalPrice(cruiseDetails, totalPrice);
        }
        saveCruiseDetailsListInDataBase(cruiseDetailsList);
        return cruiseDetailsResponse;
    }

    void saveCruiseDetailsListInDataBase(List<CruiseDetails> cruiseDetailsList) {
        List<CruiseDetailsEntity> entities = cruiseDetailsList.stream()
                .map(cruiseDetailsMapper::toCruiseDetailsEntity)
                .toList();
        cruiseDetailsRepository.saveAllAndFlush(entities);
    }

    protected Map<String, BigDecimal> fetchTotalPrices(List<CruiseDetails> cruiseDetailsList) {
        List<String> cruiseCodes = cruiseDetailsList.stream()
                .map(CruiseDetails::getCode)
                .toList();
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(NCL_API_ENDPOINT_PRICES, cruiseCodes, JsonNode.class);
            JsonNode pricesNode = Objects.requireNonNull(response.getBody()).get("prices");
            if (pricesNode != null && pricesNode.isArray()) {
                return extractTotalPrices(pricesNode);
            }
        } catch (HttpServerErrorException e) {
            String requestBody = cruiseCodes.toString();
            String errorMessage = String.format("HTTP server error occurred: %s - %s. Request body: %s", e.getStatusCode(), e.getMessage(), requestBody);
            logger.error(errorMessage, e);
        } catch (HttpClientErrorException e) {
            logger.warn("HTTP client warning");
        } catch (Exception e) {
            String requestBody = cruiseCodes.toString();
            String errorMessage = String.format("An error occurred while fetching total prices: %s. Request body: %s", e.getMessage(), requestBody);
            logger.error(errorMessage, e);
        }
        return Collections.emptyMap();
    }
    protected Map<String, BigDecimal> extractTotalPrices(JsonNode pricesNode) {
        Map<String, BigDecimal> totalPriceMap = new HashMap<>();
        for (JsonNode priceNode : pricesNode) {
            String cruiseCode = priceNode.get("cruiseCode").asText();
            BigDecimal totalPrice = priceNode.get("taxesAndFees").get("amount").decimalValue();
            totalPriceMap.put(cruiseCode, totalPrice);
        }
        return totalPriceMap;
    }

    private void setTotalPrice(CruiseDetails cruiseDetails, BigDecimal totalPrice) {
        BigDecimal duration = cruiseDetails.getDuration();
        BigDecimal fees = duration.multiply(FEES_MULTIPLIER);
        List<Sailings> sailings = cruiseDetails.getSailings();
        for (Sailings sailing : sailings) {
            sailing.getPricing().forEach(pricing -> {
                BigDecimal combinedPrice = pricing.getCombinedPrice();
                if (combinedPrice != null) {
                    pricing.setTotalPrice(combinedPrice.add(totalPrice).add(fees));
                }
            });
        }
    }
}