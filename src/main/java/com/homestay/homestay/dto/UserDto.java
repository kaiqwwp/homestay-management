package com.homestay.homestay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDto {
    private Long id;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    private String email;

    private String phone;

    private String idNumber;

    private String role = "CUSTOMER"; // ADMIN, STAFF, CUSTOMER
}
