package com.homestay.homestay.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceReportItem {
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
}
