package com.wholesaler.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportResponse {
    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double totalSales;
    private Double totalTax;
    private Double totalDiscount;
    private Integer totalBills;
    private Double totalPaid;
    private Double totalOutstanding;
    private List<ProductSales> productWiseSales;
    private List<CategorySales> categoryWiseSales;
    private Map<String, Double> paymentModeSummary;
    private Map<String, Integer> paymentStatusSummary;
    private List<DailySales> dailyBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSales {
        private Long productId;
        private String productCode;
        private String productName;
        private String unit;
        private Double quantitySold;
        private Double totalRevenue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySales {
        private Long categoryId;
        private String categoryName;
        private Double totalRevenue;
        private Integer itemCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailySales {
        private LocalDate date;
        private Integer billCount;
        private Double totalSales;
        private Double totalPaid;
    }
}
