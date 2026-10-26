package com.homestay.homestay.config;

import com.homestay.homestay.entity.User;
import com.homestay.homestay.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDefaultUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("Quản trị viên Hệ thống");
                admin.setRole("ADMIN");
                userRepository.save(admin);
            }

            if (!userRepository.existsByUsername("staff")) {
                User staff = new User();
                staff.setUsername("staff");
                staff.setPassword(passwordEncoder.encode("staff123"));
                staff.setFullName("Nhân viên Lễ tân");
                staff.setRole("STAFF");
                userRepository.save(staff);
            }
        };
    }
}
