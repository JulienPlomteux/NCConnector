package com.plomteux.ncconnector.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
@Builder
public class Payload {
    private Stateroom[] staterooms;
    private State state;

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Getter
    @Setter
    @Builder
    public static class Stateroom {
        private String itineraryCode;
        private String shipCode;
        private String metaId;
        private int numberOfGuests;
        private Sailing sailing;
        private String pricedCategoryCode;
    }

    @Getter
    @Setter
    @Builder
    public static class Sailing {
        private long sailingId;
        private long departureDate;
        private long returnDate;
    }

    @Getter
    @Setter
    @Builder
    public static class State {
        @JsonProperty("staterooms")
        private StateroomState[] stateroomStates;
    }

    @Getter
    @Setter
    @Builder
    public static class StateroomState {
        private String itineraryCode;
        private String shipCode;
    }
}

