package com.homestay.homestay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class BookingDto {
    private Long id;

    // Optional: dùng cho nội bộ hoặc unit test; trên giao diện khách hàng/lễ tân sẽ tự tạo/cập nhật theo thông tin cá nhân bên dưới
    private Long customerId;

    private String customerFullName;

    private String customerPhone;

    private String customerEmail;

    private String customerIdNumber;

    @NotNull(message = "Vui lòng chọn phòng")
    private Long roomId;

    @NotNull(message = "Vui lòng chọn ngày nhận phòng")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull(message = "Vui lòng chọn ngày trả phòng")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    private String status = "PENDING";

    private String note;

    // Mã giảm giá đền bù khách hàng áp dụng khi đặt phòng
    private String voucherCode;
}
