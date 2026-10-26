package com.homestay.homestay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomTypeDto {
    private Long id;

    @NotBlank(message = "Tên loại phòng không được để trống")
    private String name;

    @NotNull(message = "Giá mỗi đêm không được để trống")
    @Min(value = 0, message = "Giá mỗi đêm phải lớn hơn hoặc bằng 0")
    private BigDecimal pricePerNight;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu là 1 người")
    private Integer capacity;

    private String description;
}
