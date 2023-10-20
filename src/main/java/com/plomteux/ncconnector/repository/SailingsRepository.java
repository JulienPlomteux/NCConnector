package com.plomteux.ncconnector.repository;

import com.plomteux.ncconnector.entity.SailingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SailingsRepository extends JpaRepository<SailingsEntity, Long> {}