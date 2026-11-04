package com.homestay.homestay.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    /**
     * Trạng thái đơn:
     * - PENDING: Chờ khách sạn xác nhận
     * - BOOKED: Đã chấp nhận (Chờ ngày check-in)
     * - REJECTED: Đã từ chối (Kèm lý do & mã giảm giá đền bù)
     * - CHECKED_IN: Đang lưu trú (Đã nhận phòng, mở khóa thanh toán)
     * - PAID: Đã thanh toán (Chờ hết hạn ngày để về trống hoặc trả phòng)
     * - CHECKED_OUT: Đã trả phòng & Hoàn tất lưu lịch sử
     * - CANCELLED: Đã hủy
     */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(length = 500)
    private String note;

    // Thông tin từ chối & đền bù mã giảm giá 5-10%
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "compensation_code", length = 50)
    private String compensationCode;

    @Column(name = "compensation_discount")
    private Integer compensationDiscount; // 5 - 10 (%)

    @Column(name = "compensation_used")
    private Boolean compensationUsed = false;

    // Mã giảm giá được khách hàng áp dụng vào đơn đặt phòng này
    @Column(name = "applied_voucher_code", length = 50)
    private String appliedVoucherCode;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    // Lịch sử mốc thời gian chi tiết
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<BookingServiceItem> bookingServices = new ArrayList<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Invoice invoice;
}
