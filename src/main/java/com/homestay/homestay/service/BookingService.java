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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final EmailService emailService;

    @Autowired(required = false)
    private RealtimeEventService realtimeEventService;

    public BookingService(BookingRepository bookingRepository,
                          CustomerRepository customerRepository,
                          RoomRepository roomRepository,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.customerRepository = customerRepository;
        this.roomRepository = roomRepository;
        this.emailService = emailService;
    }

    public Page<Booking> filterBookings(BookingSearchDto searchDto, Pageable pageable) {
        String status = (searchDto != null && searchDto.getStatus() != null && !searchDto.getStatus().isBlank()) ? searchDto.getStatus() : null;
        LocalDate fromDate = (searchDto != null) ? searchDto.getFromDate() : null;
        LocalDate toDate = (searchDto != null) ? searchDto.getToDate() : null;
        String keyword = (searchDto != null && searchDto.getKeyword() != null && !searchDto.getKeyword().isBlank()) ? searchDto.getKeyword().trim() : null;

        return bookingRepository.filterBookings(status, fromDate, toDate, keyword, pageable);
    }

    public Page<Booking> filterBookingsByCustomerEmail(String email, BookingSearchDto searchDto, Pageable pageable) {
        if (email == null || email.isBlank()) {
            return Page.empty(pageable);
        }
        String status = (searchDto != null && searchDto.getStatus() != null && !searchDto.getStatus().isBlank()) ? searchDto.getStatus() : null;
        LocalDate fromDate = (searchDto != null) ? searchDto.getFromDate() : null;
        LocalDate toDate = (searchDto != null) ? searchDto.getToDate() : null;
        String keyword = (searchDto != null && searchDto.getKeyword() != null && !searchDto.getKeyword().isBlank()) ? searchDto.getKeyword().trim() : null;

        return bookingRepository.filterBookingsByCustomerEmail(email.trim(), status, fromDate, toDate, keyword, pageable);
    }

    public List<Booking> findAvailableVouchersByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Collections.emptyList();
        }
        return bookingRepository.findAvailableVouchersByEmail(email.trim());
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

        Customer customer;
        boolean isInternalOrTestCall = (dto.getCustomerId() != null);
        if (isInternalOrTestCall) {
            customer = customerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với mã: " + dto.getCustomerId()));
        } else {
            String fullName = dto.getCustomerFullName() != null ? dto.getCustomerFullName().trim() : "";
            String phone = dto.getCustomerPhone() != null ? dto.getCustomerPhone().trim() : "";
            String email = dto.getCustomerEmail() != null ? dto.getCustomerEmail().trim() : "";
            String idNumber = dto.getCustomerIdNumber() != null ? dto.getCustomerIdNumber().trim() : "";

            if (fullName.isEmpty()) {
                throw new BusinessException("Vui lòng nhập họ và tên người đặt phòng.");
            }
            if (phone.isEmpty()) {
                throw new BusinessException("Vui lòng nhập số điện thoại liên hệ.");
            }

            Optional<Customer> existing = Optional.empty();
            if (!email.isEmpty()) {
                existing = customerRepository.findFirstByEmailIgnoreCase(email);
            }
            if (existing.isEmpty() && !phone.isEmpty()) {
                existing = customerRepository.findFirstByPhone(phone);
            }

            customer = existing.orElseGet(Customer::new);
            customer.setFullName(fullName);
            customer.setPhone(phone);
            if (!email.isEmpty()) {
                customer.setEmail(email);
            }
            if (!idNumber.isEmpty()) {
                customer.setIdNumber(idNumber);
            }
            customer = customerRepository.save(customer);
        }

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
        // Đơn đặt phòng từ giao diện khách hàng bắt đầu ở trạng thái PENDING (Chờ khách sạn chấp nhận/từ chối)
        booking.setStatus(isInternalOrTestCall ? "BOOKED" : "PENDING");
        booking.setNote(dto.getNote());
        booking.setCreatedAt(LocalDateTime.now());

        // Áp dụng mã giảm giá đền bù (nếu khách nhập)
        if (dto.getVoucherCode() != null && !dto.getVoucherCode().trim().isEmpty()) {
            String code = dto.getVoucherCode().trim().toUpperCase();
            Booking voucherSource = bookingRepository.findUnusedCompensationVoucher(code)
                    .orElseThrow(() -> new BusinessException("Mã giảm giá '" + code + "' không tồn tại hoặc đã được sử dụng."));
            voucherSource.setCompensationUsed(true);
            bookingRepository.save(voucherSource);

            booking.setAppliedVoucherCode(code);
            booking.setDiscountPercent(voucherSource.getCompensationDiscount() != null ? voucherSource.getCompensationDiscount() : 5);
        }

        Booking saved = bookingRepository.save(booking);
        if (realtimeEventService != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String msg = String.format("Yêu cầu đặt phòng mới #%d (Chờ duyệt): Khách %s đặt Phòng %s (%s - %s)",
                    saved.getId(),
                    customer.getFullName(),
                    room.getRoomNumber(),
                    saved.getCheckIn().format(fmt),
                    saved.getCheckOut().format(fmt));
            realtimeEventService.publishBookingEvent("BOOKING_CREATED", saved, msg);
        }
        return saved;
    }

    /**
     * Khách sạn (Admin / Staff) CHẤP NHẬN yêu cầu đặt phòng
     */
    @Transactional
    public Booking approve(Long id) {
        Booking booking = findById(id);
        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể chấp nhận các đơn đang ở trạng thái 'Chờ duyệt' (PENDING).");
        }

        booking.setStatus("BOOKED");
        booking.setApprovedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);
        emailService.sendBookingConfirmation(saved);

        if (realtimeEventService != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String msg = String.format("Đặt phòng thành công! Đơn #%d (Phòng %s, %s - %s) của khách %s đã được khách sạn CHẤP NHẬN.",
                    saved.getId(),
                    saved.getRoom().getRoomNumber(),
                    saved.getCheckIn().format(fmt),
                    saved.getCheckOut().format(fmt),
                    saved.getCustomer().getFullName());
            realtimeEventService.publishBookingEvent("BOOKING_APPROVED", saved, msg);
        }
        return saved;
    }

    /**
     * Khách sạn (Admin / Staff) TỪ CHỐI yêu cầu đặt phòng và đền bù mã giảm giá 5-10%
     */
    @Transactional
    public Booking reject(Long id, String reason, Integer discountPercent) {
        Booking booking = findById(id);
        if (!"PENDING".equalsIgnoreCase(booking.getStatus()) && !"BOOKED".equalsIgnoreCase(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể từ chối đơn khi khách chưa nhận phòng.");
        }

        int discount = (discountPercent != null) ? discountPercent : 5;
        if (discount < 5) discount = 5;
        if (discount > 10) discount = 10;

        String cleanReason = (reason != null && !reason.trim().isEmpty())
                ? reason.trim()
                : "Phòng đang có sự cố kỹ thuật đột xuất / trễ lịch bảo trì";

        String voucherCode = "DENBU" + discount + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        booking.setStatus("REJECTED");
        booking.setRejectReason(cleanReason);
        booking.setCompensationDiscount(discount);
        booking.setCompensationCode(voucherCode);
        booking.setCompensationUsed(false);
        booking.setRejectedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        if (realtimeEventService != null) {
            String msg = String.format("Đơn đặt Phòng %s (#%d) bị từ chối do: %s. Tặng mã giảm giá đền bù %s (-%d%%) cho lần đặt tiếp theo!",
                    saved.getRoom().getRoomNumber(),
                    saved.getId(),
                    cleanReason,
                    voucherCode,
                    discount);
            realtimeEventService.publishBookingEvent("BOOKING_REJECTED", saved, msg);
        }
        return saved;
    }

    /**
     * Đến ngày check-in: Quản lý / Admin bấm confirm Checked-in để xác nhận khách đã nhận phòng (sau đó mới mở khóa nút Thanh toán)
     */
    @Transactional
    public Booking checkIn(Long id) {
        Booking booking = findById(id);
        if (!"BOOKED".equalsIgnoreCase(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể xác nhận nhận phòng (Check-in) khi đơn đã được chấp nhận (BOOKED).");
        }

        booking.setStatus("CHECKED_IN");
        booking.setCheckedInAt(LocalDateTime.now());
        Room room = booking.getRoom();
        room.setStatus("OCCUPIED");
        roomRepository.save(room);

        Booking saved = bookingRepository.save(booking);
        if (realtimeEventService != null) {
            String msg = String.format("Đã xác nhận Check-in đơn #%d: Khách %s đã nhận Phòng %s (Đã mở khóa thanh toán)",
                    saved.getId(),
                    saved.getCustomer().getFullName(),
                    room.getRoomNumber());
            realtimeEventService.publishBookingEvent("BOOKING_CHECKED_IN", saved, msg);
        }
        return saved;
    }

    /**
     * Giải phóng phòng về trống (AVAILABLE) và chuyển đơn từ PAID sang CHECKED_OUT (lưu lịch sử chi tiết)
     */
    @Transactional
    public Booking completeCheckoutAndReleaseRoom(Long id) {
        Booking booking = findById(id);
        if (!"PAID".equalsIgnoreCase(booking.getStatus()) && !"CHECKED_IN".equalsIgnoreCase(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể giải phóng phòng khi đơn đã thanh toán (PAID).");
        }

        booking.setStatus("CHECKED_OUT");
        booking.setCheckedOutAt(LocalDateTime.now());
        Room room = booking.getRoom();
        room.setStatus("AVAILABLE");
        roomRepository.save(room);

        Booking saved = bookingRepository.save(booking);
        if (realtimeEventService != null) {
            String msg = String.format("Hoàn tất lưu trú đơn #%d: Phòng %s đã chuyển về trạng thái TRỐNG (AVAILABLE) và lưu lịch sử.",
                    saved.getId(),
                    room.getRoomNumber());
            realtimeEventService.publishBookingEvent("BOOKING_CHECKED_OUT", saved, msg);
        }
        return saved;
    }

    @Transactional
    public Booking cancel(Long id) {
        Booking booking = findById(id);
        if (!"BOOKED".equalsIgnoreCase(booking.getStatus()) && !"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể hủy đơn đặt phòng khi khách chưa nhận phòng.");
        }

        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);
        if (realtimeEventService != null) {
            String msg = String.format("Hủy đơn #%d: Đơn đặt Phòng %s của khách %s đã bị hủy",
                    saved.getId(),
                    saved.getRoom().getRoomNumber(),
                    saved.getCustomer().getFullName());
            realtimeEventService.publishBookingEvent("BOOKING_CANCELLED", saved, msg);
        }
        return saved;
    }

    public void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException("Ngày nhận phòng và ngày trả phòng không được để trống.");
        }
        LocalDate today = LocalDate.now();
        if (checkIn.isBefore(today)) {
            throw new BusinessException("Không được phép chọn ngày nhận phòng là ngày trong quá khứ.");
        }
        if (checkOut.isBefore(today.plusDays(1))) {
            throw new BusinessException("Không được phép chọn ngày trả phòng là ngày trong quá khứ.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException("Ngày trả phòng phải lớn hơn ngày nhận phòng ít nhất 1 ngày.");
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
