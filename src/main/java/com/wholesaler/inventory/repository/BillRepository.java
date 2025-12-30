package com.wholesaler.inventory.repository;

import com.wholesaler.inventory.entity.Bill;
import com.wholesaler.inventory.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByBillNumber(String billNumber);
    List<Bill> findByCustomerId(Long customerId);
    List<Bill> findByPaymentStatus(PaymentStatus status);
    List<Bill> findByPaymentStatusIn(List<PaymentStatus> statuses);
    List<Bill> findTop100ByOrderByBillDateDesc();
    List<Bill> findByCustomerOrderByBillDateDesc(com.wholesaler.inventory.entity.Customer customer);
    List<Bill> findByBillDateBetweenOrderByBillDateDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT b FROM Bill b WHERE b.billDate BETWEEN :startDate AND :endDate ORDER BY b.billDate DESC")
    List<Bill> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Bill b WHERE b.customer.id = :customerId AND b.balanceAmount > 0")
    List<Bill> findOutstandingBillsByCustomer(@Param("customerId") Long customerId);
    
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0.0) FROM Bill b WHERE b.billDate BETWEEN :startDate AND :endDate")
    Double getTotalSalesByDateRange(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);
}
