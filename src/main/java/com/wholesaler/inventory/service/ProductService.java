package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.request.ProductRequest;
import com.wholesaler.inventory.dto.request.RateOverrideRequest;
import com.wholesaler.inventory.dto.response.ProductResponse;
import com.wholesaler.inventory.entity.Category;
import com.wholesaler.inventory.entity.Product;
import com.wholesaler.inventory.entity.RateHistory;
import com.wholesaler.inventory.exception.DuplicateResourceException;
import com.wholesaler.inventory.exception.ResourceNotFoundException;
import com.wholesaler.inventory.repository.CategoryRepository;
import com.wholesaler.inventory.repository.ProductRepository;
import com.wholesaler.inventory.repository.RateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final RateHistoryRepository rateHistoryRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getProductCode());

        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateResourceException("Product", "productCode", request.getProductCode());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Product product = Product.builder()
                .productCode(request.getProductCode())
                .productName(request.getProductName())
                .description(request.getDescription())
                .category(category)
                .unit(request.getUnit())
                .baseRate(request.getBaseRate())
                .currentRate(request.getCurrentRate() != null ? request.getCurrentRate() : request.getBaseRate())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0.0)
                .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10.0)
                .hsnCode(request.getHsnCode())
                .gstPercentage(request.getGstPercentage() != null ? request.getGstPercentage() : 0.0)
                .build();

        product = productRepository.save(product);

        RateHistory rateHistory = RateHistory.builder()
                .product(product)
                .rate(product.getBaseRate())
                .effectiveDate(LocalDate.now())
                .remarks("Initial rate")
                .build();
        rateHistoryRepository.save(rateHistory);

        log.info("Product created successfully with ID: {}", product.getId());
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse overrideRate(RateOverrideRequest request) {
        log.info("Overriding rate for product: {}", request.getProductId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        product.setCurrentRate(request.getRate());
        product = productRepository.save(product);

        RateHistory rateHistory = RateHistory.builder()
                .product(product)
                .rate(request.getRate())
                .effectiveDate(request.getEffectiveDate())
                .endDate(request.getEndDate())
                .remarks(request.getRemarks())
                .build();
        rateHistoryRepository.save(rateHistory);

        log.info("Rate overridden successfully for product: {}", product.getId());
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapToResponse(product);
    }

    @Transactional
    public void updateStock(Long productId, Double quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .description(product.getDescription())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getCategoryName())
                .unit(product.getUnit())
                .baseRate(product.getBaseRate())
                .currentRate(product.getCurrentRate())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .hsnCode(product.getHsnCode())
                .gstPercentage(product.getGstPercentage())
                .lowStock(product.getStockQuantity() <= product.getLowStockThreshold())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
