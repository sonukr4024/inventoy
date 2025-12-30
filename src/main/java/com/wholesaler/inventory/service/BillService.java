package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.request.BillItemRequest;
import com.wholesaler.inventory.dto.request.BillRequest;
import com.wholesaler.inventory.dto.response.BillItemResponse;
import com.wholesaler.inventory.dto.response.BillResponse;
import com.wholesaler.inventory.entity.*;
import com.wholesaler.inventory.enums.PaymentStatus;
import com.wholesaler.inventory.enums.TransactionType;
import com.wholesaler.inventory.exception.BusinessException;
import com.wholesaler.inventory.exception.InsufficientStockException;
import com.wholesaler.inventory.exception.ResourceNotFoundException;
import com.wholesaler.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CreditLedgerRepository creditLedgerRepository;
    private final UserRepository userRepository;
    private final RateHistoryRepository rateHistoryRepository;

    private static final AtomicInteger billCounter = new AtomicInteger(0);

    @Transactional
    public BillResponse createBill(BillRequest request) {
        log.info("Creating new bill with {} items", request.getItems().size());

        // Validate items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Bill must have at least one item");
        }

        // Get customer if provided
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));
        }

        // Create bill entity
        Bill bill = Bill.builder()
                .billNumber(generateBillNumber())
                .customer(customer)
                .billDate(LocalDateTime.now())
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : 0.0)
                .paidAmount(request.getPaidAmount())
                .paymentMode(request.getPaymentMode())
                .remarks(request.getRemarks())
                .createdBy(getCurrentUser())
                .items(new ArrayList<>())
                .build();

        // Process bill items
        double subTotal = 0.0;
        double totalTax = 0.0;
        List<BillItem> billItems = new ArrayList<>();

        for (BillItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemRequest.getProductId()));

            // Check stock availability
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for product '%s'. Available: %.2f, Requested: %.2f",
                                product.getProductName(), product.getStockQuantity(), itemRequest.getQuantity())
                );
            }

            // Determine rate (use current rate if not provided)
            Double rate = itemRequest.getRate() != null ? itemRequest.getRate() : product.getCurrentRate();
            if (rate == null) {
                rate = product.getBaseRate();
            }

            // Calculate item amounts
            Double baseAmount = itemRequest.getQuantity() * rate;

            Double discountPercentage = itemRequest.getDiscountPercentage() != null ? itemRequest.getDiscountPercentage() : 0.0;
            Double discountAmount = baseAmount * (discountPercentage / 100.0);

            Double amountAfterDiscount = baseAmount - discountAmount;

            Double taxPercentage = itemRequest.getTaxPercentage() != null ? itemRequest.getTaxPercentage() : product.getGstPercentage();
            Double taxAmount = amountAfterDiscount * (taxPercentage / 100.0);

            Double lineTotal = amountAfterDiscount + taxAmount;

            // Create bill item
            BillItem billItem = BillItem.builder()
                    .bill(bill)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .rate(rate)
                    .taxPercentage(taxPercentage)
                    .taxAmount(taxAmount)
                    .discountPercentage(discountPercentage)
                    .discountAmount(discountAmount)
                    .lineTotal(lineTotal)
                    .build();

            billItems.add(billItem);
            subTotal += amountAfterDiscount;
            totalTax += taxAmount;

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productRepository.save(product);

            log.info("Stock deducted for product {}: {} units. New stock: {}",
                    product.getProductName(), itemRequest.getQuantity(), product.getStockQuantity());
        }

        // Set bill totals
        bill.setSubTotal(subTotal);
        bill.setTaxAmount(totalTax);

        // Apply overall bill discount
        Double totalAmount = subTotal + totalTax - bill.getDiscountAmount();
        bill.setTotalAmount(totalAmount);

        // Calculate balance
        Double balanceAmount = totalAmount - bill.getPaidAmount();
        bill.setBalanceAmount(balanceAmount);

        // Determine payment status
        if (balanceAmount <= 0.0) {
            bill.setPaymentStatus(PaymentStatus.PAID);
        } else if (bill.getPaidAmount() > 0) {
            bill.setPaymentStatus(PaymentStatus.PARTIAL);
        } else {
            bill.setPaymentStatus(PaymentStatus.CREDIT);
        }

        // Add items to bill
        bill.setItems(billItems);

        // Save bill (cascades to items)
        bill = billRepository.save(bill);

        log.info("Bill created successfully: {} with total amount: {}", bill.getBillNumber(), bill.getTotalAmount());

        // Create credit ledger entry if there's an outstanding balance and customer is specified
        if (balanceAmount > 0 && customer != null) {
            createCreditLedgerEntry(bill, customer);
        }

        return mapToResponse(bill);
    }

    @Transactional(readOnly = true)
    public BillResponse getBill(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", id));
        return mapToResponse(bill);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getAllBills() {
        return billRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        return billRepository.findByCustomerOrderByBillDateDesc(customer).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getBillsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return billRepository.findByBillDateBetweenOrderByBillDateDesc(startDate, endDate).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getOutstandingBills() {
        return billRepository.findByPaymentStatusIn(
                List.of(PaymentStatus.CREDIT, PaymentStatus.PARTIAL)
        ).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BillResponse updatePayment(Long billId, Double paymentAmount, String paymentMode, String remarks) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill", "id", billId));

        if (bill.getBalanceAmount() <= 0) {
            throw new BusinessException("Bill is already fully paid");
        }

        if (paymentAmount > bill.getBalanceAmount()) {
            throw new BusinessException("Payment amount cannot exceed balance amount");
        }

        // Update bill payment
        bill.setPaidAmount(bill.getPaidAmount() + paymentAmount);
        bill.setBalanceAmount(bill.getBalanceAmount() - paymentAmount);

        if (paymentMode != null) {
            bill.setPaymentMode(paymentMode);
        }

        if (remarks != null) {
            bill.setRemarks(bill.getRemarks() != null ? bill.getRemarks() + "; " + remarks : remarks);
        }

        // Update payment status
        if (bill.getBalanceAmount() <= 0.0) {
            bill.setPaymentStatus(PaymentStatus.PAID);
        } else if (bill.getPaidAmount() > 0) {
            bill.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        bill = billRepository.save(bill);

        // Update credit ledger
        if (bill.getCustomer() != null) {
            updateCreditLedgerOnPayment(bill, paymentAmount);
        }

        log.info("Payment of {} recorded for bill {}", paymentAmount, bill.getBillNumber());

        return mapToResponse(bill);
    }

    private void createCreditLedgerEntry(Bill bill, Customer customer) {
        Double currentBalance = creditLedgerRepository.findLatestByCustomerId(customer.getId())
                .map(CreditLedger::getBalance)
                .orElse(0.0);

        CreditLedger ledger = CreditLedger.builder()
                .customer(customer)
                .bill(bill)
                .transactionType(TransactionType.SALE)
                .transactionDate(bill.getBillDate())
                .debitAmount(bill.getBalanceAmount())
                .creditAmount(0.0)
                .balance(currentBalance + bill.getBalanceAmount())
                .referenceNumber(bill.getBillNumber())
                .remarks("Credit sale - " + bill.getBillNumber())
                .recordedBy(bill.getCreatedBy())
                .build();

        creditLedgerRepository.save(ledger);
        log.info("Credit ledger entry created for customer {}, amount: {}", customer.getCustomerName(), bill.getBalanceAmount());
    }

    private void updateCreditLedgerOnPayment(Bill bill, Double paymentAmount) {
        Customer customer = bill.getCustomer();

        Double currentBalance = creditLedgerRepository.findLatestByCustomerId(customer.getId())
                .map(CreditLedger::getBalance)
                .orElse(0.0);

        CreditLedger ledger = CreditLedger.builder()
                .customer(customer)
                .bill(bill)
                .transactionType(TransactionType.PAYMENT)
                .transactionDate(LocalDateTime.now())
                .debitAmount(0.0)
                .creditAmount(paymentAmount)
                .balance(currentBalance - paymentAmount)
                .referenceNumber(bill.getBillNumber())
                .remarks("Payment for bill - " + bill.getBillNumber())
                .recordedBy(getCurrentUser())
                .build();

        creditLedgerRepository.save(ledger);
        log.info("Credit ledger updated for payment against bill {}", bill.getBillNumber());
    }

    private String generateBillNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int counter = billCounter.incrementAndGet();
        return String.format("BILL-%s-%04d", datePrefix, counter);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private BillResponse mapToResponse(Bill bill) {
        List<BillItemResponse> itemResponses = bill.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return BillResponse.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .customerId(bill.getCustomer() != null ? bill.getCustomer().getId() : null)
                .customerName(bill.getCustomer() != null ? bill.getCustomer().getCustomerName() : "Walk-in Customer")
                .billDate(bill.getBillDate())
                .subTotal(bill.getSubTotal())
                .taxAmount(bill.getTaxAmount())
                .discountAmount(bill.getDiscountAmount())
                .totalAmount(bill.getTotalAmount())
                .paidAmount(bill.getPaidAmount())
                .balanceAmount(bill.getBalanceAmount())
                .paymentStatus(bill.getPaymentStatus())
                .paymentMode(bill.getPaymentMode())
                .remarks(bill.getRemarks())
                .items(itemResponses)
                .createdBy(bill.getCreatedBy() != null ? bill.getCreatedBy().getUsername() : null)
                .createdAt(bill.getCreatedAt())
                .build();
    }

    private BillItemResponse mapItemToResponse(BillItem item) {
        return BillItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getProductName())
                .productCode(item.getProduct().getProductCode())
                .quantity(item.getQuantity())
                .unit(item.getProduct().getUnit().toString())
                .rate(item.getRate())
                .taxPercentage(item.getTaxPercentage())
                .taxAmount(item.getTaxAmount())
                .discountPercentage(item.getDiscountPercentage())
                .discountAmount(item.getDiscountAmount())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
