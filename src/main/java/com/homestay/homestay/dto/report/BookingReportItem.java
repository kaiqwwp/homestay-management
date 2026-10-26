package com.homestay.homestay.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingReportItem {
    private Long id;
    private String customerName;
    private String phone;
    private String roomNumber;
    private String checkIn;
    private String checkOut;
    private String status;
}
