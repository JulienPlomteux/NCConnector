package com.plomteux.ncconnector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import com.plomteux.ncconnector.entity.RoomType;
import com.plomteux.ncconnector.mapper.CruiseDetailsMapper;
import com.plomteux.ncconnector.model.CruiseDetails;
import com.plomteux.ncconnector.model.Sailings;
import com.plomteux.ncconnector.repository.CruiseDetailsRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Service
@AllArgsConstructor
@Slf4j
public class NCService {

    private final RestTemplate restTemplate;
    private final HttpHeaders jsonHttpHeaders;
    private final CruiseDetailsMapper cruiseDetailsMapper;
    private final CruiseDetailsRepository cruiseDetailsRepository;

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

    public ResponseEntity<List<CruiseDetails>> getAllCruisesDetails() {
        HttpEntity<String> entity = new HttpEntity<>(jsonHttpHeaders);
        ResponseEntity<List<CruiseDetails>> cruiseDetailsResponse;
        while (true) {
            try {
                cruiseDetailsResponse = restTemplate.exchange(
                        NCL_API_ENDPOINT_ITINARIES,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<>() {
                        }
                );
                break;
            } catch (HttpClientErrorException.Forbidden e) {
                handleForbiddenSleepInstead(NCL_FORBIDDEN_SLEEP_TIME);
            } catch (HttpClientErrorException e) {
                HttpStatusCode statusCode = e.getStatusCode();
                log.warn("HTTP client error occurred while retrieving cruise details: {} - {}", statusCode, e.getMessage());
                return ResponseEntity.status(statusCode).build();
            } catch (HttpServerErrorException e) {
                HttpStatusCode statusCode = e.getStatusCode();
                log.error("HTTP server error occurred while retrieving cruise details: {} - {}", statusCode, e.getMessage(), e);
                return ResponseEntity.status(statusCode).build();
            } catch (Exception e) {
                log.error("An error occurred while retrieving cruise details: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        List<CruiseDetails> cruiseDetailsList = Objects.requireNonNull(cruiseDetailsResponse.getBody());
        fetchTotalPrices(cruiseDetailsList.subList(0, 10));

        saveCruiseDetailsListInDataBase(cruiseDetailsList);
        return cruiseDetailsResponse;
    }

    void saveCruiseDetailsListInDataBase(List<CruiseDetails> cruiseDetailsList) {
        List<CruiseDetailsEntity> entities = cruiseDetailsList.stream()
                .map(cruiseDetailsMapper::toCruiseDetailsEntity)
                .toList();
        cruiseDetailsRepository.saveAllAndFlush(entities);
    }

    protected void fetchTotalPrices(List<CruiseDetails> cruiseDetailsList) {
        handleForbiddenSleepInstead(NCL_PREVENT_SLEEP_TIME);
        long startTime = System.nanoTime();
        int size = cruiseDetailsList.stream()
                .mapToInt(cruiseDetails -> cruiseDetails.getSailings().size() * 3)
                .sum();
        AtomicInteger count = new AtomicInteger(0);

        for (CruiseDetails cruiseDetails : cruiseDetailsList) {
            BigDecimal fees = getFees(cruiseDetails);
            for (Sailings sailing : cruiseDetails.getSailings()) {
                for (RoomType roomType : RoomType.values()) {
                    try {
                        executeWithRetry(() -> {
                            fetchTotalPrice(cruiseDetails, sailing, roomType, fees);
                            count.incrementAndGet();
                            printProgress(count.get(), startTime, size);
                        }, 3);
                    } catch (Exception e) {
                        log.error("Error processing cruise details: {}", e.getMessage(), e);
                    }
                }
            }
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        log.info("Execution time in minutes: {}", TimeUnit.NANOSECONDS.toMinutes(duration));
    }

    private void printProgress(int processed, long startTime, int size) {
        if (processed == 0 || processed % 50 != 0) return;
        long now = System.nanoTime();
        long elapsedTime = now - startTime;
        long estimatedRemainingTimeInNano = (elapsedTime / processed) * (size - processed);
        long estimatedRemainingTimeInMinutes = TimeUnit.NANOSECONDS.toMinutes(estimatedRemainingTimeInNano);
        log.info("{}/{}, estimated remaining time in minutes: {}", processed, size, estimatedRemainingTimeInMinutes);
    }

    private void fetchTotalPrice(CruiseDetails cruiseDetails, Sailings sailing, RoomType roomType, BigDecimal fees) {
        sailing.getPricing().stream()
                .filter(pricing -> pricing.getCode().equals(roomType.getFieldName().toUpperCase()))
                .filter(pricing -> pricing.getStatus().equals("AVAILABLE"))
                .findFirst()
                .ifPresentOrElse(
                        pricing -> {
                            BigDecimal total = fetchTotalPriceFromApi(cruiseDetails, sailing, roomType, fees);
                            switch (roomType) {
                                case INSIDE -> sailing.setInside(total);
                                case OCEANVIEW -> sailing.setOceanView(total);
                                case BALCONY -> sailing.setBalcony(total);
                                default -> throw new IllegalArgumentException("Unsupported room type: " + roomType);
                            }
                        },
                        () -> log.warn("Room type {} not found on:{}", roomType.getFieldName(), cruiseDetails.getCode())
                );
    }

    private BigDecimal fetchTotalPriceFromApi(CruiseDetails cruiseDetails, Sailings sailing, RoomType roomType, BigDecimal fees) {
        Payload payload = null;
        int retryCount = 0;
        while (true) {
            try {
                payload = createPayloadObject(cruiseDetails.getCode(), cruiseDetails.getShipCode(), roomType.getFieldName().toUpperCase(), 2, sailing.getPackageId().longValue(), Long.parseLong(sailing.getDepartureDate()), Long.parseLong(sailing.getReturnDate()));
            } catch (Exception e) {
                log.error("An error occurred while creating the payload {}", cruiseDetails.getCode(), e);
                return null;
            }
            try {
                HttpEntity<Payload> entity = new HttpEntity<>(payload, jsonHttpHeaders);
                ResponseEntity<JsonNode> response = restTemplate.postForEntity(NCL_API_ENDPOINT_PRICES, entity, JsonNode.class);
                BigDecimal total = new BigDecimal(Objects.requireNonNull(response.getBody()).get("quotes").get(0).get("total").asText());
                total = total.add(fees).divide(BigDecimal.valueOf(2));
                return total;
            } catch (HttpClientErrorException.Forbidden | ResourceAccessException e) {
                handleForbiddenSleepInstead(NCL_FORBIDDEN_SLEEP_TIME);
            } catch (HttpClientErrorException.BadRequest e) {
                if (retryCount < 5) {
                    handleForbiddenSleepInstead(5);
                    retryCount++;
                } else {
                    handleException(e, cruiseDetails, payload);
                    break;
                }
            } catch (Exception e) {
                handleException(e, cruiseDetails, payload);
                break;
            } catch (Throwable t) {
                log.error("An unexpected error occurred while fetching total prices: {}", t.getMessage(), t);
                break;
            }
        }
        return null;
    }

    private void handleForbiddenSleepInstead(int seconds) {
        try {
            log.warn("Forbidden sleep for {} seconds", seconds);
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException ie) {
            log.warn("Sleep interrupted", ie);
            Thread.currentThread().interrupt();
        }
    }

    private void handleException(Exception e, CruiseDetails cruiseDetails, Payload payload) {
        if (e instanceof HttpServerErrorException httpServerErrorException) {
            String errorMessage = String.format("HTTP server error occurred: %s - %s. Request body: %s", httpServerErrorException.getStatusCode(), e.getMessage(), cruiseDetails.getCode());
            log.error(errorMessage, e);
        } else if (e instanceof HttpClientErrorException) {
            log.warn(payload.toString(), e);
        } else {
            String errorMessage = String.format("An error occurred while fetching total prices: %s. Request body: %s", e.getMessage(), cruiseDetails.getCode());
            log.error(errorMessage, e);
        }

    }

    private BigDecimal getFees(CruiseDetails cruiseDetails) {
        BigDecimal duration = cruiseDetails.getDuration();
        return duration.multiply(FEES_MULTIPLIER);
    }

    private void executeWithRetry(Runnable task, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                task.run();
                return; // Exit if successful
            } catch (Exception e) {
                attempt++;
                log.error("Error in scheduled task, attempt {}/{}", attempt, maxRetries, e);
                if (attempt >= maxRetries) {
                    log.error("Max retries reached for task {}", task.toString());
                    throw e; // Rethrow if max retries reached
                }
            }
        }
    }

    public Payload createPayloadObject(
            @NonNull String itineraryCode, @NonNull String shipCode, @NonNull String metaId,
            @NonNull Integer numberOfGuests, @NonNull Long sailingId, @NonNull Long departureDate,
            @NonNull Long returnDate) {

        String pricedCategoryCode = metaId.charAt(0) + "X";

        Payload.Stateroom stateroom = Payload.Stateroom.builder()
                .itineraryCode(itineraryCode)
                .shipCode(shipCode)
                .metaId(metaId)
                .numberOfGuests(numberOfGuests)
                .sailing(Payload.Sailing.builder()
                        .sailingId(sailingId)
                        .departureDate(departureDate)
                        .returnDate(returnDate)
                        .build())
                .pricedCategoryCode(pricedCategoryCode)
                .build();

        Payload.StateroomState stateroomState = Payload.StateroomState.builder()
                .itineraryCode(itineraryCode)
                .shipCode(shipCode)
                .build();

        Payload.State state = Payload.State.builder()
                .stateroomStates(new Payload.StateroomState[]{stateroomState})
                .build();

        return Payload.builder()
                .staterooms(new Payload.Stateroom[]{stateroom})
                .state(state)
                .build();
    }
}