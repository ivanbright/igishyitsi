package org.example.esubmission.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.esubmission.util.ConfigUtil;

import java.util.Properties;

/**
 * Service responsible for sending emails, specifically for 2FA verification.
 * <p>
 * When SMTP credentials are not configured, this service gracefully falls back
 * to logging the verification code / reset link to the server console so that
 * developers are never blocked from using the application locally.
 * </p>
 */
public class EmailService {

    // SMTP Configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SMTP_USER = ConfigUtil.getProperty("smtp.user");
    private static final String SMTP_PASS = ConfigUtil.getProperty("smtp.pass");

    /**
     * Whether SMTP is properly configured for real email delivery.
     */
    private static boolean isSmtpConfigured() {
        return SMTP_USER != null && !SMTP_USER.trim().isEmpty()
            && SMTP_PASS != null && !SMTP_PASS.trim().isEmpty();
    }

    /**
     * Sends a verification email with a 6-digit code.
     * Falls back to console logging when SMTP is not configured.
     *
     * @param toEmail target email address
     * @param code    verification code to send
     */
    public void sendVerificationEmail(String toEmail, String code) {
        if (!isSmtpConfigured()) {
            System.out.println("========================================");
            System.out.println("[DEV MODE] SMTP not configured.");
            System.out.println("[DEV MODE] Verification code for " + toEmail + ": " + code);
            System.out.println("========================================");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override 
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            } 
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Igishyitsi - Your Verification Code");
            
            String htmlContent = "<h1>Welcome to Igishyitsi</h1>" +
                                 "<p>Your security verification code is:</p>" +
                                 "<h2 style='color: #3b82f6; letter-spacing: 5px;'>" + code + "</h2>" +
                                 "<p>This code will expire in 10 minutes.</p>" +
                                 "<p>If you did not request this, please ignore this email.</p>";
            
            message.setContent(htmlContent, "text/html");

            Transport.send(message);
            System.out.println("Verification email sent to " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Failed to send verification email to " + toEmail + ": " + e.getMessage());
            System.out.println("[FALLBACK] Verification code for " + toEmail + ": " + code);
        }
    }

    /**
     * Sends a password reset email with a link.
     * Falls back to console logging when SMTP is not configured.
     *
     * @param toEmail   target email address
     * @param token     reset token
     * @param baseUrl   the base URL of the application (e.g., http://localhost:8080/igishyitsi)
     */
    public void sendPasswordResetEmail(String toEmail, String token, String baseUrl) {
        if (!isSmtpConfigured()) {
            System.out.println("========================================");
            System.out.println("[DEV MODE] SMTP not configured.");
            System.out.println("[DEV MODE] Password reset OTP for " + toEmail + ": " + token);
            System.out.println("========================================");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Igishyitsi - Reset Your Password");
            
            String htmlContent = "<h1>Password Reset Request</h1>" +
                                 "<p>You requested to reset your password. Here is your 6-digit OTP code:</p>" +
                                 "<h2 style='color: #3b82f6; letter-spacing: 5px;'>" + token + "</h2>" +
                                 "<p>This code will expire in 1 hour.</p>" +
                                 "<p>If you did not request this, please ignore this email.</p>";
            
            message.setContent(htmlContent, "text/html");

            Transport.send(message);
            System.out.println("Password reset email sent to " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email to " + toEmail + ": " + e.getMessage());
            System.out.println("[FALLBACK] Reset OTP for " + toEmail + ": " + token);
        }
    }

    /**
     * Legacy compatibility — calls the 3-arg version with a default localhost URL.
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        sendPasswordResetEmail(toEmail, token, ConfigUtil.getProperty("app.baseUrl", "http://localhost:8080/igishyitsi"));
    }
}
