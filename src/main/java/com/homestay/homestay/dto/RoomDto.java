package com.homestay.homestay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RoomDto {
    private Long id;

    @NotBlank(message = "Số phòng không được để trống")
    private String roomNumber;

    @NotNull(message = "Vui lòng chọn loại phòng")
    private Long roomTypeId;

    private String roomTypeName;

    private String status = "AVAILABLE"; // AVAILABLE, OCCUPIED, MAINTENANCE

    private String imageUrl;

    private MultipartFile imageFile;
}
