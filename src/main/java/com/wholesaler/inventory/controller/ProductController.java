package com.wholesaler.inventory.controller;

import com.wholesaler.inventory.dto.request.ProductRequest;
import com.wholesaler.inventory.dto.request.RateOverrideRequest;
import com.wholesaler.inventory.dto.response.ApiResponse;
import com.wholesaler.inventory.dto.response.ProductResponse;
import com.wholesaler.inventory.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Product created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> response = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
        ProductResponse response = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get low stock products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts() {
        List<ProductResponse> response = productService.getLowStockProducts();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/rate-override")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Override product rate")
    public ResponseEntity<ApiResponse<ProductResponse>> overrideRate(@Valid @RequestBody RateOverrideRequest request) {
        ProductResponse response = productService.overrideRate(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Rate overridden successfully"));
    }
}
