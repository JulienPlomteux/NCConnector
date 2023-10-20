package com.plomteux.ncconnector.repository;

import com.plomteux.ncconnector.entity.CruiseDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface CruiseDetailsRepository extends JpaRepository<CruiseDetailsEntity, Long> {
    @Query("SELECT DISTINCT d.destinationCode FROM CruiseDetailsEntity c JOIN c.destinationsEntities d")
    List<String> findUniqueDestinationCodes();

    @Query("SELECT c FROM CruiseDetailsEntity c JOIN c.destinationsEntities d WHERE d.destinationCode = :destinationCode")
    List<CruiseDetailsEntity> findByDestinationCode(@Param("destinationCode") String destinationCode);

    @Query("SELECT cd FROM CruiseDetailsEntity cd WHERE cd.code = :code")
    List<CruiseDetailsEntity> findByCode(@Param("code") String code);

}
