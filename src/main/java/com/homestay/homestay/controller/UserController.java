package com.homestay.homestay.controller;

import com.homestay.homestay.dto.UserDto;
import com.homestay.homestay.entity.User;
import com.homestay.homestay.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {
        Page<User> userPage = userService.searchUsers(keyword, PageRequest.of(page, size));
        model.addAttribute("title", "Quản lý tài khoản người dùng");
        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("title", "Tạo tài khoản mới");
        model.addAttribute("userDto", new UserDto());
        return "users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userDto") UserDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Tạo tài khoản mới");
            return "users/form";
        }
        userService.create(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công!");
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        User user = userService.findById(id);
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());

        model.addAttribute("title", "Chỉnh sửa tài khoản");
        model.addAttribute("userDto", dto);
        return "users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("userDto") UserDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Chỉnh sửa tài khoản");
            return "users/form";
        }
        userService.update(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công!");
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản thành công!");
        return "redirect:/users";
    }
}
