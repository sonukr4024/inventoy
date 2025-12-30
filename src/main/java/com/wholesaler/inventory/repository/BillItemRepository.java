package com.wholesaler.inventory.repository;

import com.wholesaler.inventory.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillItemRepository extends JpaRepository<BillItem, Long> {
    List<BillItem> findByBillId(Long billId);
    List<BillItem> findByProductId(Long productId);

    @Query("SELECT bi FROM BillItem bi WHERE bi.product.id = :productId AND bi.bill.billDate BETWEEN :startDate AND :endDate")
    List<BillItem> findByProductIdAndBillDateBetween(@Param("productId") Long productId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT bi FROM BillItem bi WHERE bi.bill.billDate BETWEEN :startDate AND :endDate")
    List<BillItem> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT bi.product.id, bi.product.productName, SUM(bi.quantity), SUM(bi.lineTotal) " +
           "FROM BillItem bi WHERE bi.bill.billDate BETWEEN :startDate AND :endDate " +
           "GROUP BY bi.product.id, bi.product.productName " +
           "ORDER BY SUM(bi.lineTotal) DESC")
    List<Object[]> getProductSalesSummary(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
}
