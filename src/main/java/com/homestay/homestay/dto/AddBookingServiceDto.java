package com.homestay.homestay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddBookingServiceDto {
    @NotNull(message = "Vui lòng chọn dịch vụ")
    private Long serviceId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private Integer quantity = 1;
}
