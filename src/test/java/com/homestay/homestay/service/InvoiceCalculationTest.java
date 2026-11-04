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

    @Mock
    private EmailService emailService;

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
        booking.setCheckIn(LocalDate.now());
        booking.setCheckOut(LocalDate.now().plusDays(3)); // 3 đêm đang trong kỳ lưu trú
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
    @DisplayName("Xác nhận thanh toán và xuất hóa đơn: chuyển booking từ CHECKED_IN sang PAID và trừ giảm giá Voucher nếu có")
    void testCheckOutAndCreateInvoiceWithVoucher() {
        booking.setDiscountPercent(10); // Giảm 10%
        booking.setAppliedVoucherCode("DENBU10-TEST");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(invoiceRepository.findByBookingId(100L)).thenReturn(Optional.empty());
        when(bookingServiceRepository.findByBookingId(100L)).thenReturn(Collections.emptyList());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = invoiceService.checkOutAndCreateInvoice(100L, "Tiền mặt");

        assertNotNull(invoice);
        assertEquals(new BigDecimal("1500000"), invoice.getRoomTotal());
        assertEquals(BigDecimal.ZERO, invoice.getServiceTotal());
        assertEquals(new BigDecimal("150000"), invoice.getDiscountAmount());
        assertEquals(new BigDecimal("1350000"), invoice.getTotal());
        assertEquals("Tiền mặt", invoice.getPaymentMethod());

        // Kiểm tra chuyển trạng thái sang PAID khi khách đang lưu trú (checkOut > today)
        assertEquals("PAID", booking.getStatus());
        assertNotNull(booking.getPaidAt());

        verify(bookingRepository).save(booking);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Ném BusinessException nếu thanh toán khi đơn chưa được xác nhận Check-in (CHECKED_IN)")
    void testCheckOutInvalidStatus() {
        booking.setStatus("BOOKED");
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class, () -> {
            invoiceService.checkOutAndCreateInvoice(100L, "Tiền mặt");
        });
    }
}
