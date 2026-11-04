package com.homestay.homestay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.homestay.dto.sepay.SepayWebhookPayload;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SepayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(SepayPaymentService.class);

    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${sepay.bank.name:MBBank}")
    private String bankName;

    @Value("${sepay.bank.account:0123456789}")
    private String bankAccount;

    @Value("${sepay.bank.account-name:HOMESTAY PRO}")
    private String accountName;

    @Value("${sepay.api.key:}")
    private String sepayApiKey;

    @Value("${sepay.transfer.prefix:HOMESTAY}")
    private String transferPrefix;

    public SepayPaymentService(BookingRepository bookingRepository,
                               InvoiceRepository invoiceRepository,
                               InvoiceService invoiceService) {
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getTransferContent(Long bookingId) {
        String prefix = (transferPrefix != null && !transferPrefix.isBlank()) ? transferPrefix.trim() : "HOMESTAY";
        return prefix + bookingId;
    }

    public String generateVietQrUrl(Long bookingId, BigDecimal amount) {
        String content = getTransferContent(bookingId);
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);

        long amt = (amount != null) ? amount.longValue() : 0L;

        // URL chuẩn sinh mã QR SePay (MBBank / VietQR)
        return "https://qr.sepay.vn/img?acc=" + URLEncoder.encode(bankAccount.trim(), StandardCharsets.UTF_8) +
                "&bank=" + URLEncoder.encode(bankName.trim(), StandardCharsets.UTF_8) +
                "&amount=" + amt +
                "&des=" + encodedContent;
    }

    @Transactional
    public boolean processWebhook(SepayWebhookPayload payload, String apiKeyHeader) {
        // Kiểm tra API Key nếu đã cấu hình
        if (sepayApiKey != null && !sepayApiKey.trim().isEmpty()) {
            String expected = "Apikey " + sepayApiKey.trim();
            String expectedBearer = "Bearer " + sepayApiKey.trim();
            if (apiKeyHeader != null && !apiKeyHeader.trim().isEmpty()
                    && !apiKeyHeader.trim().equalsIgnoreCase(expected)
                    && !apiKeyHeader.trim().equalsIgnoreCase(expectedBearer)) {
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

        String prefix = (transferPrefix != null && !transferPrefix.isBlank()) ? transferPrefix.trim() : "HOMESTAY";
        Pattern pattern = Pattern.compile("(?i)(?:" + Pattern.quote(prefix) + "|HOMESTAY|HD|BOOKING)[\\s\\-_]*0*(\\d+)");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            Long bookingId = Long.parseLong(matcher.group(1));
            Optional<Booking> optBooking = bookingRepository.findById(bookingId);
            if (optBooking.isPresent()) {
                Booking booking = optBooking.get();
                if ("CHECKED_IN".equals(booking.getStatus())) {
                    Invoice invoice = invoiceService.checkOutAndCreateInvoice(bookingId, "QR SePay (MB Bank)");
                    invoice.setPaymentReference(payload.getReferenceCode() != null ? payload.getReferenceCode() : String.valueOf(payload.getId()));
                    invoiceRepository.save(invoice);
                    log.info("SePay Webhook: Đã tự động tạo hóa đơn và chuyển trạng thái Đã thanh toán cho Booking #{}", bookingId);
                    return true;
                }
            }
        }

        return true;
    }

    /**
     * Chủ động kiểm tra lịch sử giao dịch từ SePay User API (nếu đã cấu hình sepay.api.key)
     * Giúp tự động xác nhận thanh toán ngay cả khi chạy trên localhost không có public webhook.
     */
    @Transactional
    public boolean checkTransactionFromSepayApi(Long bookingId, BigDecimal expectedAmount) {
        if (sepayApiKey == null || sepayApiKey.trim().isEmpty()) {
            return false;
        }
        Optional<Booking> optBooking = bookingRepository.findById(bookingId);
        if (optBooking.isEmpty() || !"CHECKED_IN".equalsIgnoreCase(optBooking.get().getStatus())) {
            return invoiceRepository.findByBookingId(bookingId).isPresent();
        }

        try {
            String url = "https://my.sepay.vn/userapi/transactions/list?account_number="
                    + URLEncoder.encode(bankAccount.trim(), StandardCharsets.UTF_8) + "&limit=20";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + sepayApiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode transactions = root.path("transactions");
                if (transactions.isArray()) {
                    String prefix = (transferPrefix != null && !transferPrefix.isBlank()) ? transferPrefix.trim() : "HOMESTAY";
                    Pattern pattern = Pattern.compile("(?i)(?:" + Pattern.quote(prefix) + "|HOMESTAY|HD|BOOKING)\\s*0*" + bookingId + "(?!\\d)");

                    for (JsonNode tx : transactions) {
                        String txContent = tx.path("transaction_content").asText("");
                        double amountIn = tx.path("amount_in").asDouble(0.0);
                        String refCode = tx.path("reference_number").asText(tx.path("id").asText("SEPAY"));

                        if (pattern.matcher(txContent).find() && amountIn >= expectedAmount.doubleValue()) {
                            Invoice invoice = invoiceService.checkOutAndCreateInvoice(bookingId, "QR SePay (MB Bank)");
                            invoice.setPaymentReference(refCode);
                            invoiceRepository.save(invoice);
                            log.info("SePay API Polling: Đã xác nhận giao dịch {} và chuyển Booking #{} sang Đã thanh toán!", refCode, bookingId);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Không thể truy vấn SePay API: {}", ex.getMessage());
        }
        return false;
    }
}
