package com.homestay.homestay.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "id_number", length = 30)
    private String idNumber;

    @Column(nullable = false, length = 20)
    private String role = "CUSTOMER"; // ADMIN, STAFF, CUSTOMER

    @Column(name = "auth_provider", length = 20)
    private String authProvider = "LOCAL"; // LOCAL, GOOGLE

    public boolean hasCompleteProfile() {
        return fullName != null && !fullName.trim().isEmpty()
                && phone != null && !phone.trim().isEmpty()
                && idNumber != null && !idNumber.trim().isEmpty()
                && email != null && !email.trim().isEmpty();
    }
}
