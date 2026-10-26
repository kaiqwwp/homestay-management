package com.homestay.homestay.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyReportItem {
    private String roomTypeName;
    private Long totalRooms;
    private Long occupiedRooms;
    private String occupancyRate;
}
