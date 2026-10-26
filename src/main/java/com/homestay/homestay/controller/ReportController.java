package com.homestay.homestay.controller;

import com.homestay.homestay.dto.report.*;
import com.homestay.homestay.entity.*;
import com.homestay.homestay.repository.*;
import com.homestay.homestay.service.InvoiceService;
import com.homestay.homestay.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final RoomTypeRepository roomTypeRepo;
    private final RoomRepository roomRepo;
    private final BookingRepository bookingRepo;
    private final InvoiceRepository invoiceRepo;
    private final ServiceItemRepository serviceRepo;
    private final InvoiceService invoiceService;

    public ReportController(ReportService reportService,
                            RoomTypeRepository roomTypeRepo,
                            RoomRepository roomRepo,
                            BookingRepository bookingRepo,
                            InvoiceRepository invoiceRepo,
                            ServiceItemRepository serviceRepo,
                            InvoiceService invoiceService) {
        this.reportService = reportService;
        this.roomTypeRepo = roomTypeRepo;
        this.roomRepo = roomRepo;
        this.bookingRepo = bookingRepo;
        this.invoiceRepo = invoiceRepo;
        this.serviceRepo = serviceRepo;
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("title", "Trung tâm báo cáo Jasper");
        return "reports/index";
    }

    @GetMapping("/room-types.pdf")
    public ResponseEntity<byte[]> roomTypes() {
        byte[] pdf = reportService.exportPdf(
                "room_types.jrxml",
                Map.of("title", "DANH SÁCH LOẠI PHÒNG"),
                roomTypeRepo.findAll());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=room-types.pdf")
                .body(pdf);
    }

    @GetMapping("/rooms.pdf")
    public ResponseEntity<byte[]> roomsReport() {
        List<Room> rooms = roomRepo.findAll();
        List<RoomReportItem> data = rooms.stream()
                .map(r -> new RoomReportItem(
                        r.getRoomNumber(),
                        r.getRoomType().getName(),
                        r.getRoomType().getPricePerNight(),
                        r.getStatus()
                ))
                .collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("title", "BÁO CÁO DANH SÁCH PHÒNG");
        params.put("generatedDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        byte[] pdf = reportService.exportPdf("rooms.jrxml", params, data);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=rooms-report.pdf")
                .body(pdf);
    }

    @GetMapping("/occupancy.pdf")
    public ResponseEntity<byte[]> occupancyReport() {
        List<RoomType> types = roomTypeRepo.findAll();
        List<Room> allRooms = roomRepo.findAll();

        List<OccupancyReportItem> data = new ArrayList<>();
        for (RoomType rt : types) {
            long total = allRooms.stream().filter(r -> r.getRoomType().getId().equals(rt.getId())).count();
            long occupied = allRooms.stream().filter(r -> r.getRoomType().getId().equals(rt.getId()) && "OCCUPIED".equals(r.getStatus())).count();
            double rate = (total > 0) ? ((double) occupied / total * 100) : 0.0;
            String rateStr = String.format("%.1f%%", rate);

            data.add(new OccupancyReportItem(rt.getName(), total, occupied, rateStr));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("title", "BÁO CÁO CÔNG SUẤT PHÒNG");
        params.put("generatedDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        byte[] pdf = reportService.exportPdf("room_occupancy.jrxml", params, data);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=occupancy-report.pdf")
                .body(pdf);
    }

    @GetMapping("/bookings.pdf")
    public ResponseEntity<byte[]> bookingsReport() {
        List<Booking> bookings = bookingRepo.findAll();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        List<BookingReportItem> data = bookings.stream()
                .map(b -> new BookingReportItem(
                        b.getId(),
                        b.getCustomer().getFullName(),
                        b.getCustomer().getPhone(),
                        b.getRoom().getRoomNumber(),
                        b.getCheckIn().format(df),
                        b.getCheckOut().format(df),
                        b.getStatus()
                ))
                .collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("title", "BÁO CÁO DANH SÁCH ĐẶT PHÒNG");
        params.put("generatedDate", LocalDate.now().format(df));

        byte[] pdf = reportService.exportPdf("bookings.jrxml", params, data);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=bookings-report.pdf")
                .body(pdf);
    }

    @GetMapping("/invoices/{id}.pdf")
    public ResponseEntity<byte[]> invoiceReport(@PathVariable("id") Long id) {
        Invoice invoice = invoiceService.findById(id);
        Booking booking = invoice.getBooking();
        List<BookingServiceItem> bookingServices = invoiceService.getBookingServices(booking.getId());

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DecimalFormat currencyFormat = new DecimalFormat("#,##0 VNĐ");

        List<InvoiceReportItem> data = new ArrayList<>();
        // Dòng tiền phòng
        data.add(new InvoiceReportItem(
                "Tiền thuê phòng " + booking.getRoom().getRoomNumber() + " (" + booking.getRoom().getRoomType().getName() + ")",
                1,
                invoice.getRoomTotal(),
                invoice.getRoomTotal()
        ));

        // Các dịch vụ phát sinh
        for (BookingServiceItem bs : bookingServices) {
            BigDecimal total = bs.getUnitPrice().multiply(BigDecimal.valueOf(bs.getQuantity()));
            data.add(new InvoiceReportItem(
                    bs.getService().getName(),
                    bs.getQuantity(),
                    bs.getUnitPrice(),
                    total
            ));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("title", "HÓA ĐƠN THANH TOÁN DỊCH VỤ");
        params.put("invoiceId", String.valueOf(invoice.getId()));
        params.put("customerName", booking.getCustomer().getFullName());
        params.put("phone", booking.getCustomer().getPhone() != null ? booking.getCustomer().getPhone() : "-");
        params.put("roomNumber", booking.getRoom().getRoomNumber());
        params.put("dates", booking.getCheckIn().format(df) + " - " + booking.getCheckOut().format(df));
        params.put("roomTotal", currencyFormat.format(invoice.getRoomTotal()));
        params.put("serviceTotal", currencyFormat.format(invoice.getServiceTotal()));
        params.put("grandTotal", currencyFormat.format(invoice.getTotal()));
        params.put("paymentMethod", invoice.getPaymentMethod() != null ? invoice.getPaymentMethod() : "Tiền mặt");
        params.put("paidAt", invoice.getPaidAt() != null ? invoice.getPaidAt().format(dtf) : "-");

        byte[] pdf = reportService.exportPdf("invoice_detail.jrxml", params, data);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice-" + id + ".pdf")
                .body(pdf);
    }

    @GetMapping("/revenue.pdf")
    public ResponseEntity<byte[]> monthlyRevenueReport() {
        List<Invoice> invoices = invoiceRepo.findAll();
        DecimalFormat currencyFormat = new DecimalFormat("#,##0 VNĐ");

        // Nhóm doanh thu theo Tháng/Năm
        Map<String, List<Invoice>> byMonth = invoices.stream()
                .filter(i -> i.getPaidAt() != null)
                .collect(Collectors.groupingBy(i -> i.getPaidAt().format(DateTimeFormatter.ofPattern("MM/yyyy"))));

        List<RevenueReportItem> data = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<String, List<Invoice>> entry : byMonth.entrySet()) {
            String month = entry.getKey();
            long count = entry.getValue().size();
            BigDecimal roomRev = entry.getValue().stream().map(Invoice::getRoomTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal serviceRev = entry.getValue().stream().map(Invoice::getServiceTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalRev = entry.getValue().stream().map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            grandTotal = grandTotal.add(totalRev);

            data.add(new RevenueReportItem(month, count, roomRev, serviceRev, totalRev));
        }

        if (data.isEmpty()) {
            data.add(new RevenueReportItem(LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy")), 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("title", "BÁO CÁO DOANH THU THEO THÁNG");
        params.put("generatedDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        params.put("totalRevenueAll", currencyFormat.format(grandTotal));

        byte[] pdf = reportService.exportPdf("monthly_revenue.jrxml", params, data);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=monthly-revenue.pdf")
                .body(pdf);
    }

    @GetMapping("/services.pdf")
    public ResponseEntity<byte[]> servicesReport() {
        List<ServiceItem> services = serviceRepo.findAll();
        List<ServiceReportItem> data = services.stream()
                .map(s -> new ServiceReportItem(
                        s.getId(),
                        s.getName(),
                        s.getUnitPrice(),
                        s.getUnit()
                ))
                .collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("title", "BÁO CÁO DANH SÁCH DỊCH VỤ");
        params.put("generatedDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        byte[] pdf = reportService.exportPdf("services.jrxml", params, data);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=services-report.pdf")
                .body(pdf);
    }
}