package com.plomteux.ncconnector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.mapper.CruiseDetailsMapper;
import com.plomteux.ncconnector.mapper.DestinationCodeMapper;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.model.Sailings;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;


@Service
@AllArgsConstructor
public class NCService {
    private final RestTemplate restTemplate;
    private final CruiseDetailsMapper cruiseDetailsMapper;
    private final DestinationCodeMapper destinationCodeMapper;
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
                    new ParameterizedTypeReference<>() {}
            );
        } catch (Exception e) {
            // Handle exception or rethrow with custom exception handling
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

    private void saveCruiseDetailsListInDataBase(List<CruiseDetails> cruiseDetailsList) {
        List<CruiseDetailsEntity> entities = cruiseDetailsList.stream()
                .map(cruiseDetailsMapper::toCruiseDetailsEntity)
                .toList();
        cruiseDetailsRepository.saveAllAndFlush(entities);
    }
    private Map<String, BigDecimal> fetchTotalPrices(List<CruiseDetails> cruiseDetailsList) {
        Map<String, BigDecimal> totalPriceMap = new HashMap<>();
        List<String> cruiseCodes = new ArrayList<>();
        for (CruiseDetails cruiseDetails : cruiseDetailsList) {
            cruiseCodes.add(cruiseDetails.getCode());
        }
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(NCL_API_ENDPOINT_PRICES, cruiseCodes, JsonNode.class);
            JsonNode pricesNode = Objects.requireNonNull(response.getBody()).get("prices");
            if (pricesNode != null && pricesNode.isArray()) {
                for (JsonNode priceNode : pricesNode) {
                    String cruiseCode = priceNode.get("cruiseCode").asText();
                    BigDecimal totalPrice = priceNode.get("taxesAndFees").get("amount").decimalValue();
                    totalPriceMap.put(cruiseCode, totalPrice);
                }
            }
        } catch (Exception e) {
            // Handle exception or rethrow with custom exception handling
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