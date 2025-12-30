package com.wholesaler.inventory.controller;

import com.wholesaler.inventory.dto.request.AIReportRequest;
import com.wholesaler.inventory.dto.response.AIReportResponse;
import com.wholesaler.inventory.dto.response.ApiResponse;
import com.wholesaler.inventory.service.AIReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai-reports")
@RequiredArgsConstructor
@Tag(name = "AI Reports", description = "AI-powered report generation APIs")
public class AIReportController {

    private final AIReportService aiReportService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Generate AI report from natural language prompt")
    public ResponseEntity<ApiResponse<AIReportResponse>> generateReport(@Valid @RequestBody AIReportRequest request) {
        AIReportResponse report = aiReportService.generateAIReport(request);
        return ResponseEntity.ok(ApiResponse.success(report, "AI report generated"));
    }

    @PostMapping("/generate/async")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Generate AI report asynchronously")
    public ResponseEntity<ApiResponse<String>> generateReportAsync(@Valid @RequestBody AIReportRequest request) {
        CompletableFuture<AIReportResponse> future = aiReportService.generateAIReportAsync(request);

        return ResponseEntity.accepted()
                .body(ApiResponse.success("AI report generation initiated. Please check back later for results."));
    }
}
