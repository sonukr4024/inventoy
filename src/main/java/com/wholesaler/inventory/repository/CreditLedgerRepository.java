package com.wholesaler.inventory.repository;

import com.wholesaler.inventory.entity.CreditLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditLedgerRepository extends JpaRepository<CreditLedger, Long> {
    List<CreditLedger> findByCustomerIdOrderByTransactionDateDesc(Long customerId);
    
    @Query("SELECT c FROM CreditLedger c WHERE c.customer.id = :customerId ORDER BY c.transactionDate DESC LIMIT 1")
    Optional<CreditLedger> findLatestByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT c FROM CreditLedger c WHERE c.balance > 0")
    List<CreditLedger> findAllWithOutstandingBalance();
    
    @Query("SELECT DISTINCT c.customer.id FROM CreditLedger c WHERE c.balance > 0")
    List<Long> findCustomersWithOutstanding();
}
