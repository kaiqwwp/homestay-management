package com.homestay.homestay.service;

import com.homestay.homestay.dto.sepay.SepayWebhookPayload;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SepayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(SepayPaymentService.class);

    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    @Value("${sepay.bank.name:MBBank}")
    private String bankName;

    @Value("${sepay.bank.account:0123456789}")
    private String bankAccount;

    @Value("${sepay.bank.account-name:HOMESTAY PRO}")
    private String accountName;

    @Value("${sepay.api.key:}")
    private String sepayApiKey;

    public SepayPaymentService(BookingRepository bookingRepository,
                               InvoiceRepository invoiceRepository,
                               InvoiceService invoiceService) {
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
    }

    public String generateVietQrUrl(Long bookingId, BigDecimal amount) {
        String content = "HOMESTAY " + bookingId;
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
        String encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        long amt = (amount != null) ? amount.longValue() : 0L;

        // URL chuẩn sinh mã VietQR nhanh
        return "https://img.vietqr.io/image/" + bankName + "-" + bankAccount + "-compact2.png" +
                "?amount=" + amt +
                "&addInfo=" + encodedContent +
                "&accountName=" + encodedAccountName;
    }

    @Transactional
    public boolean processWebhook(SepayWebhookPayload payload, String apiKeyHeader) {
        // Kiểm tra API Key nếu đã cấu hình
        if (sepayApiKey != null && !sepayApiKey.trim().isEmpty()) {
            String expected = "Apikey " + sepayApiKey.trim();
            if (apiKeyHeader == null || !apiKeyHeader.trim().equalsIgnoreCase(expected)) {
                log.warn("SePay Webhook: Sai API Key hoặc chưa được cấp quyền.");
                return false;
            }
        }

        if (payload == null || !"in".equalsIgnoreCase(payload.getTransferType())) {
            log.info("Bỏ qua giao dịch SePay (không phải giao dịch nạp/nhận tiền): {}", payload);
            return false;
        }

        String content = payload.getContent();
        if (content == null || content.isEmpty()) {
            content = payload.getDescription();
        }

        if (content == null) {
            return false;
        }

        log.info("Nhận SePay Webhook - Nội dung: {}, Số tiền: {}", content, payload.getTransferAmount());

        // Regex tìm mã đơn booking dạng "HOMESTAY 123" hoặc "HD 123"
        Pattern pattern = Pattern.compile("(?i)(?:HOMESTAY|HD|BOOKING)\\s*(\\d+)");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            Long bookingId = Long.parseLong(matcher.group(1));
            Optional<Booking> optBooking = bookingRepository.findById(bookingId);
            if (optBooking.isPresent()) {
                Booking booking = optBooking.get();
                // Nếu đơn đang CHECKED_IN, tự động chuyển sang CHECKED_OUT và tạo hóa đơn
                if ("CHECKED_IN".equals(booking.getStatus())) {
                    Invoice invoice = invoiceService.checkOutAndCreateInvoice(bookingId, "Chuyển khoản SePay (VietQR)");
                    invoice.setPaymentReference(payload.getReferenceCode() != null ? payload.getReferenceCode() : String.valueOf(payload.getId()));
                    invoiceRepository.save(invoice);
                    log.info("SePay: Đã tự động tạo hóa đơn và thanh toán cho Booking #{}", bookingId);
                    return true;
                } else if ("BOOKED".equals(booking.getStatus())) {
                    log.info("SePay: Nhận tiền đặt cọc cho Booking #{}", bookingId);
                    return true;
                }
            }
        }

        return true;
    }
}
