package com.homestay.homestay.service;

import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.CustomerRepository;
import com.homestay.homestay.repository.InvoiceRepository;
import com.homestay.homestay.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public DashboardService(RoomRepository roomRepository,
                            BookingRepository bookingRepository,
                            InvoiceRepository invoiceRepository,
                            CustomerRepository customerRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Thống kê phòng
        long availableRooms = roomRepository.countByStatus("AVAILABLE");
        long occupiedRooms = roomRepository.countByStatus("OCCUPIED");
        long maintenanceRooms = roomRepository.countByStatus("MAINTENANCE");
        long totalRooms = roomRepository.count();

        stats.put("availableRooms", availableRooms);
        stats.put("occupiedRooms", occupiedRooms);
        stats.put("maintenanceRooms", maintenanceRooms);
        stats.put("totalRooms", totalRooms);

        // Doanh thu tháng hiện tại
        YearMonth currentYearMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentYearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentYearMonth.atEndOfMonth().atTime(LocalTime.MAX);
        BigDecimal currentMonthRevenue = invoiceRepository.sumRevenueBetween(startOfMonth, endOfMonth);
        stats.put("monthRevenue", currentMonthRevenue != null ? currentMonthRevenue : BigDecimal.ZERO);

        // Đặt phòng hôm nay
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long bookingsToday = bookingRepository.countByCreatedAtGreaterThanEqual(startOfToday);
        stats.put("bookingsToday", bookingsToday);

        // Tổng số khách hàng
        long totalCustomers = customerRepository.count();
        stats.put("totalCustomers", totalCustomers);

        return stats;
    }
}
