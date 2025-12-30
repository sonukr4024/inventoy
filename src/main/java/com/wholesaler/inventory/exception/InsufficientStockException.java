package com.wholesaler.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }

    public InsufficientStockException(String productName, Double available, Double required) {
        super(String.format("Insufficient stock for %s. Available: %.2f, Required: %.2f", 
              productName, available, required));
    }
}
