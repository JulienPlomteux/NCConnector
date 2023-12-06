package com.plomteux.ncconnector.repository;

import com.plomteux.ncconnector.entity.RoomType;
import com.plomteux.ncconnector.entity.SailingsEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SailingsRepositoryCustomImpl implements SailingsRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String CRUISE_DETAILS_ENTITY = "cruiseDetailsEntity";
    private static final String SAIL_ID = "sailId";
    private static final String PUBLISHED_DATE = "publishedDate";
    private static final String INSIDE = "inside";
    private static final String BUNDLE_TYPE = "bundleType";
    private static final String PACKAGE_ID = "packageId";
    private static final String DEPARTURE_DATE = "departureDate";
    private static final String RETURN_DATE = "returnDate";
    private static final String EMBARKATION_PORT_CODE = "embarkationPortCode";
    private static final String DURATION = "duration";

    @Override
    public List<SailingsEntity> findSailingsByDepartureDateAndDestinationCode(LocalDate departureDate, String destinationCode) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SailingsEntity> cq = cb.createQuery(SailingsEntity.class);

        Root<SailingsEntity> sailings = cq.from(SailingsEntity.class);
        cq.where(cb.equal(sailings.get(DEPARTURE_DATE), departureDate), cb.equal(sailings.get(CRUISE_DETAILS_ENTITY).get("destinationsEntities").get("destinationCode"), destinationCode));

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public SailingsEntity findSailingsWithLowestPriceRoomType(BigDecimal sailId, String roomType) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SailingsEntity> cq = cb.createQuery(SailingsEntity.class);

        Root<SailingsEntity> sailings = cq.from(SailingsEntity.class);
        cq.where(cb.equal(sailings.get(SAIL_ID), sailId));

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


        Predicate sameSailId = cb.equal(s.get(SAIL_ID), s2.get(SAIL_ID));
        Predicate sameBundleType = cb.equal(s.get(BUNDLE_TYPE), s2.get(BUNDLE_TYPE));
        Predicate samePackageId = cb.equal(s.get(PACKAGE_ID), s2.get(PACKAGE_ID));

        Expression<LocalDate> fromExpression = cb.literal(from);
        Expression<LocalDate> toExpression = cb.literal(to);
        Predicate publishedDateConditionS = cb.equal(s.get(PUBLISHED_DATE).as(LocalDate.class), fromExpression);
        Predicate publishedDateConditionS2 = cb.equal(s2.get(PUBLISHED_DATE).as(LocalDate.class), toExpression);

        Expression<BigDecimal> percentageExpression = cb.literal(percentage);
        Expression<BigDecimal> reducedPrice = cb.prod(s.get(roomTypeField), cb.diff(cb.literal(BigDecimal.ONE), percentageExpression));
        Predicate priceDropCondition;
        if (percentage.compareTo(BigDecimal.ZERO) == 0) {
            priceDropCondition = cb.lt(s2.get(roomTypeField), s.get(roomTypeField));
        } else {
            priceDropCondition = cb.le(s2.get(roomTypeField), reducedPrice);
        }

        cq.multiselect(s2, priceDifference)
                .where(publishedDateConditionS, publishedDateConditionS2, sameSailId, sameBundleType, samePackageId, priceDropCondition)
                .orderBy(cb.desc(priceDifference));

        return entityManager.createQuery(cq).getResultList().stream()
                .map(result -> {
                    SailingsEntity sailings = result.get(0, SailingsEntity.class);
                    sailings.setOldPrice(result.get(1, BigDecimal.class));
                    return sailings;
                })
                .toList();
    }

    @Override
    public List<SailingsEntity> getSailingsPricesBySailId(BigDecimal sailId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SailingsEntity> cq = cb.createQuery(SailingsEntity.class);

        Root<SailingsEntity> sailings = cq.from(SailingsEntity.class);
        cq.where(cb.equal(sailings.get(SAIL_ID), sailId));

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<SailingsEntity> findCruise(LocalDate departureDate, LocalDate returnDate, String destinationCode, BigDecimal priceUpTo, BigDecimal priceFrom, BigDecimal daysAtSeaMin, BigDecimal daysAtSeaMax, String departurePort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SailingsEntity> cq = cb.createQuery(SailingsEntity.class);

        Root<SailingsEntity> sailings = cq.from(SailingsEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.greaterThanOrEqualTo(sailings.get(DEPARTURE_DATE), departureDate));
        predicates.add(cb.lessThanOrEqualTo(sailings.get(RETURN_DATE), returnDate));
        predicates.add(cb.equal(sailings.get(PUBLISHED_DATE), LocalDate.now()));

        if (destinationCode != null) {
            predicates.add(cb.equal(sailings.get(CRUISE_DETAILS_ENTITY).get("destinationsEntities").get("destinationCode"), destinationCode));
        }
        if (priceFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(sailings.get(INSIDE), priceFrom));
        }
        if (priceUpTo != null) {
            predicates.add(cb.lessThanOrEqualTo(sailings.get(INSIDE), priceUpTo));
        }
        if (departurePort != null) {
            predicates.add(cb.equal(sailings.get(CRUISE_DETAILS_ENTITY).get(EMBARKATION_PORT_CODE), departurePort));
        }
        if (daysAtSeaMin != null) {
            predicates.add(cb.greaterThanOrEqualTo(sailings.get(CRUISE_DETAILS_ENTITY).get(DURATION), daysAtSeaMin));
        }
        if (daysAtSeaMax != null) {
            predicates.add(cb.lessThanOrEqualTo(sailings.get(CRUISE_DETAILS_ENTITY).get(DURATION), daysAtSeaMax));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(sailings.get(INSIDE)));

        return entityManager.createQuery(cq).getResultList();
    }
}