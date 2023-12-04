package com.plomteux.ncconnector.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

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
    private LocalDate departureDate;
    private LocalDate returnDate;
    private LocalDate publishedDate;
    private BigDecimal inside;
    private BigDecimal oceanView;
    private BigDecimal balcony;
    private BigDecimal oldPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cruiseDetailsEntity_id")
    private CruiseDetailsEntity cruiseDetailsEntity;

    @PrePersist
    public void prePersist() {
        this.publishedDate = LocalDate.now();
    }
}
