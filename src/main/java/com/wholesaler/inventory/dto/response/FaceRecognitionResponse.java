package com.wholesaler.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaceRecognitionResponse {
    private Boolean recognized;
    private Long customerId;
    private String customerName;
    private String phoneNumber;
    private Double confidenceScore;
    private String message;
}
