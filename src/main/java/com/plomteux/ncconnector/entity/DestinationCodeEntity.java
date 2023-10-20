package com.plomteux.ncconnector.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class DestinationCodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String destinationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cruiseDetailsEntity_id")
    private CruiseDetailsEntity cruiseDetailsEntity;

}
