package com.wholesaler.inventory.repository;

import com.wholesaler.inventory.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmail(String email);
    List<Customer> findByCustomerNameContainingIgnoreCase(String name);
    
    @Query("SELECT c FROM Customer c WHERE c.isActive = true")
    List<Customer> findAllActive();
}
