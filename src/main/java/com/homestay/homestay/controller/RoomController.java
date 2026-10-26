package com.homestay.homestay.controller;

import com.homestay.homestay.dto.RoomDto;
import com.homestay.homestay.entity.Room;
import com.homestay.homestay.service.RoomService;
import com.homestay.homestay.service.RoomTypeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public RoomController(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "roomTypeId", required = false) Long roomTypeId,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "8") int size,
                       Model model) {
        Page<Room> roomPage = roomService.filterRooms(status, roomTypeId, keyword, PageRequest.of(page, size));

        model.addAttribute("title", "Quản lý phòng");
        model.addAttribute("roomPage", roomPage);
        model.addAttribute("status", status);
        model.addAttribute("roomTypeId", roomTypeId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("roomTypes", roomTypeService.findAll());

        return "rooms/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("title", "Thêm phòng mới");
        model.addAttribute("roomDto", new RoomDto());
        model.addAttribute("roomTypes", roomTypeService.findAll());
        return "rooms/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("roomDto") RoomDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Thêm phòng mới");
            model.addAttribute("roomTypes", roomTypeService.findAll());
            return "rooms/form";
        }
        roomService.create(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm phòng mới thành công!");
        return "redirect:/rooms";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        Room room = roomService.findById(id);
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setRoomTypeId(room.getRoomType().getId());
        dto.setStatus(room.getStatus());
        dto.setImageUrl(room.getImageUrl());

        model.addAttribute("title", "Chỉnh sửa phòng " + room.getRoomNumber());
        model.addAttribute("roomDto", dto);
        model.addAttribute("roomTypes", roomTypeService.findAll());
        return "rooms/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("roomDto") RoomDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Chỉnh sửa phòng");
            model.addAttribute("roomTypes", roomTypeService.findAll());
            return "rooms/form";
        }
        roomService.update(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin phòng thành công!");
        return "redirect:/rooms";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        roomService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa phòng thành công!");
        return "redirect:/rooms";
    }

    @GetMapping("/api/available")
    @ResponseBody
    public ResponseEntity<List<Room>> getAvailableRooms(
            @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(roomService.findAvailableRooms(checkIn, checkOut));
    }
}
