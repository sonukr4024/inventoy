package com.wholesaler.inventory.dto.response;

import com.wholesaler.inventory.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillResponse {
    private Long id;
    private String billNumber;
    private Long customerId;
    private String customerName;
    private LocalDateTime billDate;
    private Double subTotal;
    private Double taxAmount;
    private Double discountAmount;
    private Double totalAmount;
    private Double paidAmount;
    private Double balanceAmount;
    private PaymentStatus paymentStatus;
    private String paymentMode;
    private String remarks;
    private List<BillItemResponse> items;
    private String createdBy;
    private LocalDateTime createdAt;
}
