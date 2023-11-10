package com.plomteux.ncconnector.entity;

import com.plomteux.ncconnector.model.CruiseDetailsEmbarkationPort;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Setter
@Getter
public class CruiseDetailsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String bundleType;
    private String code;
    private String shipCode;
    private BigDecimal duration;
    private BigDecimal guestCount;
    private String embarkationPortCode;


    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cruiseDetailsEntity")
    private List<SailingsEntity> sailingsEntities;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cruiseDetailsEntity")
    private List<DestinationCodeEntity> destinationsEntities;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cruiseDetailsEntity")
    private List<PortsOfCallEntity> portsOfCallEntities;
}
