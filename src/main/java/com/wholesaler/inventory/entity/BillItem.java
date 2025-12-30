package com.wholesaler.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bill_items", indexes = {
    @Index(name = "idx_bill_item_bill", columnList = "bill_id"),
    @Index(name = "idx_bill_item_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @Column(name = "tax_percentage")
    private Double taxPercentage = 0.0;

    @Column(name = "tax_amount")
    private Double taxAmount = 0.0;

    @Column(name = "discount_percentage")
    private Double discountPercentage = 0.0;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "line_total", nullable = false)
    private Double lineTotal;
}
