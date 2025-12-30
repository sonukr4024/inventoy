package com.wholesaler.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillRequest {

    private Long customerId;

    @NotEmpty(message = "Bill must have at least one item")
    @Valid
    private List<BillItemRequest> items;

    private Double discountAmount;

    @NotNull(message = "Paid amount is required")
    @PositiveOrZero(message = "Paid amount cannot be negative")
    private Double paidAmount;

    private String paymentMode;

    private String remarks;
}
