package com.wholesaler.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIReportRequest {

    @NotBlank(message = "Prompt is required")
    private String prompt;

    private String reportType; // SALES, INVENTORY, CUSTOMER_CREDIT

    private String dateRange; // TODAY, LAST_7_DAYS, LAST_30_DAYS, CUSTOM
}
