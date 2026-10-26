package com.homestay.homestay.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportItem {
    private String month;
    private Long totalInvoices;
    private BigDecimal roomRevenue;
    private BigDecimal serviceRevenue;
    private BigDecimal totalRevenue;
}
