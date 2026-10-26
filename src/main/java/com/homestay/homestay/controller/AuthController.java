package com.homestay.homestay.controller;

import com.homestay.homestay.dto.UserDto;
import com.homestay.homestay.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("title", "Đăng ký tài khoản khách hàng");
        model.addAttribute("userDto", new UserDto());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userDto") UserDto dto,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Đăng ký tài khoản khách hàng");
            return "register";
        }

        dto.setRole("CUSTOMER");
        userService.create(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản thành công! Quý khách có thể đăng nhập ngay.");
        return "redirect:/login";
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/dashboard";
    }
}
