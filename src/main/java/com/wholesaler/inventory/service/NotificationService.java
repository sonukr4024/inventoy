package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.response.CustomerOutstandingResponse;
import com.wholesaler.inventory.entity.Customer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.notifications.from-email}")
    private String fromEmail;

    @Value("${app.notifications.from-name}")
    private String fromName;

    @Value("${app.notifications.email-retry-attempts:3}")
    private int maxRetryAttempts;

    @Async
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String body) {
        return CompletableFuture.completedFuture(sendEmail(to, subject, body));
    }

    @Async
    public CompletableFuture<Boolean> sendHtmlEmailAsync(String to, String subject, String htmlBody) {
        return CompletableFuture.completedFuture(sendHtmlEmail(to, subject, htmlBody));
    }

    public boolean sendEmail(String to, String subject, String body) {
        log.info("Sending email to: {} with subject: {}", to, subject);

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(String.format("%s <%s>", fromName, fromEmail));
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);

                mailSender.send(message);
                log.info("Email sent successfully to: {}", to);
                return true;

            } catch (Exception e) {
                log.error("Failed to send email (attempt {}/{}): {}", attempt, maxRetryAttempts, e.getMessage());

                if (attempt == maxRetryAttempts) {
                    log.error("All email sending attempts failed for: {}", to);
                    return false;
                }

                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("Sending HTML email to: {} with subject: {}", to, subject);

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);

                mailSender.send(message);
                log.info("HTML email sent successfully to: {}", to);
                return true;

            } catch (MessagingException e) {
                log.error("Failed to send HTML email (attempt {}/{}): {}", attempt, maxRetryAttempts, e.getMessage());

                if (attempt == maxRetryAttempts) {
                    log.error("All HTML email sending attempts failed for: {}", to);
                    return false;
                }

                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    public void sendMonthlyDueReminder(Customer customer, CustomerOutstandingResponse outstanding) {
        if (customer.getEmail() == null || customer.getEmail().isEmpty()) {
            log.warn("Cannot send due reminder - no email for customer: {}", customer.getCustomerName());
            return;
        }

        String subject = "Payment Reminder - Outstanding Balance";
        String htmlBody = buildDueReminderHtml(customer, outstanding);

        sendHtmlEmailAsync(customer.getEmail(), subject, htmlBody);
    }

    public void sendBulkDueReminders(List<CustomerOutstandingResponse> outstandingList) {
        log.info("Sending bulk due reminders to {} customers", outstandingList.size());

        for (CustomerOutstandingResponse outstanding : outstandingList) {
            if (outstanding.getEmail() != null && !outstanding.getEmail().isEmpty() &&
                outstanding.getTotalOutstanding() > 0) {

                Customer customer = new Customer();
                customer.setCustomerName(outstanding.getCustomerName());
                customer.setEmail(outstanding.getEmail());
                customer.setPhoneNumber(outstanding.getPhoneNumber());

                sendMonthlyDueReminder(customer, outstanding);
            }
        }

        log.info("Bulk due reminders sent successfully");
    }

    private String buildDueReminderHtml(Customer customer, CustomerOutstandingResponse outstanding) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }");
        html.append(".content { padding: 20px; background-color: #f9f9f9; }");
        html.append(".bill-table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append(".bill-table th, .bill-table td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
        html.append(".bill-table th { background-color: #4CAF50; color: white; }");
        html.append(".total { font-size: 18px; font-weight: bold; color: #d32f2f; }");
        html.append(".footer { text-align: center; padding: 20px; font-size: 12px; color: #777; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h2>").append(fromName).append("</h2>");
        html.append("<p>Payment Reminder</p>");
        html.append("</div>");

        html.append("<div class='content'>");
        html.append("<p>Dear ").append(customer.getCustomerName()).append(",</p>");
        html.append("<p>This is a friendly reminder that you have an outstanding balance with us.</p>");

        html.append("<h3>Outstanding Summary:</h3>");
        html.append("<table class='bill-table'>");
        html.append("<tr><th>Details</th><th>Amount</th></tr>");
        html.append("<tr><td>Total Outstanding</td><td class='total'>₹")
                .append(String.format("%.2f", outstanding.getTotalOutstanding())).append("</td></tr>");
        html.append("<tr><td>Credit Limit</td><td>₹")
                .append(String.format("%.2f", outstanding.getCreditLimit())).append("</td></tr>");
        html.append("</table>");

        if (outstanding.getOutstandingBills() != null && !outstanding.getOutstandingBills().isEmpty()) {
            html.append("<h3>Outstanding Bills:</h3>");
            html.append("<table class='bill-table'>");
            html.append("<tr><th>Bill Number</th><th>Date</th><th>Total</th><th>Balance</th><th>Days Overdue</th></tr>");

            for (CustomerOutstandingResponse.OutstandingBill bill : outstanding.getOutstandingBills()) {
                html.append("<tr>");
                html.append("<td>").append(bill.getBillNumber()).append("</td>");
                html.append("<td>").append(bill.getBillDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</td>");
                html.append("<td>₹").append(String.format("%.2f", bill.getTotalAmount())).append("</td>");
                html.append("<td>₹").append(String.format("%.2f", bill.getBalanceAmount())).append("</td>");
                html.append("<td>").append(bill.getDaysOverdue()).append(" days</td>");
                html.append("</tr>");
            }

            html.append("</table>");
        }

        html.append("<p>Please arrange to clear this outstanding amount at your earliest convenience.</p>");
        html.append("<p>If you have already made the payment, please disregard this reminder.</p>");
        html.append("<p>For any queries, please contact us.</p>");
        html.append("<p>Thank you for your business!</p>");
        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>This is an automated message. Please do not reply to this email.</p>");
        html.append("<p>&copy; ").append(LocalDateTime.now().getYear()).append(" ")
                .append(fromName).append(". All rights reserved.</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    public void sendLowStockAlert(String recipientEmail, List<String> lowStockProducts) {
        String subject = "Low Stock Alert - Inventory Management System";

        StringBuilder body = new StringBuilder();
        body.append("Low Stock Alert\n\n");
        body.append("The following products are running low on stock:\n\n");

        for (String product : lowStockProducts) {
            body.append("- ").append(product).append("\n");
        }

        body.append("\nPlease restock these items at your earliest convenience.\n\n");
        body.append("Generated at: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        sendEmailAsync(recipientEmail, subject, body.toString());
    }

    public void sendBillReceipt(String customerEmail, String billNumber, String customerName,
                                double totalAmount, double paidAmount, double balanceAmount) {
        String subject = "Bill Receipt - " + billNumber;

        String htmlBody = buildBillReceiptHtml(billNumber, customerName, totalAmount, paidAmount, balanceAmount);

        sendHtmlEmailAsync(customerEmail, subject, htmlBody);
    }

    private String buildBillReceiptHtml(String billNumber, String customerName,
                                       double totalAmount, double paidAmount, double balanceAmount) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");
        html.append("<h2 style='color: #4CAF50;'>Bill Receipt</h2>");
        html.append("<p>Dear ").append(customerName).append(",</p>");
        html.append("<p>Thank you for your purchase. Here are your bill details:</p>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr><td style='padding: 10px; border: 1px solid #ddd;'><strong>Bill Number:</strong></td>");
        html.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(billNumber).append("</td></tr>");
        html.append("<tr><td style='padding: 10px; border: 1px solid #ddd;'><strong>Total Amount:</strong></td>");
        html.append("<td style='padding: 10px; border: 1px solid #ddd;'>₹").append(String.format("%.2f", totalAmount)).append("</td></tr>");
        html.append("<tr><td style='padding: 10px; border: 1px solid #ddd;'><strong>Paid Amount:</strong></td>");
        html.append("<td style='padding: 10px; border: 1px solid #ddd;'>₹").append(String.format("%.2f", paidAmount)).append("</td></tr>");
        html.append("<tr><td style='padding: 10px; border: 1px solid #ddd;'><strong>Balance Amount:</strong></td>");
        html.append("<td style='padding: 10px; border: 1px solid #ddd;'>₹").append(String.format("%.2f", balanceAmount)).append("</td></tr>");
        html.append("</table>");
        html.append("<p>Thank you for your business!</p>");
        html.append("<p style='font-size: 12px; color: #777;'>").append(fromName).append("</p>");
        html.append("</div></body></html>");

        return html.toString();
    }
}
