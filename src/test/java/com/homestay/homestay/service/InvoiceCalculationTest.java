package com.homestay.homestay.service;

import com.homestay.homestay.entity.*;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.BookingServiceRepository;
import com.homestay.homestay.repository.InvoiceRepository;
import com.homestay.homestay.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceCalculationTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingServiceRepository bookingServiceRepository;

    @Mock
    private ServiceItemService serviceItemService;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Booking booking;
    private Room room;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("Phòng đôi");
        roomType.setPricePerNight(new BigDecimal("500000"));

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        room.setStatus("OCCUPIED");

        booking = new Booking();
        booking.setId(100L);
        booking.setRoom(room);
        booking.setCheckIn(LocalDate.of(2026, 10, 1));
        booking.setCheckOut(LocalDate.of(2026, 10, 4)); // 3 đêm
        booking.setStatus("CHECKED_IN");
    }

    @Test
    @DisplayName("Tính đúng tiền phòng = số đêm x đơn giá/đêm (3 đêm x 500.000 = 1.500.000)")
    void testCalculateRoomTotal() {
        BigDecimal roomTotal = invoiceService.calculateRoomTotal(booking);
        assertEquals(new BigDecimal("1500000"), roomTotal);
    }

    @Test
    @DisplayName("Tính đúng tiền dịch vụ = tổng (số lượng x đơn giá tại thời điểm dùng)")
    void testCalculateServiceTotal() {
        ServiceItem s1 = new ServiceItem();
        s1.setName("Ăn sáng");
        BookingServiceItem bs1 = new BookingServiceItem();
        bs1.setService(s1);
        bs1.setQuantity(2);
        bs1.setUnitPrice(new BigDecimal("50000")); // 100.000

        ServiceItem s2 = new ServiceItem();
        s2.setName("Thuê xe máy");
        BookingServiceItem bs2 = new BookingServiceItem();
        bs2.setService(s2);
        bs2.setQuantity(1);
        bs2.setUnitPrice(new BigDecimal("150000")); // 150.000

        when(bookingServiceRepository.findByBookingId(100L)).thenReturn(Arrays.asList(bs1, bs2));

        BigDecimal serviceTotal = invoiceService.calculateServiceTotal(booking);
        assertEquals(new BigDecimal("250000"), serviceTotal);
    }

    @Test
    @DisplayName("Hoàn tất trả phòng và tạo hóa đơn: chuyển booking sang CHECKED_OUT, phòng sang AVAILABLE")
    void testCheckOutAndCreateInvoice() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(invoiceRepository.findByBookingId(100L)).thenReturn(Optional.empty());
        when(bookingServiceRepository.findByBookingId(100L)).thenReturn(Collections.emptyList());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = invoiceService.checkOutAndCreateInvoice(100L, "Tiền mặt");

        assertNotNull(invoice);
        assertEquals(new BigDecimal("1500000"), invoice.getRoomTotal());
        assertEquals(BigDecimal.ZERO, invoice.getServiceTotal());
        assertEquals(new BigDecimal("1500000"), invoice.getTotal());
        assertEquals("Tiền mặt", invoice.getPaymentMethod());

        // Kiểm tra chuyển trạng thái
        assertEquals("CHECKED_OUT", booking.getStatus());
        assertEquals("AVAILABLE", room.getStatus());

        verify(bookingRepository).save(booking);
        verify(roomRepository).save(room);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Ném BusinessException nếu trả phòng khi đơn không ở trạng thái CHECKED_IN")
    void testCheckOutInvalidStatus() {
        booking.setStatus("BOOKED");
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class, () -> {
            invoiceService.checkOutAndCreateInvoice(100L, "Tiền mặt");
        });
    }
}
