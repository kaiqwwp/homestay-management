package com.homestay.homestay.service;

import com.homestay.homestay.dto.BookingDto;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Customer;
import com.homestay.homestay.entity.Room;
import com.homestay.homestay.entity.RoomType;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.CustomerRepository;
import com.homestay.homestay.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingAvailabilityTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingService bookingService;

    private Customer customer;
    private Room room;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFullName("Nguyễn Văn A");
        customer.setEmail("nguyenvana@gmail.com");

        RoomType roomType = new RoomType();
        roomType.setName("Phòng đơn");

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        room.setStatus("AVAILABLE");
    }

    @Test
    @DisplayName("Ném BusinessException nếu ngày nhận/trả trong quá khứ hoặc ngày trả <= ngày nhận")
    void testValidateBookingDatesInvalid() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        assertThrows(BusinessException.class, () -> {
            bookingService.validateBookingDates(checkIn, checkOut);
        });

        assertThrows(BusinessException.class, () -> {
            bookingService.validateBookingDates(checkIn, checkIn);
        });

        // Kiểm tra không cho phép chọn ngày nhận phòng trong quá khứ
        assertThrows(BusinessException.class, () -> {
            bookingService.validateBookingDates(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        });
    }

    @Test
    @DisplayName("Ném BusinessException nếu phòng đã có người đặt trong khoảng ngày trùng lịch")
    void testCreateBookingConflict() {
        BookingDto dto = new BookingDto();
        dto.setCustomerId(1L);
        dto.setRoomId(10L);
        dto.setCheckIn(LocalDate.now().plusDays(1));
        dto.setCheckOut(LocalDate.now().plusDays(5));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        Booking existing = new Booking();
        existing.setId(99L);
        when(bookingRepository.findConflictingBookings(eq(10L), eq(dto.getCheckIn()), eq(dto.getCheckOut()), isNull()))
                .thenReturn(List.of(existing));

        assertThrows(BusinessException.class, () -> {
            bookingService.create(dto);
        });
    }

    @Test
    @DisplayName("Tạo đơn đặt phòng nội bộ thành công khi không bị trùng lịch")
    void testCreateBookingSuccess() {
        BookingDto dto = new BookingDto();
        dto.setCustomerId(1L);
        dto.setRoomId(10L);
        dto.setCheckIn(LocalDate.now().plusDays(3));
        dto.setCheckOut(LocalDate.now().plusDays(5));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(eq(10L), eq(dto.getCheckIn()), eq(dto.getCheckOut()), isNull()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking created = bookingService.create(dto);

        assertNotNull(created);
        assertEquals("BOOKED", created.getStatus());
        assertNotNull(created.getCreatedAt());
        assertEquals(dto.getCheckIn(), created.getCheckIn());
        assertEquals(dto.getCheckOut(), created.getCheckOut());
    }

    @Test
    @DisplayName("Khách hàng đặt phòng trực tuyến bắt đầu ở trạng thái chờ duyệt (PENDING)")
    void testCreateCustomerBookingStartsPending() {
        BookingDto dto = new BookingDto();
        dto.setCustomerFullName("Võ Nhựt Thanh");
        dto.setCustomerPhone("0909556688");
        dto.setCustomerEmail("kodvnt61@gmail.com");
        dto.setCustomerIdNumber("012345678963");
        dto.setRoomId(10L);
        dto.setCheckIn(LocalDate.now().plusDays(2));
        dto.setCheckOut(LocalDate.now().plusDays(4));

        when(customerRepository.findFirstByPhone("0909556688")).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(eq(10L), eq(dto.getCheckIn()), eq(dto.getCheckOut()), isNull()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking created = bookingService.create(dto);

        assertNotNull(created);
        assertEquals("PENDING", created.getStatus());
    }

    @Test
    @DisplayName("Admin duyệt đơn (approve): chuyển từ PENDING sang BOOKED và ghi nhận approvedAt")
    void testApproveBooking() {
        Booking booking = new Booking();
        booking.setId(50L);
        booking.setStatus("PENDING");
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setCheckIn(LocalDate.now().plusDays(1));
        booking.setCheckOut(LocalDate.now().plusDays(2));

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking approved = bookingService.approve(50L);

        assertEquals("BOOKED", approved.getStatus());
        assertNotNull(approved.getApprovedAt());
    }

    @Test
    @DisplayName("Admin từ chối đơn (reject): chuyển sang REJECTED và sinh mã Voucher đền bù DENBU10")
    void testRejectBookingGeneratesVoucher() {
        Booking booking = new Booking();
        booking.setId(51L);
        booking.setStatus("PENDING");
        booking.setCustomer(customer);
        booking.setRoom(room);

        when(bookingRepository.findById(51L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking rejected = bookingService.reject(51L, "Phòng bảo trì đột xuất", 10);

        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("Phòng bảo trì đột xuất", rejected.getRejectReason());
        assertEquals(10, rejected.getCompensationDiscount());
        assertNotNull(rejected.getCompensationCode());
        assertTrue(rejected.getCompensationCode().startsWith("DENBU10-"));
        assertNotNull(rejected.getRejectedAt());
    }

    @Test
    @DisplayName("Nhận phòng (Check-in): chuyển booking từ BOOKED sang CHECKED_IN và room sang OCCUPIED")
    void testCheckIn() {
        Booking booking = new Booking();
        booking.setId(50L);
        booking.setStatus("BOOKED");
        booking.setRoom(room);

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.checkIn(50L);

        assertEquals("CHECKED_IN", result.getStatus());
        assertNotNull(result.getCheckedInAt());
        assertEquals("OCCUPIED", room.getStatus());
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("Giải phóng phòng sau khi đã PAID (completeCheckoutAndReleaseRoom): chuyển sang CHECKED_OUT và mở khóa phòng AVAILABLE")
    void testReleaseRoomAfterPaid() {
        room.setStatus("OCCUPIED");
        Booking booking = new Booking();
        booking.setId(50L);
        booking.setStatus("PAID");
        booking.setRoom(room);

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.completeCheckoutAndReleaseRoom(50L);

        assertEquals("CHECKED_OUT", result.getStatus());
        assertNotNull(result.getCheckedOutAt());
        assertEquals("AVAILABLE", room.getStatus());
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("Hủy đơn đặt phòng khi đang BOOKED chuyển sang CANCELLED")
    void testCancelBooking() {
        Booking booking = new Booking();
        booking.setId(50L);
        booking.setStatus("BOOKED");

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancel(50L);

        assertEquals("CANCELLED", result.getStatus());
    }
}
