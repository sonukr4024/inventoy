package com.wholesaler.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wholesaler.inventory.enums.Unit;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_name", columnList = "product_name"),
    @Index(name = "idx_product_code", columnList = "product_code", unique = true),
    @Index(name = "idx_category", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Unit unit;

    @Column(name = "base_rate", nullable = false)
    private Double baseRate;

    @Column(name = "current_rate")
    private Double currentRate;

    @Column(name = "stock_quantity", nullable = false)
    private Double stockQuantity = 0.0;

    @Column(name = "low_stock_threshold")
    private Double lowStockThreshold = 10.0;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "gst_percentage")
    private Double gstPercentage = 0.0;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<RateHistory> rateHistories = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<BillItem> billItems = new ArrayList<>();
}
