package com.plomteux.ncconnector.repository;

import com.plomteux.ncconnector.entity.DestinationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinationRepository extends JpaRepository<DestinationCodeEntity, Long> {}