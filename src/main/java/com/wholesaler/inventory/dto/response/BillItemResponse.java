package com.wholesaler.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productCode;
    private Double quantity;
    private String unit;
    private Double rate;
    private Double taxPercentage;
    private Double taxAmount;
    private Double discountPercentage;
    private Double discountAmount;
    private Double lineTotal;
}
