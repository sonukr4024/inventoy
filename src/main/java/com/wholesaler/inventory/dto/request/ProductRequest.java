package com.wholesaler.inventory.dto.request;

import com.wholesaler.inventory.enums.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product code is required")
    private String productCode;

    @NotBlank(message = "Product name is required")
    private String productName;

    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Unit is required")
    private Unit unit;

    @NotNull(message = "Base rate is required")
    @Positive(message = "Base rate must be positive")
    private Double baseRate;

    private Double currentRate;

    private Double stockQuantity;

    private Double lowStockThreshold;

    private String hsnCode;

    private Double gstPercentage;
}
