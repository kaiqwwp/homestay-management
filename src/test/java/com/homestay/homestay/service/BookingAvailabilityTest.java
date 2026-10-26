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

    @InjectMocks
    private BookingService bookingService;

    private Customer customer;
    private Room room;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFullName("Nguyễn Văn A");

        RoomType roomType = new RoomType();
        roomType.setName("Phòng đơn");

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        room.setStatus("AVAILABLE");
    }

    @Test
    @DisplayName("Ném BusinessException nếu ngày trả phòng trước hoặc bằng ngày nhận phòng")
    void testValidateBookingDatesInvalid() {
        LocalDate checkIn = LocalDate.of(2026, 10, 5);
        LocalDate checkOut = LocalDate.of(2026, 10, 4);

        assertThrows(BusinessException.class, () -> {
            bookingService.validateBookingDates(checkIn, checkOut);
        });

        assertThrows(BusinessException.class, () -> {
            bookingService.validateBookingDates(checkIn, checkIn);
        });
    }

    @Test
    @DisplayName("Ném BusinessException nếu phòng đã có người đặt trong khoảng ngày trùng lịch")
    void testCreateBookingConflict() {
        BookingDto dto = new BookingDto();
        dto.setCustomerId(1L);
        dto.setRoomId(10L);
        dto.setCheckIn(LocalDate.of(2026, 10, 1));
        dto.setCheckOut(LocalDate.of(2026, 10, 5));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        // Giả lập đã có booking xung đột
        Booking existing = new Booking();
        existing.setId(99L);
        when(bookingRepository.findConflictingBookings(eq(10L), eq(dto.getCheckIn()), eq(dto.getCheckOut()), isNull()))
                .thenReturn(List.of(existing));

        assertThrows(BusinessException.class, () -> {
            bookingService.create(dto);
        });
    }

    @Test
    @DisplayName("Tạo đơn đặt phòng thành công khi không bị trùng lịch")
    void testCreateBookingSuccess() {
        BookingDto dto = new BookingDto();
        dto.setCustomerId(1L);
        dto.setRoomId(10L);
        dto.setCheckIn(LocalDate.of(2026, 10, 10));
        dto.setCheckOut(LocalDate.of(2026, 10, 12));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(eq(10L), eq(dto.getCheckIn()), eq(dto.getCheckOut()), isNull()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking created = bookingService.create(dto);

        assertNotNull(created);
        assertEquals("BOOKED", created.getStatus());
        assertEquals(dto.getCheckIn(), created.getCheckIn());
        assertEquals(dto.getCheckOut(), created.getCheckOut());
    }

    @Test
    @DisplayName("Nhận phòng (Check-in): chuyển booking sang CHECKED_IN và room sang OCCUPIED")
    void testCheckIn() {
        Booking booking = new Booking();
        booking.setId(50L);
        booking.setStatus("BOOKED");
        booking.setRoom(room);

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.checkIn(50L);

        assertEquals("CHECKED_IN", result.getStatus());
        assertEquals("OCCUPIED", room.getStatus());
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
