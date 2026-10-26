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

    @NotBlank(message = "Vai trò không được để trống")
    private String role; // ADMIN, STAFF
}
