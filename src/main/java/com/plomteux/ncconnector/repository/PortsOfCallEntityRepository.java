package com.plomteux.ncconnector.repository;

import com.plomteux.ncconnector.entity.PortsOfCallEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortsOfCallEntityRepository extends JpaRepository<PortsOfCallEntity, Long> {
}