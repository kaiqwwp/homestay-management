package com.homestay.homestay.service;

import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeEventService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final DashboardService dashboardService;

    public RealtimeEventService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public SseEmitter subscribe() {
        // Timeout 30 phút, client EventSource sẽ tự động kết nối lại
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError(ex -> {
            emitter.complete();
            emitters.remove(emitter);
        });

        try {
            Map<String, Object> initData = new HashMap<>();
            initData.put("status", "CONNECTED");
            initData.put("timestamp", LocalDateTime.now().format(TIME_FMT));
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(initData, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void publishBookingEvent(String eventType, Booking booking, String customMessage) {
        if (emitters.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("timestamp", LocalDateTime.now().format(TIME_FMT));

        String checkInStr = booking.getCheckIn() != null ? booking.getCheckIn().format(DATE_FMT) : "";
        String checkOutStr = booking.getCheckOut() != null ? booking.getCheckOut().format(DATE_FMT) : "";
        String customerName = booking.getCustomer() != null ? booking.getCustomer().getFullName() : "Khách hàng";
        String customerPhone = booking.getCustomer() != null ? booking.getCustomer().getPhone() : "";
        String customerEmail = (booking.getCustomer() != null && booking.getCustomer().getEmail() != null) ? booking.getCustomer().getEmail() : "";
        Long roomId = booking.getRoom() != null ? booking.getRoom().getId() : null;
        String roomNumber = booking.getRoom() != null ? booking.getRoom().getRoomNumber() : "";
        String roomStatus = booking.getRoom() != null ? booking.getRoom().getStatus() : "AVAILABLE";
        String roomTypeName = (booking.getRoom() != null && booking.getRoom().getRoomType() != null)
                ? booking.getRoom().getRoomType().getName() : "";

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("id", booking.getId());
        bookingData.put("customerName", customerName);
        bookingData.put("customerPhone", customerPhone);
        bookingData.put("customerEmail", customerEmail);
        bookingData.put("roomId", roomId);
        bookingData.put("roomNumber", roomNumber);
        bookingData.put("roomStatus", roomStatus);
        bookingData.put("roomTypeName", roomTypeName);
        bookingData.put("checkIn", checkInStr);
        bookingData.put("checkOut", checkOutStr);
        bookingData.put("checkInIso", booking.getCheckIn() != null ? booking.getCheckIn().toString() : "");
        bookingData.put("checkOutIso", booking.getCheckOut() != null ? booking.getCheckOut().toString() : "");
        bookingData.put("status", booking.getStatus());
        bookingData.put("statusLabel", mapStatusLabel(booking.getStatus()));
        bookingData.put("rejectReason", booking.getRejectReason() != null ? booking.getRejectReason() : "");
        bookingData.put("compensationCode", booking.getCompensationCode() != null ? booking.getCompensationCode() : "");
        bookingData.put("compensationDiscount", booking.getCompensationDiscount() != null ? booking.getCompensationDiscount() : 0);
        bookingData.put("appliedVoucherCode", booking.getAppliedVoucherCode() != null ? booking.getAppliedVoucherCode() : "");
        bookingData.put("discountPercent", booking.getDiscountPercent() != null ? booking.getDiscountPercent() : 0);
        payload.put("booking", bookingData);

        String message = customMessage;
        if (message == null || message.isBlank()) {
            message = "Đơn #" + booking.getId() + " - " + customerName + " (Phòng " + roomNumber + ")";
        }
        payload.put("message", message);

        // Đính kèm số liệu thống kê Dashboard thời gian thực
        try {
            Map<String, Object> rawStats = dashboardService.getDashboardStats();
            Map<String, Object> stats = new HashMap<>(rawStats);
            Object revObj = rawStats.get("monthRevenue");
            BigDecimal rev = (revObj instanceof BigDecimal) ? (BigDecimal) revObj : BigDecimal.ZERO;
            DecimalFormat df = new DecimalFormat("#,###");
            stats.put("monthRevenueFormatted", df.format(rev) + " đ");
            payload.put("stats", stats);
        } catch (Exception ex) {
            log.warn("Không thể tính stats realtime: {}", ex.getMessage());
        }

        broadcast("booking-event", payload);
    }

    public void publishCustomerEvent(String eventType, Customer customer, String customMessage) {
        if (emitters.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("timestamp", LocalDateTime.now().format(TIME_FMT));

        Map<String, Object> custData = new HashMap<>();
        custData.put("id", customer.getId());
        custData.put("fullName", customer.getFullName());
        custData.put("phone", customer.getPhone() != null ? customer.getPhone() : "");
        custData.put("email", customer.getEmail() != null ? customer.getEmail() : "");
        custData.put("idNumber", customer.getIdNumber() != null ? customer.getIdNumber() : "Chưa có");
        payload.put("customer", custData);
        payload.put("message", customMessage != null ? customMessage : ("Khách hàng mới: " + customer.getFullName()));

        try {
            Map<String, Object> rawStats = dashboardService.getDashboardStats();
            payload.put("stats", rawStats);
        } catch (Exception ex) {
            log.warn("Không thể tính stats realtime: {}", ex.getMessage());
        }

        broadcast("customer-event", payload);
    }

    private String mapStatusLabel(String status) {
        if ("PENDING".equalsIgnoreCase(status)) return "Chờ xác nhận";
        if ("BOOKED".equalsIgnoreCase(status)) return "Đã chấp nhận";
        if ("REJECTED".equalsIgnoreCase(status)) return "Đã từ chối";
        if ("CHECKED_IN".equalsIgnoreCase(status)) return "Đang lưu trú";
        if ("PAID".equalsIgnoreCase(status)) return "Đã thanh toán";
        if ("CHECKED_OUT".equalsIgnoreCase(status)) return "Hoàn tất (Đã trả)";
        if ("CANCELLED".equalsIgnoreCase(status)) return "Đã hủy";
        return status;
    }

    public void broadcast(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}

