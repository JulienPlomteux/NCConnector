package com.plomteux.ncconnector.repository;

import com.plomteux.ncconnector.entity.SailingsEntity;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface SailingsRepository extends JpaRepository<SailingsEntity, Long>, SailingsRepositoryCustom{}