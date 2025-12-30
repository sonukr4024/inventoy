package com.wholesaler.inventory.entity;

import com.wholesaler.inventory.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills", indexes = {
    @Index(name = "idx_bill_number", columnList = "bill_number", unique = true),
    @Index(name = "idx_bill_customer", columnList = "customer_id"),
    @Index(name = "idx_bill_date", columnList = "bill_date"),
    @Index(name = "idx_bill_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill extends BaseEntity {

    @Column(name = "bill_number", nullable = false, unique = true, length = 50)
    private String billNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "bill_date", nullable = false)
    private LocalDateTime billDate;

    @Column(name = "sub_total", nullable = false)
    private Double subTotal = 0.0;

    @Column(name = "tax_amount")
    private Double taxAmount = 0.0;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount = 0.0;

    @Column(name = "paid_amount", nullable = false)
    private Double paidAmount = 0.0;

    @Column(name = "balance_amount", nullable = false)
    private Double balanceAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BillItem> items = new ArrayList<>();
}
