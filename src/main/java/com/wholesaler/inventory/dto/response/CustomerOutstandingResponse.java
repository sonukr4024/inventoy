package com.wholesaler.inventory.dto.response;

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
public class CustomerOutstandingResponse {
    private Long customerId;
    private String customerName;
    private String phoneNumber;
    private String email;
    private Double totalOutstanding;
    private Double creditLimit;
    private LocalDateTime oldestDueDate;
    private List<OutstandingBill> outstandingBills;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OutstandingBill {
        private Long billId;
        private String billNumber;
        private LocalDateTime billDate;
        private Double totalAmount;
        private Double paidAmount;
        private Double balanceAmount;
        private Integer daysOverdue;
    }
}
