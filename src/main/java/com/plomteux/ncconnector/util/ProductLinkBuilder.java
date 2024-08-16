package com.plomteux.ncconnector.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
@Component
public class ProductLinkBuilder {
    @Value("${ncl.base.productlink.url}")
    private String NCL_BASE_PRODUCTLINK_URL;

    private static String baseUrl;

    @PostConstruct
    private void init() {
        baseUrl = NCL_BASE_PRODUCTLINK_URL;
    }
    public static String buildProductViewLink(String itineraryCode, BigDecimal voyageId, LocalDate sailDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = sailDate.format(formatter);

        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("itineraryCode", itineraryCode)
                .queryParam("selectedStateroomMeta", "INSIDE")
                .queryParam("voyageId", voyageId.intValue())
                .queryParam("sailDate", formattedDate)
                .queryParam("guestCount", 2)
                .toUriString();
    }
}
