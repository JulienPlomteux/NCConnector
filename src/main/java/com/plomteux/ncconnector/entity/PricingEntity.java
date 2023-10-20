//package com.plomteux.ncconnector.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.text.SimpleDateFormat;
//import java.math.BigDecimal;
//import java.util.Date;
//
//@Entity
//@Setter
//@Getter
//public class PricingEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    @Temporal(TemporalType.TIMESTAMP)
//    private String date;
//    private String status;
//    private String code;
//    private BigDecimal combinedPrice;
//    private BigDecimal totalPrice;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "sailingsEntity_id")
//    private SailingsEntity sailingsEntity;
//
//    @PrePersist
//    private void prePersist() {
//        date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
//    }
//}
//
