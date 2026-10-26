package com.homestay.homestay.controller;

import com.homestay.homestay.dto.CustomerDto;
import com.homestay.homestay.entity.Customer;
import com.homestay.homestay.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {
        Page<Customer> customerPage = customerService.searchCustomers(keyword, PageRequest.of(page, size));
        model.addAttribute("title", "Quản lý khách hàng");
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("keyword", keyword);
        return "customers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("title", "Thêm khách hàng mới");
        model.addAttribute("customerDto", new CustomerDto());
        return "customers/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("customerDto") CustomerDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Thêm khách hàng mới");
            return "customers/form";
        }
        customerService.create(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm khách hàng thành công!");
        return "redirect:/customers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        Customer customer = customerService.findById(id);
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setFullName(customer.getFullName());
        dto.setPhone(customer.getPhone());
        dto.setEmail(customer.getEmail());
        dto.setIdNumber(customer.getIdNumber());

        model.addAttribute("title", "Chỉnh sửa khách hàng");
        model.addAttribute("customerDto", dto);
        return "customers/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("customerDto") CustomerDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Chỉnh sửa khách hàng");
            return "customers/form";
        }
        customerService.update(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khách hàng thành công!");
        return "redirect:/customers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        customerService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa khách hàng thành công!");
        return "redirect:/customers";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<List<Customer>> searchApi(@RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(customerService.searchByKeyword(keyword));
    }
}
