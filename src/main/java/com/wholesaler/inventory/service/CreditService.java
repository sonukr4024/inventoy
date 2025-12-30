package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.request.PaymentRequest;
import com.wholesaler.inventory.dto.response.CreditLedgerResponse;
import com.wholesaler.inventory.dto.response.CustomerOutstandingResponse;
import com.wholesaler.inventory.entity.*;
import com.wholesaler.inventory.enums.TransactionType;
import com.wholesaler.inventory.exception.ResourceNotFoundException;
import com.wholesaler.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {

    private final CreditLedgerRepository creditLedgerRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreditLedgerResponse recordPayment(PaymentRequest request) {
        log.info("Recording payment for customer: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        Double currentBalance = creditLedgerRepository.findLatestByCustomerId(customer.getId())
                .map(CreditLedger::getBalance)
                .orElse(0.0);

        CreditLedger ledger = CreditLedger.builder()
                .customer(customer)
                .transactionType(TransactionType.PAYMENT)
                .transactionDate(LocalDateTime.now())
                .debitAmount(0.0)
                .creditAmount(request.getAmount())
                .balance(currentBalance - request.getAmount())
                .referenceNumber(request.getReferenceNumber())
                .remarks(request.getRemarks())
                .recordedBy(getCurrentUser())
                .build();

        ledger = creditLedgerRepository.save(ledger);
        log.info("Payment recorded successfully");

        return mapToResponse(ledger);
    }

    @Transactional(readOnly = true)
    public List<CustomerOutstandingResponse> getAllOutstanding() {
        List<Long> customerIds = creditLedgerRepository.findCustomersWithOutstanding();
        
        return customerIds.stream()
                .map(this::getCustomerOutstanding)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerOutstandingResponse getCustomerOutstanding(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        Double totalOutstanding = creditLedgerRepository.findLatestByCustomerId(customerId)
                .map(CreditLedger::getBalance)
                .orElse(0.0);

        List<Bill> outstandingBills = billRepository.findOutstandingBillsByCustomer(customerId);

        List<CustomerOutstandingResponse.OutstandingBill> bills = outstandingBills.stream()
                .map(bill -> CustomerOutstandingResponse.OutstandingBill.builder()
                        .billId(bill.getId())
                        .billNumber(bill.getBillNumber())
                        .billDate(bill.getBillDate())
                        .totalAmount(bill.getTotalAmount())
                        .paidAmount(bill.getPaidAmount())
                        .balanceAmount(bill.getBalanceAmount())
                        .daysOverdue((int) ChronoUnit.DAYS.between(bill.getBillDate(), LocalDateTime.now()))
                        .build())
                .collect(Collectors.toList());

        return CustomerOutstandingResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getCustomerName())
                .phoneNumber(customer.getPhoneNumber())
                .email(customer.getEmail())
                .totalOutstanding(totalOutstanding)
                .creditLimit(customer.getCreditLimit())
                .oldestDueDate(bills.isEmpty() ? null : bills.get(bills.size() - 1).getBillDate())
                .outstandingBills(bills)
                .build();
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private CreditLedgerResponse mapToResponse(CreditLedger ledger) {
        return CreditLedgerResponse.builder()
                .id(ledger.getId())
                .customerId(ledger.getCustomer().getId())
                .customerName(ledger.getCustomer().getCustomerName())
                .billId(ledger.getBill() != null ? ledger.getBill().getId() : null)
                .billNumber(ledger.getBill() != null ? ledger.getBill().getBillNumber() : null)
                .transactionType(ledger.getTransactionType())
                .transactionDate(ledger.getTransactionDate())
                .debitAmount(ledger.getDebitAmount())
                .creditAmount(ledger.getCreditAmount())
                .balance(ledger.getBalance())
                .referenceNumber(ledger.getReferenceNumber())
                .remarks(ledger.getRemarks())
                .recordedBy(ledger.getRecordedBy() != null ? ledger.getRecordedBy().getUsername() : null)
                .build();
    }
}
