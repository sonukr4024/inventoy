package com.wholesaler.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wholesaler.inventory.dto.request.AIReportRequest;
import com.wholesaler.inventory.dto.response.AIReportResponse;
import com.wholesaler.inventory.entity.Bill;
import com.wholesaler.inventory.entity.Customer;
import com.wholesaler.inventory.entity.Product;
import com.wholesaler.inventory.repository.BillRepository;
import com.wholesaler.inventory.repository.CustomerRepository;
import com.wholesaler.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIReportService {

    private final BillRepository billRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ReportService reportService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai-report.api-url}")
    private String aiApiUrl;

    @Value("${app.ai-report.api-key}")
    private String aiApiKey;

    @Value("${app.ai-report.model}")
    private String aiModel;

    @Value("${app.ai-report.timeout-seconds}")
    private int timeoutSeconds;

    @Async
    public CompletableFuture<AIReportResponse> generateAIReportAsync(AIReportRequest request) {
        return CompletableFuture.completedFuture(generateAIReport(request));
    }

    @Transactional(readOnly = true)
    public AIReportResponse generateAIReport(AIReportRequest request) {
        log.info("Generating AI report for prompt: {}", request.getPrompt());

        try {
            // Gather relevant data based on the prompt
            String contextData = gatherContextData(request.getPrompt());

            // Build AI API request
            String aiResponse = callAIAPI(request.getPrompt(), contextData);

            return AIReportResponse.builder()
                    .success(true)
                    .prompt(request.getPrompt())
                    .reportContent(aiResponse)
                    .generatedAt(LocalDateTime.now())
                    .model(aiModel)
                    .message("AI report generated successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate AI report", e);
            return AIReportResponse.builder()
                    .success(false)
                    .prompt(request.getPrompt())
                    .reportContent(null)
                    .generatedAt(LocalDateTime.now())
                    .model(aiModel)
                    .message("Failed to generate report: " + e.getMessage())
                    .build();
        }
    }

    private String gatherContextData(String prompt) {
        StringBuilder context = new StringBuilder();
        String lowerPrompt = prompt.toLowerCase();

        // Add business context
        context.append("=== BUSINESS CONTEXT ===\n");
        context.append("This is a Fruit/Fish Wholesaler and Grocery Store Inventory Management System.\n\n");

        // Gather sales data if mentioned
        if (containsAny(lowerPrompt, "sales", "revenue", "bill", "transaction")) {
            context.append("=== RECENT SALES DATA ===\n");
            List<Bill> recentBills = billRepository.findTop100ByOrderByBillDateDesc();
            context.append(formatBillsData(recentBills));
            context.append("\n");
        }

        // Gather product/inventory data if mentioned
        if (containsAny(lowerPrompt, "product", "inventory", "stock", "item")) {
            context.append("=== INVENTORY DATA ===\n");
            List<Product> products = productRepository.findAll();
            context.append(formatProductsData(products));
            context.append("\n");
        }

        // Gather customer data if mentioned
        if (containsAny(lowerPrompt, "customer", "client", "buyer", "outstanding", "credit", "due")) {
            context.append("=== CUSTOMER DATA ===\n");
            List<Customer> customers = customerRepository.findAll();
            context.append(formatCustomersData(customers));
            context.append("\n");
        }

        // Add date range context if mentioned
        if (containsAny(lowerPrompt, "today", "yesterday", "week", "month", "year")) {
            context.append("=== DATE CONTEXT ===\n");
            context.append("Current Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
        }

        return context.toString();
    }

    private String formatBillsData(List<Bill> bills) {
        StringBuilder sb = new StringBuilder();
        sb.append("Total Bills: ").append(bills.size()).append("\n");

        double totalSales = bills.stream().mapToDouble(bill -> bill.getTotalAmount()).sum();
        double totalPaid = bills.stream().mapToDouble(bill -> bill.getPaidAmount()).sum();
        double totalOutstanding = bills.stream().mapToDouble(bill -> bill.getBalanceAmount()).sum();

        sb.append("Total Sales Amount: ").append(String.format("%.2f", totalSales)).append("\n");
        sb.append("Total Paid Amount: ").append(String.format("%.2f", totalPaid)).append("\n");
        sb.append("Total Outstanding: ").append(String.format("%.2f", totalOutstanding)).append("\n");

        // Sample bills
        sb.append("\nRecent Bills (Sample):\n");
        bills.stream().limit(10).forEach(bill -> {
            sb.append(String.format("  - Bill %s | Date: %s | Amount: %.2f | Paid: %.2f | Status: %s\n",
                    bill.getBillNumber(),
                    bill.getBillDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    bill.getTotalAmount(),
                    bill.getPaidAmount(),
                    bill.getPaymentStatus()));
        });

        return sb.toString();
    }

    private String formatProductsData(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("Total Products: ").append(products.size()).append("\n");

        double totalInventoryValue = products.stream()
                .mapToDouble(p -> p.getStockQuantity() * p.getCurrentRate())
                .sum();

        sb.append("Total Inventory Value: ").append(String.format("%.2f", totalInventoryValue)).append("\n");

        long lowStockCount = products.stream()
                .filter(p -> p.getStockQuantity() < p.getLowStockThreshold())
                .count();

        sb.append("Low Stock Products: ").append(lowStockCount).append("\n");

        // Sample products
        sb.append("\nProducts (Sample):\n");
        products.stream().limit(15).forEach(product -> {
            sb.append(String.format("  - %s (%s) | Stock: %.2f %s | Rate: %.2f\n",
                    product.getProductName(),
                    product.getProductCode(),
                    product.getStockQuantity(),
                    product.getUnit(),
                    product.getCurrentRate()));
        });

        return sb.toString();
    }

    private String formatCustomersData(List<Customer> customers) {
        StringBuilder sb = new StringBuilder();
        sb.append("Total Customers: ").append(customers.size()).append("\n");

        // Sample customers
        sb.append("\nCustomers (Sample):\n");
        customers.stream().limit(10).forEach(customer -> {
            sb.append(String.format("  - %s | Phone: %s | Email: %s | Credit Limit: %.2f\n",
                    customer.getCustomerName(),
                    customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "N/A",
                    customer.getEmail() != null ? customer.getEmail() : "N/A",
                    customer.getCreditLimit()));
        });

        return sb.toString();
    }

    private String callAIAPI(String prompt, String contextData) throws Exception {
        log.info("Calling AI API: {}", aiApiUrl);

        // Build request payload
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", aiModel);

        ArrayNode messages = requestBody.putArray("messages");

        // System message with context
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "You are a business intelligence analyst for a Fruit/Fish Wholesaler and Grocery Store. " +
                "Analyze the provided data and generate insights, summaries, and recommendations. " +
                "Be concise, accurate, and actionable in your analysis.");

        // User message with context data
        ObjectNode contextMessage = messages.addObject();
        contextMessage.put("role", "user");
        contextMessage.put("content", "Here is the current business data:\n\n" + contextData);

        // User query
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

        // Make API call
        ResponseEntity<String> response = restTemplate.exchange(
                aiApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String aiContent = responseJson
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            log.info("AI API response received successfully");
            return aiContent;
        } else {
            throw new RuntimeException("AI API returned error: " + response.getStatusCode());
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
