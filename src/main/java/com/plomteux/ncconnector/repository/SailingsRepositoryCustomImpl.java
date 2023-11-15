package com.plomteux.ncconnector.repository;

import com.plomteux.ncconnector.entity.RoomType;
import com.plomteux.ncconnector.entity.SailingsEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Repository
public class SailingsRepositoryCustomImpl implements SailingsRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<SailingsEntity> findSailingsByDepartureDateAndDestinationCode(LocalDate departureDate, String destinationCode) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SailingsEntity> cq = cb.createQuery(SailingsEntity.class);

        Root<SailingsEntity> sailings = cq.from(SailingsEntity.class);
        cq.where(cb.equal(sailings.get("departureDate"), departureDate), cb.equal(sailings.get("cruiseDetailsEntity").get("destinationsEntities").get("destinationCode"), destinationCode));

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public SailingsEntity findSailingsWithLowestPriceRoomType(BigDecimal sailId, String roomType) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SailingsEntity> cq = cb.createQuery(SailingsEntity.class);

        Root<SailingsEntity> sailings = cq.from(SailingsEntity.class);
        cq.where(cb.equal(sailings.get("sailId"), sailId));

        String roomTypeField;
        try {
            roomTypeField = RoomType.valueOf(roomType.toUpperCase()).getFieldName();
        } catch (IllegalArgumentException e) {
            roomTypeField = RoomType.INSIDE.getFieldName();
        }

        cq.orderBy(cb.asc(sailings.get(roomTypeField)));

        return entityManager.createQuery(cq).setMaxResults(1).getSingleResult();
    }

    @Override
    public List<SailingsEntity> getSailingsPriceDrops(LocalDate from, LocalDate to, BigDecimal percentage, String roomType) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<SailingsEntity> s = cq.from(SailingsEntity.class);
        Root<SailingsEntity> s2 = cq.from(SailingsEntity.class);

        String roomTypeField;
        try {
            roomTypeField = RoomType.valueOf(roomType.toUpperCase()).getFieldName();
        } catch (IllegalArgumentException e) {
            roomTypeField = RoomType.INSIDE.getFieldName();
        }

        Expression<BigDecimal> priceDifference = cb.diff(
                s.get(roomTypeField),
                s2.get(roomTypeField)
        );


        Predicate sameSailId = cb.equal(s.get("sailId"), s2.get("sailId"));
        Predicate sameBundleType = cb.equal(s.get("bundleType"), s2.get("bundleType"));
        Predicate samePackageId = cb.equal(s.get("packageId"), s2.get("packageId"));

        Expression<LocalDate> fromExpression = cb.literal(from);
        Expression<LocalDate> toExpression = cb.literal(to);
        Predicate publishedDateConditionS = cb.equal(s.get("publishedDate").as(LocalDate.class), fromExpression);
        Predicate publishedDateConditionS2 = cb.equal(s2.get("publishedDate").as(LocalDate.class), toExpression);

        Expression<BigDecimal> percentageExpression = cb.literal(percentage);
        Expression<BigDecimal> reducedPrice = cb.prod(s.get(roomTypeField), cb.diff(cb.literal(BigDecimal.ONE), percentageExpression));
        Predicate priceDropCondition = cb.le(s2.get(roomTypeField), reducedPrice);

        cq.multiselect(s2, priceDifference)
                .where(publishedDateConditionS, publishedDateConditionS2, sameSailId, sameBundleType, samePackageId, priceDropCondition)
                .orderBy(cb.desc(priceDifference));

        return entityManager.createQuery(cq).getResultList().stream()
                .map(result -> {
                    SailingsEntity sailings = result.get(0, SailingsEntity.class);
                    sailings.setOldPrice(result.get(1, BigDecimal.class));
                    return sailings;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SailingsEntity> getSailingsPricesBySailId(BigDecimal sailId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SailingsEntity> cq = cb.createQuery(SailingsEntity.class);

        Root<SailingsEntity> sailings = cq.from(SailingsEntity.class);
        cq.where(cb.equal(sailings.get("sailId"), sailId));

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<SailingsEntity> findCruise(LocalDate departureDate, LocalDate returnDate, String destinationCode, BigDecimal priceUpTo, BigDecimal priceFrom, BigDecimal daysAtSeaMin, BigDecimal daysAtSeaMax, String departurePort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SailingsEntity> cq = cb.createQuery(SailingsEntity.class);

        Root<SailingsEntity> sailings = cq.from(SailingsEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.greaterThanOrEqualTo(sailings.get("departureDate"), departureDate));
        predicates.add(cb.lessThanOrEqualTo(sailings.get("returnDate"), returnDate));
        predicates.add(cb.equal(sailings.get("publishedDate"), LocalDate.now()));

        if (destinationCode != null) {
            predicates.add(cb.equal(sailings.get("cruiseDetailsEntity").get("destinationsEntities").get("destinationCode"), destinationCode));
        }
        if (priceFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(sailings.get("inside"), priceFrom));
        }
        if (priceUpTo != null) {
            predicates.add(cb.lessThanOrEqualTo(sailings.get("inside"), priceUpTo));
        }
        if (departurePort != null) {
            predicates.add(cb.equal(sailings.get("cruiseDetailsEntity").get("embarkationPortCode"), departurePort));
        }
        if (daysAtSeaMin != null) {
            predicates.add(cb.greaterThanOrEqualTo(sailings.get("cruiseDetailsEntity").get("duration"), daysAtSeaMin));
        }
        if (daysAtSeaMax != null) {
            predicates.add(cb.lessThanOrEqualTo(sailings.get("cruiseDetailsEntity").get("duration"), daysAtSeaMax));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(sailings.get("inside")));

        return entityManager.createQuery(cq).getResultList();
    }
}