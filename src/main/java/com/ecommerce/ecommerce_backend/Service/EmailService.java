package com.ecommerce.ecommerce_backend.Service;

import com.ecommerce.ecommerce_backend.entity.CustomerOrder;
import com.ecommerce.ecommerce_backend.entity.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String body) throws MessagingException {
        if (mailHost == null || mailHost.isEmpty() || mailUsername == null || mailUsername.isEmpty() || mailPassword == null || mailPassword.isEmpty()) {
            logger.warn("Email service not fully configured. Host: {}, Username: {}. Simulating email send to: {}", mailHost, mailUsername, to);
            logger.info("Simulated email:\nTo: {}\nSubject: {}\nBody:\n{}", to, subject, body);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.setFrom(mailUsername);

            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            throw e;
        }
    }

    public void sendOrderConfirmationEmail(CustomerOrder order) throws MessagingException {
        if (order == null || order.getUser() == null || order.getUser().getEmail() == null) {
            logger.error("Cannot send order confirmation email: Order or user email is null");
            throw new IllegalArgumentException("Order or user email cannot be null");
        }

        String to = order.getUser().getEmail();
        String userName = order.getUser().getUsername();
        String orderId = String.valueOf(order.getId());
        double total = order.getTotal();
        String subject = "Order Confirmation - Order #" + orderId;

        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(userName).append(",\n\n");
        body.append("Thank you for your order! Here are the details:\n\n");
        body.append("Order ID: ").append(orderId).append("\n");
        body.append("Order Date: ").append(order.getOrderDate()).append("\n");
        body.append("Items:\n");
        for (OrderItem item : order.getItems()) {
            body.append("- ").append(item.getProduct().getName())
                    .append(" (Qty: ").append(item.getQuantity())
                    .append(", Price: $").append(item.getPrice())
                    .append(")\n");
        }
        body.append("Total Amount: $").append(total).append("\n\n");

        if (order.getShippingAddress() != null) {
            body.append("Shipping Address:\n");
            body.append(order.getShippingAddress().getFullName()).append("\n");
            body.append(order.getShippingAddress().getStreetAddress()).append("\n");
            body.append(order.getShippingAddress().getCity()).append(", ");
            body.append(order.getShippingAddress().getPostalCode()).append("\n\n");
        }

        body.append("We appreciate your business! If you have any questions, feel free to contact us.\n\n");
        body.append("Best regards,\nThe Ecommerce Team");

        sendEmail(to, subject, body.toString());
    }
}