package com.homestay.homestay.service;

import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Invoice;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    private boolean isMailConfigured() {
        return mailSender != null
                && fromEmail != null && !fromEmail.trim().isEmpty()
                && mailPassword != null && !mailPassword.trim().isEmpty();
    }

    @Async
    public void sendBookingConfirmation(Booking booking) {
        String toEmail = booking.getCustomer().getEmail();
        if (toEmail == null || toEmail.trim().isEmpty() || !isMailConfigured()) {
            log.info("Bỏ qua gửi email đặt phòng (chưa cấu hình email khách hoặc SMTP): {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Homestay Pro] Xác nhận đơn đặt phòng #" + booking.getId());

            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 10px;">
                    <h2 style="color: #4f46e5; text-align: center;">XÁC NHẬN ĐẶT PHÒNG THÀNH CÔNG</h2>
                    <p>Kính gửi quý khách <strong>%s</strong>,</p>
                    <p>Homestay Pro xin chân thành cảm ơn quý khách đã tin tưởng đặt phòng. Dưới đây là thông tin chi tiết đơn đặt phòng của quý khách:</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-top: 15px;">
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Mã đơn đặt:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9; color: #4f46e5;">#%d</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Phòng:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">Phòng %s (%s)</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Ngày nhận phòng:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">%s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Ngày trả phòng:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">%s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Trạng thái:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9; color: #16a34a;">Đã xác nhận (BOOKED)</td></tr>
                    </table>
                    <p style="margin-top: 20px;">Quý khách vui lòng đến nhận phòng đúng thời gian quy định. Mọi thắc mắc xin liên hệ hotline: <strong>1900 6868</strong>.</p>
                    <hr style="border: none; border-top: 1px solid #cbd5e1; margin: 20px 0;"/>
                    <p style="font-size: 12px; color: #64748b; text-align: center;">Homestay Pro - Hệ thống quản lý nghỉ dưỡng thông minh</p>
                </div>
            """.formatted(
                    booking.getCustomer().getFullName(),
                    booking.getId(),
                    booking.getRoom().getRoomNumber(),
                    booking.getRoom().getRoomType().getName(),
                    booking.getCheckIn().format(df),
                    booking.getCheckOut().format(df)
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Đã gửi email xác nhận đặt phòng tới: {}", toEmail);
        } catch (Exception e) {
            log.warn("Không thể gửi email xác nhận đặt phòng: {}", e.getMessage());
        }
    }

    @Async
    public void sendBookingRejectionWithVoucher(Booking booking) {
        String toEmail = booking.getCustomer().getEmail();
        if (toEmail == null || toEmail.trim().isEmpty() || !isMailConfigured()) {
            log.info("Bỏ qua gửi email từ chối đặt phòng (chưa cấu hình SMTP): {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail.trim());
            helper.setTo(toEmail.trim());
            helper.setSubject("[Homestay Pro] Thông báo về yêu cầu đặt phòng #" + booking.getId() + " & Quà tặng Mã giảm giá đền bù");

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #fecaca; border-radius: 10px;">
                    <h2 style="color: #dc2626; text-align: center;">THÔNG BÁO TỪ CHỐI ĐẶT PHÒNG & QUÀ TẶNG ĐỀN BÙ</h2>
                    <p>Kính gửi quý khách <strong>%s</strong>,</p>
                    <p>Homestay Pro rất tiếc chưa thể đáp ứng yêu cầu đặt <strong>Phòng %s</strong> (Mã đơn #%d) của quý khách.</p>
                    <p><strong>Lý do:</strong> %s</p>
                    <div style="background: #fef9c3; border: 1px dashed #ca8a04; padding: 15px; border-radius: 8px; margin: 15px 0; text-align: center;">
                        <p style="margin: 0 0 8px 0;">Thay lời xin lỗi, Homestay Pro xin gửi tặng quý khách <strong>Mã giảm giá %d%%</strong> cho lần đặt phòng tiếp theo:</p>
                        <span style="font-size: 20px; font-weight: bold; color: #dc2626; letter-spacing: 1px;">%s</span>
                    </div>
                    <p>Rất mong được đón tiếp và phục vụ quý khách tại Homestay Pro!</p>
                </div>
            """.formatted(
                    booking.getCustomer().getFullName(),
                    booking.getRoom().getRoomNumber(),
                    booking.getId(),
                    booking.getRejectReason() != null ? booking.getRejectReason() : "Phòng đang bảo trì đột xuất",
                    booking.getCompensationDiscount() != null ? booking.getCompensationDiscount() : 10,
                    booking.getCompensationCode() != null ? booking.getCompensationCode() : ""
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Đã gửi email từ chối kèm mã giảm giá đền bù tới: {}", toEmail);
        } catch (Exception e) {
            log.warn("Không thể gửi email từ chối đặt phòng: {}", e.getMessage());
        }
    }

    @Async
    public void sendInvoiceReceipt(Invoice invoice) {
        String toEmail = invoice.getBooking().getCustomer().getEmail();
        if (toEmail == null || toEmail.trim().isEmpty() || !isMailConfigured()) {
            log.info("Bỏ qua gửi email hóa đơn (chưa cấu hình email khách hoặc SMTP): {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Homestay Pro] Hóa đơn thanh toán #" + invoice.getId());

            DecimalFormat cur = new DecimalFormat("#,##0 VNĐ");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 10px;">
                    <h2 style="color: #16a34a; text-align: center;">HÓA ĐƠN THANH TOÁN DỊCH VỤ</h2>
                    <p>Kính gửi quý khách <strong>%s</strong>,</p>
                    <p>Hóa đơn lưu trú của quý khách tại Homestay Pro đã được thanh toán thành công:</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-top: 15px;">
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Mã hóa đơn:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">#%d</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Phòng:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">Phòng %s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Tiền phòng:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">%s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Tiền dịch vụ:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">%s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Tổng thanh toán:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9; font-weight: bold; color: #16a34a; font-size: 16px;">%s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Hình thức:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">%s</td></tr>
                        <tr><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;"><strong>Thời gian:</strong></td><td style="padding: 8px; border-bottom: 1px solid #f1f5f9;">%s</td></tr>
                    </table>
                    <p style="margin-top: 20px;">Cảm ơn quý khách đã lưu trú tại Homestay Pro. Hẹn gặp lại quý khách lần sau!</p>
                </div>
            """.formatted(
                    invoice.getBooking().getCustomer().getFullName(),
                    invoice.getId(),
                    invoice.getBooking().getRoom().getRoomNumber(),
                    cur.format(invoice.getRoomTotal()),
                    cur.format(invoice.getServiceTotal()),
                    cur.format(invoice.getTotal()),
                    invoice.getPaymentMethod(),
                    invoice.getPaidAt() != null ? invoice.getPaidAt().format(dtf) : "-"
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Đã gửi email hóa đơn thanh toán tới: {}", toEmail);
        } catch (Exception e) {
            log.warn("Không thể gửi email hóa đơn thanh toán: {}", e.getMessage());
        }
    }
}
