package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.request.CustomerRequest;
import com.wholesaler.inventory.dto.response.CustomerResponse;
import com.wholesaler.inventory.entity.Customer;
import com.wholesaler.inventory.entity.CreditLedger;
import com.wholesaler.inventory.exception.ResourceNotFoundException;
import com.wholesaler.inventory.repository.CreditLedgerRepository;
import com.wholesaler.inventory.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CreditLedgerRepository creditLedgerRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("Creating customer: {}", request.getCustomerName());

        Customer customer = Customer.builder()
                .customerName(request.getCustomerName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .gstin(request.getGstin())
                .creditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : 0.0)
                .build();

        customer = customerRepository.save(customer);
        log.info("Customer created successfully with ID: {}", customer.getId());

        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        log.info("Updating customer: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setCustomerName(request.getCustomerName());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setGstin(request.getGstin());
        customer.setCreditLimit(request.getCreditLimit());

        customer = customerRepository.save(customer);
        log.info("Customer updated successfully: {}", id);

        return mapToResponse(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        log.info("Deleting customer: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setIsActive(false);
        customerRepository.save(customer);

        log.info("Customer deleted successfully: {}", id);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        Double outstandingBalance = creditLedgerRepository.findLatestByCustomerId(customer.getId())
                .map(CreditLedger::getBalance)
                .orElse(0.0);

        return CustomerResponse.builder()
                .id(customer.getId())
                .customerName(customer.getCustomerName())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .gstin(customer.getGstin())
                .creditLimit(customer.getCreditLimit())
                .faceImageCount(customer.getFaceImages().size())
                .outstandingBalance(outstandingBalance)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
