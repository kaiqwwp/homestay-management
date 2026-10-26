package com.homestay.homestay.controller;

import com.homestay.homestay.dto.AddBookingServiceDto;
import com.homestay.homestay.dto.BookingDto;
import com.homestay.homestay.dto.BookingSearchDto;
import com.homestay.homestay.dto.PaymentDto;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.BookingServiceItem;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.service.BookingService;
import com.homestay.homestay.service.CustomerService;
import com.homestay.homestay.service.InvoiceService;
import com.homestay.homestay.service.RoomService;
import com.homestay.homestay.service.ServiceItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final CustomerService customerService;
    private final RoomService roomService;
    private final ServiceItemService serviceItemService;
    private final InvoiceService invoiceService;

    public BookingController(BookingService bookingService,
                             CustomerService customerService,
                             RoomService roomService,
                             ServiceItemService serviceItemService,
                             InvoiceService invoiceService) {
        this.bookingService = bookingService;
        this.customerService = customerService;
        this.roomService = roomService;
        this.serviceItemService = serviceItemService;
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public String list(@ModelAttribute("searchDto") BookingSearchDto searchDto,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {
        Page<Booking> bookingPage = bookingService.filterBookings(searchDto, PageRequest.of(page, size));
        model.addAttribute("title", "Quản lý đặt phòng");
        model.addAttribute("bookingPage", bookingPage);
        model.addAttribute("searchDto", searchDto);
        return "bookings/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(value = "checkIn", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                             @RequestParam(value = "checkOut", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                             Model model) {
        BookingDto dto = new BookingDto();
        if (checkIn == null) {
            checkIn = LocalDate.now();
        }
        if (checkOut == null) {
            checkOut = checkIn.plusDays(1);
        }
        dto.setCheckIn(checkIn);
        dto.setCheckOut(checkOut);

        model.addAttribute("title", "Tạo đơn đặt phòng mới");
        model.addAttribute("bookingDto", dto);
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("availableRooms", roomService.findAvailableRooms(checkIn, checkOut));
        return "bookings/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("bookingDto") BookingDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Tạo đơn đặt phòng mới");
            model.addAttribute("customers", customerService.findAll());
            LocalDate cIn = dto.getCheckIn() != null ? dto.getCheckIn() : LocalDate.now();
            LocalDate cOut = dto.getCheckOut() != null ? dto.getCheckOut() : cIn.plusDays(1);
            model.addAttribute("availableRooms", roomService.findAvailableRooms(cIn, cOut));
            return "bookings/form";
        }

        bookingService.create(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Đặt phòng thành công!");
        return "redirect:/bookings";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Booking booking = bookingService.findById(id);
        List<BookingServiceItem> bookingServices = invoiceService.getBookingServices(id);
        Invoice invoice = invoiceService.findByBookingId(id);

        BigDecimal roomTotal = invoiceService.calculateRoomTotal(booking);
        BigDecimal serviceTotal = invoiceService.calculateServiceTotal(booking);
        BigDecimal grandTotal = roomTotal.add(serviceTotal);

        model.addAttribute("title", "Chi tiết đơn đặt phòng #" + booking.getId());
        model.addAttribute("booking", booking);
        model.addAttribute("bookingServices", bookingServices);
        model.addAttribute("invoice", invoice);
        model.addAttribute("roomTotal", roomTotal);
        model.addAttribute("serviceTotal", serviceTotal);
        model.addAttribute("grandTotal", grandTotal);

        model.addAttribute("services", serviceItemService.findAll());
        model.addAttribute("addServiceDto", new AddBookingServiceDto());
        model.addAttribute("paymentDto", new PaymentDto());

        return "bookings/detail";
    }

    @PostMapping("/{id}/check-in")
    public String checkIn(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        bookingService.checkIn(id);
        redirectAttributes.addFlashAttribute("successMessage", "Khách đã nhận phòng thành công!");
        return "redirect:/bookings/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        bookingService.cancel(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn đặt phòng!");
        return "redirect:/bookings";
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                           @RequestParam(value = "days", defaultValue = "14") int days,
                           Model model) {
        Map<String, Object> calendarData = bookingService.getCalendarData(startDate, days);
        model.addAttribute("title", "Lịch đặt phòng theo ngày");
        model.addAllAttributes(calendarData);
        return "bookings/calendar";
    }
}
