package com.homestay.homestay.service;

import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.entity.Room;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.InvoiceRepository;
import com.homestay.homestay.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class RoomStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoomStatusScheduler.class);

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final RealtimeEventService realtimeEventService;

    public RoomStatusScheduler(RoomRepository roomRepository,
                               BookingRepository bookingRepository,
                               InvoiceRepository invoiceRepository,
                               RealtimeEventService realtimeEventService) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.realtimeEventService = realtimeEventService;
    }

    /**
     * Đồng bộ hóa trạng thái phòng theo thời gian thực mỗi 60 giây:
     * 1. Nếu khách đang ở (CHECKED_IN hoặc PAID) đã hết hạn trả phòng (checkOut <= today) và đã thanh toán -> Tự động CHECKED_OUT và giải phóng phòng về AVAILABLE.
     * 2. Đảm bảo trạng thái Room (OCCUPIED / AVAILABLE) luôn khớp với việc có khách đang lưu trú hay không (trừ phòng MAINTENANCE).
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    @Transactional
    public void syncRealTimeRoomAndBookingStatuses() {
        LocalDate today = LocalDate.now();
        List<Room> allRooms = roomRepository.findAll();

        for (Room room : allRooms) {
            if ("MAINTENANCE".equalsIgnoreCase(room.getStatus())) {
                continue;
            }

            List<Booking> activeBookings = bookingRepository.findActiveCheckedInByRoomId(room.getId());
            boolean hasActiveGuest = false;

            for (Booking b : activeBookings) {
                Optional<Invoice> invOpt = invoiceRepository.findByBookingId(b.getId());
                boolean isPaid = "PAID".equalsIgnoreCase(b.getStatus()) || invOpt.isPresent();
                // Nếu đã đến/quá ngày trả phòng và đã thanh toán -> tự động chuyển phòng về trống và lưu vào lịch sử
                if (!b.getCheckOut().isAfter(today) && isPaid) {
                    b.setStatus("CHECKED_OUT");
                    b.setCheckedOutAt(LocalDateTime.now());
                    bookingRepository.save(b);
                    log.info("Auto checkout cho đơn #{} tại phòng {}", b.getId(), room.getRoomNumber());
                    realtimeEventService.publishBookingEvent("BOOKING_CHECKED_OUT", b,
                            "Tự động giải phóng Phòng " + room.getRoomNumber() + " về TRỐNG (Đơn #" + b.getId() + " đã hết hạn ngày lưu trú & đã thanh toán)");
                } else {
                    hasActiveGuest = true;
                }
            }

            String expectedStatus = hasActiveGuest ? "OCCUPIED" : "AVAILABLE";
            if (!expectedStatus.equalsIgnoreCase(room.getStatus())) {
                room.setStatus(expectedStatus);
                roomRepository.save(room);
                log.info("Đồng bộ trạng thái phòng {} -> {}", room.getRoomNumber(), expectedStatus);
            }
        }
    }
}
