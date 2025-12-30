package com.wholesaler.inventory.repository;

import com.wholesaler.inventory.entity.RateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RateHistoryRepository extends JpaRepository<RateHistory, Long> {
    List<RateHistory> findByProductIdOrderByEffectiveDateDesc(Long productId);
    
    @Query("SELECT r FROM RateHistory r WHERE r.product.id = :productId " +
           "AND r.effectiveDate <= :date " +
           "AND (r.endDate IS NULL OR r.endDate >= :date) " +
           "ORDER BY r.effectiveDate DESC")
    Optional<RateHistory> findEffectiveRateForDate(@Param("productId") Long productId, 
                                                     @Param("date") LocalDate date);
}
