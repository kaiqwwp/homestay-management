package com.homestay.homestay.service;

import com.homestay.homestay.dto.BookingDto;
import com.homestay.homestay.dto.BookingSearchDto;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Customer;
import com.homestay.homestay.entity.Room;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.exception.ResourceNotFoundException;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.CustomerRepository;
import com.homestay.homestay.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository,
                          CustomerRepository customerRepository,
                          RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.roomRepository = roomRepository;
    }

    public Page<Booking> filterBookings(BookingSearchDto searchDto, Pageable pageable) {
        String status = (searchDto != null && searchDto.getStatus() != null && !searchDto.getStatus().isBlank()) ? searchDto.getStatus() : null;
        LocalDate fromDate = (searchDto != null) ? searchDto.getFromDate() : null;
        LocalDate toDate = (searchDto != null) ? searchDto.getToDate() : null;
        String keyword = (searchDto != null && searchDto.getKeyword() != null && !searchDto.getKeyword().isBlank()) ? searchDto.getKeyword().trim() : null;

        return bookingRepository.filterBookings(status, fromDate, toDate, keyword, pageable);
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt phòng với mã: " + id));
    }

    @Transactional
    public Booking create(BookingDto dto) {
        validateBookingDates(dto.getCheckIn(), dto.getCheckOut());

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với mã: " + dto.getCustomerId()));

        Room room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng với mã: " + dto.getRoomId()));

        if ("MAINTENANCE".equalsIgnoreCase(room.getStatus())) {
            throw new BusinessException("Phòng " + room.getRoomNumber() + " đang bảo trì, không thể đặt.");
        }

        // Kiểm tra trùng lịch
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                room.getId(), dto.getCheckIn(), dto.getCheckOut(), null
        );
        if (!conflicts.isEmpty()) {
            throw new BusinessException("Phòng " + room.getRoomNumber() + " đã có người đặt trong khoảng thời gian này.");
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setCheckIn(dto.getCheckIn());
        booking.setCheckOut(dto.getCheckOut());
        booking.setStatus("BOOKED");
        booking.setNote(dto.getNote());

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking checkIn(Long id) {
        Booking booking = findById(id);
        if (!"BOOKED".equals(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể nhận phòng khi đơn ở trạng thái 'Đã đặt' (BOOKED).");
        }

        booking.setStatus("CHECKED_IN");
        Room room = booking.getRoom();
        room.setStatus("OCCUPIED");
        roomRepository.save(room);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancel(Long id) {
        Booking booking = findById(id);
        if (!"BOOKED".equals(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể hủy đơn đặt phòng khi đang ở trạng thái 'Đã đặt' (BOOKED).");
        }

        booking.setStatus("CANCELLED");
        return bookingRepository.save(booking);
    }

    public void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException("Ngày nhận phòng và ngày trả phòng không được để trống.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException("Ngày trả phòng phải sau ngày nhận phòng ít nhất 1 ngày.");
        }
    }

    public Map<String, Object> getCalendarData(LocalDate startDate, int days) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (days <= 0) {
            days = 14;
        }

        LocalDate endDate = startDate.plusDays(days);
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(startDate.plusDays(i));
        }

        List<Room> rooms = roomRepository.findAll();
        List<Booking> bookings = bookingRepository.findBookingsForSchedule(startDate, endDate);

        // Tạo dữ liệu ma trận: Room -> Map<LocalDate, BookingSlotInfo>
        Map<Long, Map<LocalDate, String>> scheduleMatrix = new HashMap<>();
        Map<Long, Map<LocalDate, Long>> bookingIdMatrix = new HashMap<>();

        for (Room room : rooms) {
            Map<LocalDate, String> row = new HashMap<>();
            Map<LocalDate, Long> idRow = new HashMap<>();

            for (LocalDate date : dates) {
                row.put(date, "FREE");
                idRow.put(date, null);
            }

            for (Booking b : bookings) {
                if (b.getRoom().getId().equals(room.getId())) {
                    LocalDate cur = b.getCheckIn();
                    while (cur.isBefore(b.getCheckOut())) {
                        if (row.containsKey(cur)) {
                            row.put(cur, b.getStatus());
                            idRow.put(cur, b.getId());
                        }
                        cur = cur.plusDays(1);
                    }
                }
            }

            scheduleMatrix.put(room.getId(), row);
            bookingIdMatrix.put(room.getId(), idRow);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("days", days);
        result.put("dates", dates);
        result.put("rooms", rooms);
        result.put("scheduleMatrix", scheduleMatrix);
        result.put("bookingIdMatrix", bookingIdMatrix);
        return result;
    }
}
