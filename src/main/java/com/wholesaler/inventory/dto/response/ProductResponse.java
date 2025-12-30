package com.wholesaler.inventory.dto.response;

import com.wholesaler.inventory.enums.Unit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String productCode;
    private String productName;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Unit unit;
    private Double baseRate;
    private Double currentRate;
    private Double stockQuantity;
    private Double lowStockThreshold;
    private String hsnCode;
    private Double gstPercentage;
    private boolean lowStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
