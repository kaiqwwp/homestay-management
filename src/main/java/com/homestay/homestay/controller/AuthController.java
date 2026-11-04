package com.homestay.homestay.controller;

import com.homestay.homestay.dto.UserDto;
import com.homestay.homestay.entity.User;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.security.SecurityUserAdvice;
import com.homestay.homestay.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final SecurityUserAdvice securityUserAdvice;

    public AuthController(UserService userService, SecurityUserAdvice securityUserAdvice) {
        this.userService = userService;
        this.securityUserAdvice = securityUserAdvice;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("title", "Đăng ký tài khoản thành viên");
        model.addAttribute("userDto", new UserDto());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userDto") UserDto dto,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            result.rejectValue("phone", "NotBlank", "Số điện thoại không được để trống");
        }
        if (dto.getIdNumber() == null || dto.getIdNumber().trim().isEmpty()) {
            result.rejectValue("idNumber", "NotBlank", "Số CCCD / Passport không được để trống");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            result.rejectValue("email", "NotBlank", "Email không được để trống");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().length() < 6) {
            result.rejectValue("password", "Size", "Mật khẩu phải có tối thiểu 6 ký tự");
        }

        if (result.hasErrors()) {
            model.addAttribute("title", "Đăng ký tài khoản thành viên");
            return "register";
        }

        try {
            dto.setRole("CUSTOMER");
            userService.create(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng ký thành viên thành công! Thông tin cá nhân (Họ tên, SĐT, CCCD, Email) đã được lưu để sử dụng khi đặt phòng.");
            return "redirect:/login";
        } catch (BusinessException ex) {
            model.addAttribute("title", "Đăng ký tài khoản thành viên");
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/profile")
    public String profilePage(@RequestParam(value = "requireUpdate", required = false) Boolean requireUpdate,
                              Model model) {
        User currentUser = securityUserAdvice.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Thông tin cá nhân");
        model.addAttribute("user", currentUser);
        model.addAttribute("requireUpdate", Boolean.TRUE.equals(requireUpdate) || !currentUser.hasCompleteProfile());
        return "profile/index";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam("phone") String phone,
                                @RequestParam("idNumber") String idNumber,
                                @RequestParam("email") String email,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                RedirectAttributes redirectAttributes) {
        User currentUser = securityUserAdvice.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            userService.updateOwnProfile(currentUser.getId(), fullName, phone, idNumber, email, newPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật thông tin cá nhân thành công! Bạn có thể sử dụng thông tin này để đặt phòng ngay.");
            return "redirect:/profile";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile?requireUpdate=true";
        }
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/dashboard";
    }
}
