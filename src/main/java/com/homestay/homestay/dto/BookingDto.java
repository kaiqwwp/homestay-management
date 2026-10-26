package com.homestay.homestay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class BookingDto {
    private Long id;

    @NotNull(message = "Vui lòng chọn khách hàng")
    private Long customerId;

    @NotNull(message = "Vui lòng chọn phòng")
    private Long roomId;

    @NotNull(message = "Vui lòng chọn ngày nhận phòng")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull(message = "Vui lòng chọn ngày trả phòng")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    private String status = "BOOKED";

    private String note;
}
