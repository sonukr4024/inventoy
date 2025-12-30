package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.response.CustomerOutstandingResponse;
import com.wholesaler.inventory.dto.response.SalesReportResponse;
import com.wholesaler.inventory.entity.Bill;
import com.wholesaler.inventory.entity.BillItem;
import com.wholesaler.inventory.entity.Product;
import com.wholesaler.inventory.enums.PaymentStatus;
import com.wholesaler.inventory.repository.BillItemRepository;
import com.wholesaler.inventory.repository.BillRepository;
import com.wholesaler.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final ProductRepository productRepository;
    private final CreditService creditService;

    @Transactional(readOnly = true)
    public SalesReportResponse getDailySalesReport(LocalDate date) {
        log.info("Generating daily sales report for: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return generateSalesReport(startOfDay, endOfDay, "Daily Sales Report - " + date);
    }

    @Transactional(readOnly = true)
    public SalesReportResponse getMonthlySalesReport(int year, int month) {
        log.info("Generating monthly sales report for: {}-{}", year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        LocalDateTime startOfMonth = startDate.atStartOfDay();
        LocalDateTime endOfMonth = endDate.atTime(LocalTime.MAX);

        return generateSalesReport(startOfMonth, endOfMonth,
                String.format("Monthly Sales Report - %s %d", startDate.getMonth(), year));
    }

    @Transactional(readOnly = true)
    public SalesReportResponse getCustomSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating custom sales report from {} to {}", startDate, endDate);

        return generateSalesReport(startDate, endDate,
                String.format("Sales Report (%s to %s)", startDate.toLocalDate(), endDate.toLocalDate()));
    }

    private SalesReportResponse generateSalesReport(LocalDateTime startDate, LocalDateTime endDate, String title) {
        List<Bill> bills = billRepository.findByBillDateBetweenOrderByBillDateDesc(startDate, endDate);

        if (bills.isEmpty()) {
            return SalesReportResponse.builder()
                    .title(title)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalBills(0)
                    .totalSales(0.0)
                    .totalTax(0.0)
                    .totalDiscount(0.0)
                    .totalPaid(0.0)
                    .totalOutstanding(0.0)
                    .productWiseSales(Collections.emptyList())
                    .categoryWiseSales(Collections.emptyList())
                    .paymentModeSummary(Collections.emptyMap())
                    .dailyBreakdown(Collections.emptyList())
                    .build();
        }

        // Calculate totals
        int totalBills = bills.size();
        double totalSales = bills.stream().mapToDouble(Bill::getTotalAmount).sum();
        double totalTax = bills.stream().mapToDouble(Bill::getTaxAmount).sum();
        double totalDiscount = bills.stream().mapToDouble(Bill::getDiscountAmount).sum();
        double totalPaid = bills.stream().mapToDouble(Bill::getPaidAmount).sum();
        double totalOutstanding = bills.stream().mapToDouble(Bill::getBalanceAmount).sum();

        // Product-wise sales
        List<SalesReportResponse.ProductSales> productWiseSales = generateProductWiseSales(bills);

        // Category-wise sales
        List<SalesReportResponse.CategorySales> categoryWiseSales = generateCategoryWiseSales(bills);

        // Payment mode summary
        Map<String, Double> paymentModeSummary = bills.stream()
                .filter(bill -> bill.getPaymentMode() != null && !bill.getPaymentMode().isEmpty())
                .collect(Collectors.groupingBy(
                        Bill::getPaymentMode,
                        Collectors.summingDouble(Bill::getPaidAmount)
                ));

        // Payment status summary
        Map<String, Integer> paymentStatusSummary = bills.stream()
                .collect(Collectors.groupingBy(
                        bill -> bill.getPaymentStatus().toString(),
                        Collectors.summingInt(bill -> 1)
                ));

        // Daily breakdown
        List<SalesReportResponse.DailySales> dailyBreakdown = generateDailyBreakdown(bills);

        return SalesReportResponse.builder()
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .totalBills(totalBills)
                .totalSales(totalSales)
                .totalTax(totalTax)
                .totalDiscount(totalDiscount)
                .totalPaid(totalPaid)
                .totalOutstanding(totalOutstanding)
                .productWiseSales(productWiseSales)
                .categoryWiseSales(categoryWiseSales)
                .paymentModeSummary(paymentModeSummary)
                .paymentStatusSummary(paymentStatusSummary)
                .dailyBreakdown(dailyBreakdown)
                .build();
    }

    private List<SalesReportResponse.ProductSales> generateProductWiseSales(List<Bill> bills) {
        Map<Long, SalesReportResponse.ProductSales> productSalesMap = new HashMap<>();

        for (Bill bill : bills) {
            for (BillItem item : bill.getItems()) {
                Product product = item.getProduct();
                Long productId = product.getId();

                SalesReportResponse.ProductSales productSales = productSalesMap.getOrDefault(
                        productId,
                        SalesReportResponse.ProductSales.builder()
                                .productId(productId)
                                .productCode(product.getProductCode())
                                .productName(product.getProductName())
                                .unit(product.getUnit().toString())
                                .quantitySold(0.0)
                                .totalRevenue(0.0)
                                .build()
                );

                productSales.setQuantitySold(productSales.getQuantitySold() + item.getQuantity());
                productSales.setTotalRevenue(productSales.getTotalRevenue() + item.getLineTotal());

                productSalesMap.put(productId, productSales);
            }
        }

        return productSalesMap.values().stream()
                .sorted(Comparator.comparing(SalesReportResponse.ProductSales::getTotalRevenue).reversed())
                .collect(Collectors.toList());
    }

    private List<SalesReportResponse.CategorySales> generateCategoryWiseSales(List<Bill> bills) {
        Map<Long, SalesReportResponse.CategorySales> categorySalesMap = new HashMap<>();

        for (Bill bill : bills) {
            for (BillItem item : bill.getItems()) {
                Product product = item.getProduct();
                Long categoryId = product.getCategory().getId();

                SalesReportResponse.CategorySales categorySales = categorySalesMap.getOrDefault(
                        categoryId,
                        SalesReportResponse.CategorySales.builder()
                                .categoryId(categoryId)
                                .categoryName(product.getCategory().getCategoryName())
                                .totalRevenue(0.0)
                                .itemCount(0)
                                .build()
                );

                categorySales.setTotalRevenue(categorySales.getTotalRevenue() + item.getLineTotal());
                categorySales.setItemCount(categorySales.getItemCount() + 1);

                categorySalesMap.put(categoryId, categorySales);
            }
        }

        return categorySalesMap.values().stream()
                .sorted(Comparator.comparing(SalesReportResponse.CategorySales::getTotalRevenue).reversed())
                .collect(Collectors.toList());
    }

    private List<SalesReportResponse.DailySales> generateDailyBreakdown(List<Bill> bills) {
        Map<LocalDate, SalesReportResponse.DailySales> dailyMap = new HashMap<>();

        for (Bill bill : bills) {
            LocalDate billDate = bill.getBillDate().toLocalDate();

            SalesReportResponse.DailySales dailySales = dailyMap.getOrDefault(
                    billDate,
                    SalesReportResponse.DailySales.builder()
                            .date(billDate)
                            .billCount(0)
                            .totalSales(0.0)
                            .totalPaid(0.0)
                            .build()
            );

            dailySales.setBillCount(dailySales.getBillCount() + 1);
            dailySales.setTotalSales(dailySales.getTotalSales() + bill.getTotalAmount());
            dailySales.setTotalPaid(dailySales.getTotalPaid() + bill.getPaidAmount());

            dailyMap.put(billDate, dailySales);
        }

        return dailyMap.values().stream()
                .sorted(Comparator.comparing(SalesReportResponse.DailySales::getDate))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalesReportResponse.ProductSales getProductSalesReport(Long productId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating product sales report for product: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<BillItem> billItems = billItemRepository.findByProductIdAndBillDateBetween(
                productId, startDate, endDate);

        double totalQuantity = billItems.stream().mapToDouble(BillItem::getQuantity).sum();
        double totalRevenue = billItems.stream().mapToDouble(BillItem::getLineTotal).sum();

        return SalesReportResponse.ProductSales.builder()
                .productId(productId)
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .unit(product.getUnit().toString())
                .quantitySold(totalQuantity)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CustomerOutstandingResponse> getOutstandingReport() {
        log.info("Generating outstanding report for all customers");
        return creditService.getAllOutstanding();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLowStockReport() {
        log.info("Generating low stock report");

        List<Product> lowStockProducts = productRepository.findLowStockProducts();

        List<Map<String, Object>> productDetails = lowStockProducts.stream()
                .map(product -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("productId", product.getId());
                    details.put("productCode", product.getProductCode());
                    details.put("productName", product.getProductName());
                    details.put("categoryName", product.getCategory().getCategoryName());
                    details.put("currentStock", product.getStockQuantity());
                    details.put("lowStockThreshold", product.getLowStockThreshold());
                    details.put("unit", product.getUnit().toString());
                    details.put("deficit", product.getLowStockThreshold() - product.getStockQuantity());
                    return details;
                })
                .sorted((a, b) -> Double.compare((Double) a.get("deficit"), (Double) b.get("deficit")))
                .collect(Collectors.toList());

        Map<String, Object> report = new HashMap<>();
        report.put("title", "Low Stock Report");
        report.put("generatedAt", LocalDateTime.now());
        report.put("totalProducts", productDetails.size());
        report.put("products", productDetails);

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInventoryValuationReport() {
        log.info("Generating inventory valuation report");

        List<Product> allProducts = productRepository.findAll();

        double totalInventoryValue = 0.0;
        List<Map<String, Object>> productValuations = new ArrayList<>();

        for (Product product : allProducts) {
            double value = product.getStockQuantity() * product.getCurrentRate();
            totalInventoryValue += value;

            Map<String, Object> valuation = new HashMap<>();
            valuation.put("productId", product.getId());
            valuation.put("productCode", product.getProductCode());
            valuation.put("productName", product.getProductName());
            valuation.put("categoryName", product.getCategory().getCategoryName());
            valuation.put("stockQuantity", product.getStockQuantity());
            valuation.put("unit", product.getUnit().toString());
            valuation.put("currentRate", product.getCurrentRate());
            valuation.put("totalValue", value);

            productValuations.add(valuation);
        }

        // Sort by total value descending
        productValuations.sort((a, b) -> Double.compare((Double) b.get("totalValue"), (Double) a.get("totalValue")));

        Map<String, Object> report = new HashMap<>();
        report.put("title", "Inventory Valuation Report");
        report.put("generatedAt", LocalDateTime.now());
        report.put("totalProducts", allProducts.size());
        report.put("totalInventoryValue", totalInventoryValue);
        report.put("productValuations", productValuations);

        return report;
    }
}
