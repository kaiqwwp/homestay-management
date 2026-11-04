package com.homestay.homestay.controller;

import com.homestay.homestay.dto.AddBookingServiceDto;
import com.homestay.homestay.dto.BookingDto;
import com.homestay.homestay.dto.BookingSearchDto;
import com.homestay.homestay.dto.PaymentDto;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.BookingServiceItem;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.entity.User;
import com.homestay.homestay.repository.CustomerRepository;
import com.homestay.homestay.security.SecurityUserAdvice;
import com.homestay.homestay.service.BookingService;
import com.homestay.homestay.service.InvoiceService;
import com.homestay.homestay.service.RoomService;
import com.homestay.homestay.service.SepayPaymentService;
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
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final CustomerRepository customerRepository;
    private final RoomService roomService;
    private final ServiceItemService serviceItemService;
    private final InvoiceService invoiceService;
    private final SepayPaymentService sepayPaymentService;
    private final SecurityUserAdvice securityUserAdvice;

    public BookingController(BookingService bookingService,
                             CustomerRepository customerRepository,
                             RoomService roomService,
                             ServiceItemService serviceItemService,
                             InvoiceService invoiceService,
                             SepayPaymentService sepayPaymentService,
                             SecurityUserAdvice securityUserAdvice) {
        this.bookingService = bookingService;
        this.customerRepository = customerRepository;
        this.roomService = roomService;
        this.serviceItemService = serviceItemService;
        this.invoiceService = invoiceService;
        this.sepayPaymentService = sepayPaymentService;
        this.securityUserAdvice = securityUserAdvice;
    }

    @GetMapping
    public String list(@ModelAttribute("searchDto") BookingSearchDto searchDto,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {
        Page<Booking> bookingPage;
        if (Boolean.TRUE.equals(securityUserAdvice.isStaffOrAdmin())) {
            bookingPage = bookingService.filterBookings(searchDto, PageRequest.of(page, size));
            model.addAttribute("title", "Quản lý & Lịch sử Đặt phòng");
            model.addAttribute("availableVouchers", Collections.emptyList());
        } else {
            User currentUser = securityUserAdvice.getCurrentUser();
            String email = (currentUser != null) ? currentUser.getEmail() : null;
            bookingPage = bookingService.filterBookingsByCustomerEmail(email, searchDto, PageRequest.of(page, size));
            model.addAttribute("title", "Lịch sử phòng đã đặt của tôi");
            model.addAttribute("availableVouchers", bookingService.findAvailableVouchersByEmail(email));
        }
        model.addAttribute("bookingPage", bookingPage);
        model.addAttribute("searchDto", searchDto);
        return "bookings/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(value = "checkIn", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                             @RequestParam(value = "checkOut", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                             @RequestParam(value = "roomId", required = false) Long roomId,
                             @RequestParam(value = "voucherCode", required = false) String voucherCode,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        User currentUser = securityUserAdvice.getCurrentUser();
        if (currentUser != null && Boolean.TRUE.equals(securityUserAdvice.isCustomer()) && !currentUser.hasCompleteProfile()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Vui lòng cập nhật đầy đủ Số điện thoại và Số CCCD / Passport trong Thông tin cá nhân trước khi đặt phòng!");
            return "redirect:/profile?requireUpdate=true";
        }

        LocalDate today = LocalDate.now();
        BookingDto dto = new BookingDto();
        if (checkIn == null || checkIn.isBefore(today)) {
            checkIn = today;
        }
        if (checkOut == null || !checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(1);
        }
        dto.setCheckIn(checkIn);
        dto.setCheckOut(checkOut);
        if (roomId != null) {
            dto.setRoomId(roomId);
        }
        if (voucherCode != null && !voucherCode.isBlank()) {
            dto.setVoucherCode(voucherCode.trim());
        }

        if (currentUser != null && Boolean.TRUE.equals(securityUserAdvice.isCustomer())) {
            dto.setCustomerFullName(currentUser.getFullName());
            dto.setCustomerPhone(currentUser.getPhone());
            dto.setCustomerIdNumber(currentUser.getIdNumber());
            dto.setCustomerEmail(currentUser.getEmail());
            model.addAttribute("availableVouchers", bookingService.findAvailableVouchersByEmail(currentUser.getEmail()));
        } else {
            model.addAttribute("availableVouchers", Collections.emptyList());
        }

        model.addAttribute("title", "Tạo đơn đặt phòng mới");
        model.addAttribute("bookingDto", dto);
        model.addAttribute("today", today);
        model.addAttribute("minCheckOut", checkIn.plusDays(1));
        model.addAttribute("availableRooms", roomService.findAvailableRooms(checkIn, checkOut));
        return "bookings/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("bookingDto") BookingDto dto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        User currentUser = securityUserAdvice.getCurrentUser();
        if (currentUser != null && Boolean.TRUE.equals(securityUserAdvice.isCustomer())) {
            if (!currentUser.hasCompleteProfile()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Vui lòng cập nhật đầy đủ Số điện thoại và Số CCCD / Passport trước khi đặt phòng!");
                return "redirect:/profile?requireUpdate=true";
            }
            dto.setCustomerFullName(currentUser.getFullName());
            dto.setCustomerPhone(currentUser.getPhone());
            dto.setCustomerIdNumber(currentUser.getIdNumber());
            dto.setCustomerEmail(currentUser.getEmail());
        }

        if (dto.getCustomerFullName() == null || dto.getCustomerFullName().trim().isEmpty()) {
            result.rejectValue("customerFullName", "NotBlank", "Vui lòng nhập họ và tên người đặt phòng");
        }
        if (dto.getCustomerPhone() == null || dto.getCustomerPhone().trim().isEmpty()) {
            result.rejectValue("customerPhone", "NotBlank", "Vui lòng nhập số điện thoại liên hệ");
        }

        LocalDate today = LocalDate.now();
        if (dto.getCheckIn() == null) {
            result.rejectValue("checkIn", "NotNull", "Vui lòng chọn ngày nhận phòng");
        } else if (dto.getCheckIn().isBefore(today)) {
            result.rejectValue("checkIn", "PastDate", "Không được phép chọn ngày đặt (nhận phòng) là ngày trong quá khứ!");
        }

        if (dto.getCheckOut() == null) {
            result.rejectValue("checkOut", "NotNull", "Vui lòng chọn ngày trả phòng");
        } else if (dto.getCheckOut().isBefore(today.plusDays(1))) {
            result.rejectValue("checkOut", "PastDate", "Không được phép chọn ngày trả phòng là ngày trong quá khứ!");
        } else if (dto.getCheckIn() != null && !dto.getCheckOut().isAfter(dto.getCheckIn())) {
            result.rejectValue("checkOut", "InvalidRange", "Ngày trả phòng phải lớn hơn ngày đặt (nhận phòng) ít nhất 1 ngày!");
        }

        if (result.hasErrors()) {
            model.addAttribute("title", "Tạo đơn đặt phòng mới");
            LocalDate cIn = (dto.getCheckIn() != null && !dto.getCheckIn().isBefore(today)) ? dto.getCheckIn() : today;
            LocalDate cOut = (dto.getCheckOut() != null && dto.getCheckOut().isAfter(cIn)) ? dto.getCheckOut() : cIn.plusDays(1);
            model.addAttribute("today", today);
            model.addAttribute("minCheckOut", cIn.plusDays(1));
            model.addAttribute("availableRooms", roomService.findAvailableRooms(cIn, cOut));
            if (currentUser != null && Boolean.TRUE.equals(securityUserAdvice.isCustomer())) {
                model.addAttribute("availableVouchers", bookingService.findAvailableVouchersByEmail(currentUser.getEmail()));
            }
            return "bookings/form";
        }

        Booking created = bookingService.create(dto);
        String checkInFormatted = created.getCheckIn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String checkOutFormatted = created.getCheckOut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã gửi yêu cầu đặt phòng (Mã đơn #" + created.getId() + " - Phòng " + created.getRoom().getRoomNumber() + ", từ " + checkInFormatted + " đến " + checkOutFormatted + ")! " +
                "Yêu cầu của quý khách đang chờ khách sạn xác nhận.");
        return "redirect:/bookings";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Booking booking = bookingService.findById(id);
        List<BookingServiceItem> bookingServices = invoiceService.getBookingServices(id);
        Invoice invoice = invoiceService.findByBookingId(id);

        BigDecimal roomTotal = invoiceService.calculateRoomTotal(booking);
        BigDecimal serviceTotal = invoiceService.calculateServiceTotal(booking);
        BigDecimal discountAmount = invoiceService.calculateDiscountAmount(booking, roomTotal, serviceTotal);
        BigDecimal grandTotal = roomTotal.add(serviceTotal).subtract(discountAmount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }

        model.addAttribute("title", "Chi tiết đơn đặt phòng #" + booking.getId());
        model.addAttribute("booking", booking);
        model.addAttribute("bookingServices", bookingServices);
        model.addAttribute("invoice", invoice);
        model.addAttribute("roomTotal", roomTotal);
        model.addAttribute("serviceTotal", serviceTotal);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("grandTotal", grandTotal);

        model.addAttribute("services", serviceItemService.findAll());
        model.addAttribute("addServiceDto", new AddBookingServiceDto());
        model.addAttribute("paymentDto", new PaymentDto());

        // Thông tin thanh toán Online qua QR SePay MB Bank
        model.addAttribute("sepayQrUrl", sepayPaymentService.generateVietQrUrl(booking.getId(), grandTotal));
        model.addAttribute("sepayBankName", sepayPaymentService.getBankName());
        model.addAttribute("sepayBankAccount", sepayPaymentService.getBankAccount());
        model.addAttribute("sepayAccountName", sepayPaymentService.getAccountName());
        model.addAttribute("sepayTransferContent", sepayPaymentService.getTransferContent(booking.getId()));

        return "bookings/detail";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Booking approved = bookingService.approve(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã CHẤP NHẬN yêu cầu đặt phòng #" + approved.getId() + " (Phòng " + approved.getRoom().getRoomNumber() + ")! Đã gửi thông báo đặt phòng thành công cho khách hàng.");
        return "redirect:/bookings/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id,
                         @RequestParam(value = "reason", required = false) String reason,
                         @RequestParam(value = "discountPercent", defaultValue = "10") Integer discountPercent,
                         RedirectAttributes redirectAttributes) {
        Booking rejected = bookingService.reject(id, reason, discountPercent);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã TỪ CHỐI đơn #" + rejected.getId() + " và gửi thông báo kèm Mã giảm giá đền bù " +
                rejected.getCompensationCode() + " (-" + rejected.getCompensationDiscount() + "%) cho khách hàng!");
        return "redirect:/bookings/" + id;
    }

    @PostMapping("/{id}/check-in")
    public String checkIn(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        bookingService.checkIn(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã xác nhận khách nhận phòng (Checked-in) thành công! Mục Thanh toán hiện đã được mở khóa.");
        return "redirect:/bookings/" + id;
    }

    @PostMapping("/{id}/release-room")
    public String releaseRoom(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Booking released = bookingService.completeCheckoutAndReleaseRoom(id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã hoàn tất trả phòng và chuyển Phòng " + released.getRoom().getRoomNumber() + " về trạng thái TRỐNG (AVAILABLE)! Lịch sử đã được lưu chi tiết.");
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
