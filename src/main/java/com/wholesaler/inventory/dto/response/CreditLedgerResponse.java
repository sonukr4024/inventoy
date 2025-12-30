package com.wholesaler.inventory.dto.response;

import com.wholesaler.inventory.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLedgerResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long billId;
    private String billNumber;
    private TransactionType transactionType;
    private LocalDateTime transactionDate;
    private Double debitAmount;
    private Double creditAmount;
    private Double balance;
    private String referenceNumber;
    private String remarks;
    private String recordedBy;
}
