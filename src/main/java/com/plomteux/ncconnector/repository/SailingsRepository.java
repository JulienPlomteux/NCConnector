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
public interface SailingsRepository extends JpaRepository<SailingsEntity, Long> {
    @Query("SELECT se FROM SailingsEntity se " +
            "JOIN se.cruiseDetailsEntity cd " +
            "JOIN cd.destinationsEntities de " +
            "WHERE se.departureDate = :departureDate " +
            "AND de.destinationCode = :destinationCode")
    List<SailingsEntity> findSailingsByDepartureDateAndDestinationCode(
            @Param("departureDate") String departureDate,
            @Param("destinationCode") String destinationCode);

    @Query("SELECT se FROM SailingsEntity se " +
            "WHERE se.sailId = :sailId " +
            "ORDER BY " +
            "CASE WHEN :roomType = 'inside' THEN se.inside " +
            "     WHEN :roomType = 'oceanView' THEN se.oceanView " +
            "     WHEN :roomType = 'miniSuite' THEN se.oceanView " +
            "     WHEN :roomType = 'studio' THEN se.oceanView " +
            "     WHEN :roomType = 'suite' THEN se.oceanView " +
            "     WHEN :roomType = 'haven' THEN se.oceanView " +
            "     WHEN :roomType = 'spa' THEN se.oceanView " +
            "     ELSE se.inside END ASC " +
            "LIMIT 1")
    SailingsEntity findSailingsWithLowestPriceRoomType(
            @Param("sailId") BigDecimal sailId,
            @Param("roomType") String roomType);

    @Query("SELECT s2, " +
            "CASE " +
            "   WHEN :roomType = 'inside' THEN s.inside - s2.inside " +
            "   WHEN :roomType = 'studio' THEN s.studio - s2.studio " +
            "   WHEN :roomType = 'oceanView' THEN s.oceanView - s2.oceanView " +
            "   WHEN :roomType = 'miniSuite' THEN s.miniSuite - s2.miniSuite " +
            "   WHEN :roomType = 'suite' THEN s.suite - s2.suite " +
            "   WHEN :roomType = 'spa' THEN s.spa - s2.spa " +
            "   WHEN :roomType = 'haven' THEN s.haven - s2.haven " +
            "   ELSE s.inside - s2.inside " +
            "END AS priceDifference " +
            "FROM SailingsEntity s " +
            "INNER JOIN SailingsEntity s2 ON s.sailId = s2.sailId " +
            "WHERE s.publishedDate = :from " +
            "AND s.bundleType = s2.bundleType " +
            "AND s.packageId = s2.packageId " +
            "AND ((:roomType = 'inside' AND s.inside > s2.inside * (1 - COALESCE(:percentage, 0)))" +
            "   OR (:roomType = 'studio' AND s.studio > s2.studio * (1 - COALESCE(:percentage, 0)))" +
            "   OR (:roomType = 'oceanView' AND s.oceanView > s2.oceanView * (1 - COALESCE(:percentage, 0)))" +
            "   OR (:roomType = 'miniSuite' AND s.miniSuite > s2.miniSuite * (1 - COALESCE(:percentage, 0)))" +
            "   OR (:roomType = 'suite' AND s.suite > s2.suite * (1 - COALESCE(:percentage, 0)))" +
            "   OR (:roomType = 'spa' AND s.spa > s2.spa * (1 - COALESCE(:percentage, 0)))" +
            "   OR (:roomType = 'haven' AND s.haven > s2.haven * (1 - COALESCE(:percentage, 0)))) " +
            "AND s2.publishedDate = :to " +
            "ORDER BY " +
            "   CASE " +
            "       WHEN :roomType = 'inside' THEN s.inside - s2.inside " +
            "       WHEN :roomType = 'studio' THEN s.studio - s2.studio " +
            "       WHEN :roomType = 'oceanView' THEN s.oceanView - s2.oceanView " +
            "       WHEN :roomType = 'miniSuite' THEN s.miniSuite - s2.miniSuite " +
            "       WHEN :roomType = 'suite' THEN s.suite - s2.suite " +
            "       WHEN :roomType = 'spa' THEN s.spa - s2.spa " +
            "       WHEN :roomType = 'haven' THEN s.haven - s2.haven " +
            "       ELSE s.inside - s2.inside " +
            "   END DESC")
    List<Tuple> getSailingsPriceDrops(
            @Param("from") String from,
            @Param("to") String to,
            @Param("percentage") BigDecimal percentage,
            @Param("roomType") String roomType);

    @Query("SELECT se FROM SailingsEntity se " +
            "WHERE se.sailId = :sailId ")
    List<SailingsEntity> getSailingsPricesBySailId(@Param("sailId") BigDecimal sailId);


    @Query("SELECT se FROM SailingsEntity se " +
            "JOIN se.cruiseDetailsEntity cd " +
            "JOIN cd.destinationsEntities de " +
            "WHERE (se.departureDate BETWEEN :departureDate AND :returnDate) " +
            "AND (:destinationCode IS NULL OR de.destinationCode = :destinationCode) " +
            "AND (:priceFrom IS NULL OR se.inside >= :priceFrom) " +
            "AND (:priceUpTo IS NULL OR se.inside <= :priceUpTo) " +
            "AND (:departurePort IS NULL OR cd.embarkationPortCode = :departurePort) " +
            "AND (:daysAtSeaMin IS NULL OR cd.duration >= :daysAtSeaMin) " +
            "AND (:daysAtSeaMax IS NULL OR cd.duration <= :daysAtSeaMax) " +
            "AND se.publishedDate = CURRENT_DATE " +
            "ORDER BY se.inside ASC")
    List<SailingsEntity> findCruise(@Param("departureDate") LocalDate departureDate,
                                    @Param("returnDate") LocalDate returnDate,
                                    @Param("destinationCode") String destinationCode,
                                    @Param("priceUpTo") BigDecimal priceUpTo,
                                    @Param("priceFrom") BigDecimal priceFrom,
                                    @Param("daysAtSeaMin") BigDecimal daysAtSeaMin,
                                    @Param("daysAtSeaMax") BigDecimal daysAtSeaMax,
                                    @Param("departurePort") String departurePort);


}