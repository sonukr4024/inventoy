package com.wholesaler.inventory.controller;

import com.wholesaler.inventory.dto.request.BillRequest;
import com.wholesaler.inventory.dto.response.ApiResponse;
import com.wholesaler.inventory.dto.response.BillResponse;
import com.wholesaler.inventory.service.BillService;
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
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Billing management APIs")
public class BillController {

    private final BillService billService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create bill")
    public ResponseEntity<ApiResponse<BillResponse>> createBill(@Valid @RequestBody BillRequest request) {
        BillResponse response = billService.createBill(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Bill created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get bill by ID")
    public ResponseEntity<ApiResponse<BillResponse>> getBill(@PathVariable Long id) {
        BillResponse response = billService.getBill(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all bills")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getAllBills() {
        List<BillResponse> response = billService.getAllBills();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
