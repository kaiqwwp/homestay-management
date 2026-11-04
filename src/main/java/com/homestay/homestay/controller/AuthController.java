package com.homestay.homestay.controller;

import com.homestay.homestay.dto.UserDto;
import com.homestay.homestay.entity.User;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.repository.UserRepository;
import com.homestay.homestay.security.SecurityUserAdvice;
import com.homestay.homestay.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.UUID;

@Controller
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUserAdvice securityUserAdvice;

    public AuthController(UserService userService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          SecurityUserAdvice securityUserAdvice) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    @GetMapping("/oauth2/authorization/google")
    public String googleChooserPage() {
        return "google-login";
    }

    @PostMapping("/oauth2/google-callback")
    public String handleGoogleLogin(@RequestParam("email") String email,
                                    @RequestParam(value = "fullName", required = false) String fullName,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập địa chỉ Gmail hợp lệ!");
            return "redirect:/oauth2/authorization/google";
        }

        // Mặc định tên là tên của Gmail (hoặc phần trước @ của email nếu không nhập)
        String defaultGoogleName = (fullName != null && !fullName.trim().isEmpty())
                ? fullName.trim()
                : cleanEmail.substring(0, cleanEmail.indexOf('@'));

        User user = userRepository.findByEmail(cleanEmail)
                .orElseGet(() -> userRepository.findByUsername(cleanEmail).orElse(null));

        if (user == null) {
            user = new User();
            user.setUsername(cleanEmail);
            user.setEmail(cleanEmail);
            user.setFullName(defaultGoogleName);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole("CUSTOMER");
            user.setAuthProvider("GOOGLE");
            user = userRepository.save(user);
        }

        userService.syncOrCreateCustomerForUser(user, user.getEmail(), user.getPhone());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(auth);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        if (!user.hasCompleteProfile()) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đăng nhập bằng Google (" + cleanEmail + ") thành công! Vui lòng cập nhật Số CCCD và Số điện thoại để có thể đặt phòng.");
            return "redirect:/profile?requireUpdate=true";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/dashboard";
    }
}
