package com.plomteux.ncconnector.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Entity
@Setter
@Getter
public class SailingsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal sailId;
    private String bundleType;
    private BigDecimal packageId;
    private String departureDate;
    private String returnDate;
    private String publishedDate;
    private BigDecimal studio;
    private BigDecimal inside;
    private BigDecimal oceanView;
    private BigDecimal balcony;
    private BigDecimal miniSuite;
    private BigDecimal suite;
    private BigDecimal haven;
    private BigDecimal spa;
    private BigDecimal oldPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cruiseDetailsEntity_id")
    private CruiseDetailsEntity cruiseDetailsEntity;

    @PrePersist
    public void prePersist() {
        this.publishedDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    }
}
