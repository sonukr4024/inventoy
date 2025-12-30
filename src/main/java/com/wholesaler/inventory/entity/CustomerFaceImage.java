package com.wholesaler.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_face_images", indexes = {
    @Index(name = "idx_face_customer", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFaceImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "image_path", nullable = false, columnDefinition = "TEXT")
    private String imagePath;

    @Column(name = "face_embedding", columnDefinition = "bytea")
    @Lob
    private byte[] faceEmbedding;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "encoding_status", length = 20)
    private String encodingStatus = "PENDING"; // PENDING, COMPLETED, FAILED
}
