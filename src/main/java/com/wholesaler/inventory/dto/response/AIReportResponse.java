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
public class AIReportResponse {
    private Boolean success;
    private String prompt;
    private String reportContent;
    private LocalDateTime generatedAt;
    private String model;
    private String message;
}
