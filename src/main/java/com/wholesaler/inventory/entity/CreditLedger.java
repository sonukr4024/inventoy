package com.wholesaler.inventory.entity;

import com.wholesaler.inventory.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_ledger", indexes = {
    @Index(name = "idx_ledger_customer", columnList = "customer_id"),
    @Index(name = "idx_ledger_date", columnList = "transaction_date"),
    @Index(name = "idx_ledger_type", columnList = "transaction_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLedger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "debit_amount")
    private Double debitAmount = 0.0;

    @Column(name = "credit_amount")
    private Double creditAmount = 0.0;

    @Column(name = "balance", nullable = false)
    private Double balance;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;
}
