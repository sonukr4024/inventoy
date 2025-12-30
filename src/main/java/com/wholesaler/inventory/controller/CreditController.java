package com.wholesaler.inventory.controller;

import com.wholesaler.inventory.dto.request.PaymentRequest;
import com.wholesaler.inventory.dto.response.ApiResponse;
import com.wholesaler.inventory.dto.response.CreditLedgerResponse;
import com.wholesaler.inventory.dto.response.CustomerOutstandingResponse;
import com.wholesaler.inventory.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
@Tag(name = "Credit Management", description = "Credit/Udhaari management APIs")
public class CreditController {

    private final CreditService creditService;

    @PostMapping("/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Record payment against customer outstanding")
    public ResponseEntity<ApiResponse<CreditLedgerResponse>> recordPayment(@Valid @RequestBody PaymentRequest request) {
        CreditLedgerResponse response = creditService.recordPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment recorded successfully"));
    }

    @GetMapping("/outstanding")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all customers with outstanding balance")
    public ResponseEntity<ApiResponse<List<CustomerOutstandingResponse>>> getAllOutstanding() {
        List<CustomerOutstandingResponse> response = creditService.getAllOutstanding();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/outstanding/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get outstanding details for a specific customer")
    public ResponseEntity<ApiResponse<CustomerOutstandingResponse>> getCustomerOutstanding(@PathVariable Long customerId) {
        CustomerOutstandingResponse response = creditService.getCustomerOutstanding(customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
