package com.homestay.homestay.controller;

import com.homestay.homestay.entity.User;
import com.homestay.homestay.security.SecurityUserAdvice;
import com.homestay.homestay.service.BookingService;
import com.homestay.homestay.service.DashboardService;
import com.homestay.homestay.service.RoomService;
import com.homestay.homestay.service.ServiceItemService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final RoomService roomService;
    private final ServiceItemService serviceItemService;
    private final BookingService bookingService;
    private final SecurityUserAdvice securityUserAdvice;

    public DashboardController(DashboardService dashboardService,
                               RoomService roomService,
                               ServiceItemService serviceItemService,
                               BookingService bookingService,
                               SecurityUserAdvice securityUserAdvice) {
        this.dashboardService = dashboardService;
        this.roomService = roomService;
        this.serviceItemService = serviceItemService;
        this.bookingService = bookingService;
        this.securityUserAdvice = securityUserAdvice;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = dashboardService.getDashboardStats();
        model.addAttribute("title", "Homestay Pro - Nghỉ dưỡng & Đặt phòng");
        model.addAttribute("stats", stats);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1));
        model.addAttribute("featuredRooms", roomService.filterRoomCards(null, null, null, PageRequest.of(0, 12)).getContent());
        model.addAttribute("services", serviceItemService.findAll());

        User currentUser = securityUserAdvice.getCurrentUser();
        if (currentUser != null && Boolean.TRUE.equals(securityUserAdvice.isCustomer())) {
            model.addAttribute("availableVouchers", bookingService.findAvailableVouchersByEmail(currentUser.getEmail()));
        } else {
            model.addAttribute("availableVouchers", Collections.emptyList());
        }
        return "dashboard/index";
    }
}
