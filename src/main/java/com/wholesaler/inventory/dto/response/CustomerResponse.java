package com.wholesaler.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    private Long id;
    private String customerName;
    private String phoneNumber;
    private String email;
    private String address;
    private String gstin;
    private Double creditLimit;
    private Integer faceImageCount;
    private Double outstandingBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
