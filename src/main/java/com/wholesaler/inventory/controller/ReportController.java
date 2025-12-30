package com.wholesaler.inventory.controller;

import com.wholesaler.inventory.dto.response.ApiResponse;
import com.wholesaler.inventory.dto.response.CustomerOutstandingResponse;
import com.wholesaler.inventory.dto.response.SalesReportResponse;
import com.wholesaler.inventory.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report generation APIs")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/sales/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get daily sales report")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getDailySalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        SalesReportResponse report = reportService.getDailySalesReport(date);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/sales/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get monthly sales report")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getMonthlySalesReport(
            @RequestParam int year,
            @RequestParam int month) {
        SalesReportResponse report = reportService.getMonthlySalesReport(year, month);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/sales/custom")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get custom date range sales report")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getCustomSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        SalesReportResponse report = reportService.getCustomSalesReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/sales/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get product-wise sales report")
    public ResponseEntity<ApiResponse<SalesReportResponse.ProductSales>> getProductSalesReport(
            @PathVariable Long productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        SalesReportResponse.ProductSales report = reportService.getProductSalesReport(productId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/outstanding")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get outstanding report for all customers")
    public ResponseEntity<ApiResponse<List<CustomerOutstandingResponse>>> getOutstandingReport() {
        List<CustomerOutstandingResponse> report = reportService.getOutstandingReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/inventory/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get low stock report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLowStockReport() {
        Map<String, Object> report = reportService.getLowStockReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/inventory/valuation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get inventory valuation report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventoryValuationReport() {
        Map<String, Object> report = reportService.getInventoryValuationReport();
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
