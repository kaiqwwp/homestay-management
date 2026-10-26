package com.homestay.homestay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentDto {
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod = "Tiền mặt";
}
