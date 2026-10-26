package com.homestay.homestay.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomReportItem {
    private String roomNumber;
    private String roomTypeName;
    private BigDecimal pricePerNight;
    private String status;
}
