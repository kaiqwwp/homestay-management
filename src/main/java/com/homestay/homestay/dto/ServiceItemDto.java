package com.homestay.homestay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceItemDto {
    private Long id;

    @NotBlank(message = "Tên dịch vụ không được để trống")
    private String name;

    @NotNull(message = "Đơn giá không được để trống")
    @Min(value = 0, message = "Đơn giá phải lớn hơn hoặc bằng 0")
    private BigDecimal unitPrice;

    @NotBlank(message = "Đơn vị tính không được để trống")
    private String unit;
}
