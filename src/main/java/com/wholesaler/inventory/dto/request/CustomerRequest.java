package com.wholesaler.inventory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String phoneNumber;

    @Email(message = "Email must be valid")
    private String email;

    private String address;

    private String gstin;

    private Double creditLimit;
}
