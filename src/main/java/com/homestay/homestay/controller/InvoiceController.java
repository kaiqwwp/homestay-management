package com.homestay.homestay.controller;

import com.homestay.homestay.dto.AddBookingServiceDto;
import com.homestay.homestay.dto.PaymentDto;
import com.homestay.homestay.entity.BookingServiceItem;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/invoices")
    public String list(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {
        Page<Invoice> invoicePage = invoiceService.filterInvoices(keyword, PageRequest.of(page, size));
        model.addAttribute("title", "Quản lý hóa đơn");
        model.addAttribute("invoicePage", invoicePage);
        model.addAttribute("keyword", keyword);
        return "invoices/list";
    }

    @GetMapping("/invoices/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Invoice invoice = invoiceService.findById(id);
        List<BookingServiceItem> bookingServices = invoiceService.getBookingServices(invoice.getBooking().getId());

        model.addAttribute("title", "Chi tiết hóa đơn #" + invoice.getId());
        model.addAttribute("invoice", invoice);
        model.addAttribute("bookingServices", bookingServices);
        return "invoices/detail";
    }

    @PostMapping("/invoices/checkout")
    public String checkout(@RequestParam("bookingId") Long bookingId,
                           @ModelAttribute("paymentDto") PaymentDto paymentDto,
                           RedirectAttributes redirectAttributes) {
        Invoice invoice = invoiceService.checkOutAndCreateInvoice(bookingId, paymentDto.getPaymentMethod());
        redirectAttributes.addFlashAttribute("successMessage", "Trả phòng và thanh toán hóa đơn #" + invoice.getId() + " thành công!");
        return "redirect:/invoices/" + invoice.getId();
    }

    @PostMapping("/bookings/{bookingId}/services")
    public String addService(@PathVariable("bookingId") Long bookingId,
                             @Valid @ModelAttribute("addServiceDto") AddBookingServiceDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn dịch vụ và nhập số lượng hợp lệ.");
            return "redirect:/bookings/" + bookingId;
        }

        invoiceService.addServiceToBooking(bookingId, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm dịch vụ vào đơn đặt phòng thành công!");
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/services/{id}/delete")
    public String removeService(@PathVariable("id") Long id,
                                @RequestParam("bookingId") Long bookingId,
                                RedirectAttributes redirectAttributes) {
        invoiceService.removeServiceFromBooking(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã hủy dịch vụ khỏi đơn đặt phòng!");
        return "redirect:/bookings/" + bookingId;
    }
}
