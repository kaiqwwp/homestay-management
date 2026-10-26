package com.homestay.homestay.controller;

import com.homestay.homestay.dto.RoomTypeDto;
import com.homestay.homestay.entity.RoomType;
import com.homestay.homestay.service.RoomTypeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("title", "Danh sách loại phòng");
        model.addAttribute("roomTypes", roomTypeService.findAll());
        return "room_types/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("title", "Thêm loại phòng mới");
        model.addAttribute("roomTypeDto", new RoomTypeDto());
        return "room_types/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("roomTypeDto") RoomTypeDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Thêm loại phòng mới");
            return "room_types/form";
        }
        roomTypeService.create(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm loại phòng thành công!");
        return "redirect:/room-types";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        RoomType roomType = roomTypeService.findById(id);
        RoomTypeDto dto = new RoomTypeDto();
        dto.setId(roomType.getId());
        dto.setName(roomType.getName());
        dto.setPricePerNight(roomType.getPricePerNight());
        dto.setCapacity(roomType.getCapacity());
        dto.setDescription(roomType.getDescription());

        model.addAttribute("title", "Chỉnh sửa loại phòng");
        model.addAttribute("roomTypeDto", dto);
        return "room_types/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("roomTypeDto") RoomTypeDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Chỉnh sửa loại phòng");
            return "room_types/form";
        }
        roomTypeService.update(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật loại phòng thành công!");
        return "redirect:/room-types";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        roomTypeService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa loại phòng thành công!");
        return "redirect:/room-types";
    }
}
