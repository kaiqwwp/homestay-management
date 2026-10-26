package com.homestay.homestay.controller;

import com.homestay.homestay.dto.ServiceItemDto;
import com.homestay.homestay.entity.ServiceItem;
import com.homestay.homestay.service.ServiceItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/services")
public class ServiceItemController {

    private final ServiceItemService serviceItemService;

    public ServiceItemController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {
        Page<ServiceItem> servicePage = serviceItemService.searchServices(keyword, PageRequest.of(page, size));
        model.addAttribute("title", "Quản lý dịch vụ");
        model.addAttribute("servicePage", servicePage);
        model.addAttribute("keyword", keyword);
        return "services/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("title", "Thêm dịch vụ mới");
        model.addAttribute("serviceItemDto", new ServiceItemDto());
        return "services/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("serviceItemDto") ServiceItemDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Thêm dịch vụ mới");
            return "services/form";
        }
        serviceItemService.create(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm dịch vụ mới thành công!");
        return "redirect:/services";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        ServiceItem item = serviceItemService.findById(id);
        ServiceItemDto dto = new ServiceItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setUnit(item.getUnit());

        model.addAttribute("title", "Chỉnh sửa dịch vụ");
        model.addAttribute("serviceItemDto", dto);
        return "services/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("serviceItemDto") ServiceItemDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Chỉnh sửa dịch vụ");
            return "services/form";
        }
        serviceItemService.update(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật dịch vụ thành công!");
        return "redirect:/services";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        serviceItemService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa dịch vụ thành công!");
        return "redirect:/services";
    }
}
