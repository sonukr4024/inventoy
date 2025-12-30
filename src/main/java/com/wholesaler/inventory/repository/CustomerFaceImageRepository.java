package com.wholesaler.inventory.repository;

import com.wholesaler.inventory.entity.Customer;
import com.wholesaler.inventory.entity.CustomerFaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerFaceImageRepository extends JpaRepository<CustomerFaceImage, Long> {
    List<CustomerFaceImage> findByCustomer(Customer customer);
    List<CustomerFaceImage> findByCustomerId(Long customerId);
    List<CustomerFaceImage> findByEncodingStatus(String encodingStatus);

    long countByCustomerId(Long customerId);
}
