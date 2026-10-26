package com.homestay.homestay.controller.api;

import com.homestay.homestay.dto.sepay.SepayWebhookPayload;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.service.BookingService;
import com.homestay.homestay.service.InvoiceService;
import com.homestay.homestay.service.SepayPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentApiController {

    private final SepayPaymentService sepayPaymentService;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;

    public PaymentApiController(SepayPaymentService sepayPaymentService,
                                BookingService bookingService,
                                InvoiceService invoiceService) {
        this.sepayPaymentService = sepayPaymentService;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
    }

    @PostMapping("/sepay-webhook")
    public ResponseEntity<Map<String, Object>> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SepayWebhookPayload payload) {

        boolean success = sepayPaymentService.processWebhook(payload, authHeader);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Xử lý giao dịch SePay thành công" : "Bỏ qua hoặc không khớp giao dịch"
        ));
    }

    @GetMapping("/vietqr/{bookingId}")
    public ResponseEntity<Map<String, Object>> getVietQr(@PathVariable("bookingId") Long bookingId) {
        Booking booking = bookingService.findById(bookingId);
        BigDecimal roomTotal = invoiceService.calculateRoomTotal(booking);
        BigDecimal serviceTotal = invoiceService.calculateServiceTotal(booking);
        BigDecimal grandTotal = roomTotal.add(serviceTotal);

        String qrUrl = sepayPaymentService.generateVietQrUrl(bookingId, grandTotal);

        return ResponseEntity.ok(Map.of(
                "bookingId", bookingId,
                "customerName", booking.getCustomer().getFullName(),
                "amount", grandTotal,
                "content", "HOMESTAY " + bookingId,
                "qrUrl", qrUrl
        ));
    }
}
