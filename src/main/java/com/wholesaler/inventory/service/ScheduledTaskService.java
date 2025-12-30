package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.response.CustomerOutstandingResponse;
import com.wholesaler.inventory.entity.Product;
import com.wholesaler.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final CreditService creditService;
    private final NotificationService notificationService;
    private final ProductRepository productRepository;

    @Value("${app.notifications.from-email}")
    private String adminEmail;

    /**
     * Scheduled task to send monthly due reminders
     * Runs on the 1st of every month at 9 AM
     * Cron: 0 0 9 1 * ?
     */
    @Scheduled(cron = "${app.notifications.monthly-due-check-cron:0 0 9 1 * ?}")
    public void sendMonthlyDueReminders() {
        log.info("Starting monthly due reminders task at {}", LocalDateTime.now());

        try {
            List<CustomerOutstandingResponse> outstandingList = creditService.getAllOutstanding();

            // Filter customers with outstanding balance
            List<CustomerOutstandingResponse> customersWithDues = outstandingList.stream()
                    .filter(outstanding -> outstanding.getTotalOutstanding() > 0)
                    .collect(Collectors.toList());

            if (customersWithDues.isEmpty()) {
                log.info("No customers with outstanding dues found");
                return;
            }

            log.info("Found {} customers with outstanding dues", customersWithDues.size());

            // Send reminders in bulk
            notificationService.sendBulkDueReminders(customersWithDues);

            log.info("Monthly due reminders sent successfully to {} customers", customersWithDues.size());

        } catch (Exception e) {
            log.error("Error sending monthly due reminders", e);
        }
    }

    /**
     * Scheduled task to check low stock and send alerts
     * Runs every day at 8 AM
     * Cron: 0 0 8 * * ?
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkLowStockAndAlert() {
        log.info("Starting low stock check task at {}", LocalDateTime.now());

        try {
            List<Product> lowStockProducts = productRepository.findLowStockProducts();

            if (lowStockProducts.isEmpty()) {
                log.info("No low stock products found");
                return;
            }

            log.info("Found {} low stock products", lowStockProducts.size());

            List<String> productNames = lowStockProducts.stream()
                    .map(product -> String.format("%s (%s) - Current: %.2f %s, Threshold: %.2f %s",
                            product.getProductName(),
                            product.getProductCode(),
                            product.getStockQuantity(),
                            product.getUnit(),
                            product.getLowStockThreshold(),
                            product.getUnit()))
                    .collect(Collectors.toList());

            // Send alert to admin
            notificationService.sendLowStockAlert(adminEmail, productNames);

            log.info("Low stock alert sent successfully");

        } catch (Exception e) {
            log.error("Error checking low stock", e);
        }
    }

    /**
     * Scheduled task to clean up temporary files
     * Runs every day at 2 AM
     * Cron: 0 0 2 * * ?
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupTempFiles() {
        log.info("Starting temp files cleanup task at {}", LocalDateTime.now());

        try {
            // TODO: Implement cleanup logic for temporary face recognition files
            // Delete files older than 7 days from uploads/face-images/temp_*

            log.info("Temp files cleanup completed");

        } catch (Exception e) {
            log.error("Error cleaning up temp files", e);
        }
    }

    /**
     * Scheduled task for daily sales summary
     * Runs every day at 11 PM
     * Cron: 0 0 23 * * ?
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void generateDailySalesSummary() {
        log.info("Generating daily sales summary at {}", LocalDateTime.now());

        try {
            // TODO: Generate and email daily sales summary to admin
            // This can use the ReportService to generate the report

            log.info("Daily sales summary generated");

        } catch (Exception e) {
            log.error("Error generating daily sales summary", e);
        }
    }

    /**
     * Scheduled task to update product rates
     * Runs every Sunday at 6 AM
     * Cron: 0 0 6 ? * SUN
     */
    @Scheduled(cron = "0 0 6 ? * SUN")
    public void weeklyRateUpdate() {
        log.info("Starting weekly rate update check at {}", LocalDateTime.now());

        try {
            // TODO: Check for any pending rate updates and apply them
            // This could involve checking a rate_update_queue table

            log.info("Weekly rate update completed");

        } catch (Exception e) {
            log.error("Error during weekly rate update", e);
        }
    }


    @Scheduled(cron = "0 */5 * * * ?")
    public void healthCheck() {
        log.debug("Health check - System running at {}", LocalDateTime.now());
    }

    /**
     * Manual trigger for monthly due reminders (can be called via API)
     */
    public void triggerMonthlyDueRemindersManually() {
        log.info("Manually triggering monthly due reminders");
        sendMonthlyDueReminders();
    }

    /**
     * Manual trigger for low stock alert (can be called via API)
     */
    public void triggerLowStockAlertManually() {
        log.info("Manually triggering low stock alert");
        checkLowStockAndAlert();
    }
}
