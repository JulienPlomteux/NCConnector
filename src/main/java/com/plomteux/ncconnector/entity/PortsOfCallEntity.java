package com.plomteux.ncconnector.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class PortsOfCallEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String portsOfCall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cruiseDetailsEntity_id")
    private CruiseDetailsEntity cruiseDetailsEntity;
}
