package com.wholesaler.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateOverrideRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Rate is required")
    @Positive(message = "Rate must be positive")
    private Double rate;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    private LocalDate endDate;

    private String remarks;
}
