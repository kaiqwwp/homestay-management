package com.homestay.homestay.security;

import com.homestay.homestay.entity.User;
import com.homestay.homestay.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
@Component("authHelper")
public class SecurityUserAdvice {

    private final UserRepository userRepository;

    public SecurityUserAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @ModelAttribute("currentUserRole")
    public String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "ANONYMOUS";
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if ("ROLE_ADMIN".equals(a)) return "ADMIN";
            if ("ROLE_STAFF".equals(a)) return "STAFF";
            if ("ROLE_CUSTOMER".equals(a)) return "CUSTOMER";
        }
        return "CUSTOMER";
    }

    @ModelAttribute("userRoleLabel")
    public String getUserRoleLabel() {
        String role = getCurrentUserRole();
        if ("ADMIN".equals(role)) return "Quản trị viên (ADMIN)";
        if ("STAFF".equals(role)) return "Lễ tân (STAFF)";
        if ("CUSTOMER".equals(role)) return "Khách hàng (CUSTOMER)";
        return "Khách";
    }

    @ModelAttribute("isAdmin")
    public Boolean isAdmin() {
        return Boolean.valueOf("ADMIN".equals(getCurrentUserRole()));
    }

    @ModelAttribute("isStaff")
    public Boolean isStaff() {
        return Boolean.valueOf("STAFF".equals(getCurrentUserRole()));
    }

    @ModelAttribute("isStaffOrAdmin")
    public Boolean isStaffOrAdmin() {
        String role = getCurrentUserRole();
        return Boolean.valueOf("ADMIN".equals(role) || "STAFF".equals(role));
    }

    @ModelAttribute("isCustomer")
    public Boolean isCustomer() {
        return Boolean.valueOf("CUSTOMER".equals(getCurrentUserRole()));
    }
}
