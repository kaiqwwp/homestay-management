package com.homestay.homestay.controller.api;

import com.homestay.homestay.dto.sepay.SepayWebhookPayload;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.service.BookingService;
import com.homestay.homestay.service.InvoiceService;
import com.homestay.homestay.service.RealtimeEventService;
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
    private final RealtimeEventService realtimeEventService;

    public PaymentApiController(SepayPaymentService sepayPaymentService,
                                BookingService bookingService,
                                InvoiceService invoiceService,
                                RealtimeEventService realtimeEventService) {
        this.sepayPaymentService = sepayPaymentService;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
        this.realtimeEventService = realtimeEventService;
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
        BigDecimal discountAmount = invoiceService.calculateDiscountAmount(booking, roomTotal, serviceTotal);
        BigDecimal grandTotal = roomTotal.add(serviceTotal).subtract(discountAmount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        String qrUrl = sepayPaymentService.generateVietQrUrl(bookingId, grandTotal);

        return ResponseEntity.ok(Map.of(
                "bookingId", bookingId,
                "customerName", booking.getCustomer().getFullName(),
                "amount", grandTotal,
                "bankName", sepayPaymentService.getBankName(),
                "bankAccount", sepayPaymentService.getBankAccount(),
                "accountName", sepayPaymentService.getAccountName(),
                "content", sepayPaymentService.getTransferContent(bookingId),
                "qrUrl", qrUrl
        ));
    }

    @GetMapping("/status/{bookingId}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(@PathVariable("bookingId") Long bookingId) {
        Booking booking = bookingService.findById(bookingId);
        Invoice existingInvoice = invoiceService.findByBookingId(bookingId);
        boolean isPaid = existingInvoice != null
                || "PAID".equalsIgnoreCase(booking.getStatus())
                || "CHECKED_OUT".equalsIgnoreCase(booking.getStatus());

        if (!isPaid && "CHECKED_IN".equalsIgnoreCase(booking.getStatus())) {
            BigDecimal roomTotal = invoiceService.calculateRoomTotal(booking);
            BigDecimal serviceTotal = invoiceService.calculateServiceTotal(booking);
            BigDecimal discountAmount = invoiceService.calculateDiscountAmount(booking, roomTotal, serviceTotal);
            BigDecimal grandTotal = roomTotal.add(serviceTotal).subtract(discountAmount);
            if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
                grandTotal = BigDecimal.ZERO;
            }
            isPaid = sepayPaymentService.checkTransactionFromSepayApi(bookingId, grandTotal);
            if (isPaid) {
                booking = bookingService.findById(bookingId);
            }
        }

        return ResponseEntity.ok(Map.of(
                "bookingId", bookingId,
                "paid", isPaid,
                "status", booking.getStatus()
        ));
    }

    @PostMapping("/request-cash/{bookingId}")
    public ResponseEntity<Map<String, Object>> requestCashPayment(@PathVariable("bookingId") Long bookingId) {
        Booking booking = bookingService.findById(bookingId);
        if (realtimeEventService != null) {
            String msg = String.format("Khách hàng %s (Đơn #%d - Phòng %s) chọn THANH TOÁN TIỀN MẶT tại quầy. Vui lòng thu tiền và bấm Xác nhận Thanh toán!",
                    booking.getCustomer().getFullName(),
                    booking.getId(),
                    booking.getRoom().getRoomNumber());
            realtimeEventService.publishBookingEvent("BOOKING_CASH_REQUEST", booking, msg);
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi thông báo thanh toán tiền mặt đến Quản lý / Admin! Vui lòng thanh toán tại quầy lễ tân để Admin xác nhận."
        ));
    }
}
