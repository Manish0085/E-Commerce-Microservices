package com.sparepartshop.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender sender;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    public void sendOrderConfirmation(String toEmail,
                                      String customerName,
                                      String orderNumber,
                                      BigDecimal totalAmount) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Order Confirmation — " + orderNumber);
            helper.setText(buildBody(customerName, orderNumber, totalAmount), false);

            sender.send(message);
            log.info("Order confirmation email sent to {} for order {}", toEmail, orderNumber);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send email to {} for order {}: {}", toEmail, orderNumber, ex.getMessage(), ex);
        }
    }


    public void sendPaymentConfirmation(String toEmail,
                                        String customerName,
                                        String invoiceNumber,
                                        BigDecimal amount,
                                        String paymentReference) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Payment Received — Invoice " + invoiceNumber);
            helper.setText(String.format("""
                Hi %s,

                We've received your payment of Rs. %s for invoice %s.
                Reference: %s

                Thank you!

                — SparePartShop
                """, customerName, amount, invoiceNumber, paymentReference), false);

            sender.send(message);
            log.info("Payment confirmation email sent to {} for invoice {}", toEmail, invoiceNumber);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send payment confirmation to {} for invoice {}: {}",
                    toEmail, invoiceNumber, ex.getMessage(), ex);
        }
    }

    public void sendWelcomeEmail(String toEmail, String customerName) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to SparePartShop!");
            helper.setText(String.format("""
                Hi %s,

                Welcome aboard! Your SparePartShop account is ready.

                You can now browse our catalog of car filters, brake parts,
                and other spare parts. Need help? Just reply to this email.

                — SparePartShop
                """, customerName), false);

            sender.send(message);
            log.info("Welcome email sent to {}", toEmail);

        }catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send welcome email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }


    public void sendOrderCancellation(String toEmail,
                                      String customerName,
                                      String orderNumber,
                                      String reason) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(toEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Order Cancelled — " + orderNumber);
            helper.setText(String.format("""
                Hi %s,

                Your order %s has been cancelled.
                Reason: %s

                If this was a mistake, please contact support.

                — SparePartShop
                """, customerName, orderNumber, reason != null ? reason : "Not specified"), false);

            sender.send(message);
            log.info("Order cancellation email sent to {} for order {}", toEmail, orderNumber);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send cancellation email to {} for order {}: {}",
                    toEmail, orderNumber, ex.getMessage(), ex);
        }

    }
    private String buildBody(String name, String orderNumber, BigDecimal total) {
        return """
                Hi %s,

                Thank you for your order with SparePartShop!

                Order Number : %s
                Total Amount : Rs. %s

                We'll send another email when your order is on its way.

                — SparePartShop
                """.formatted(name, orderNumber, total);
    }
}