package com.homestay.homestay.service;

import com.homestay.homestay.dto.sepay.SepayWebhookPayload;
import com.homestay.homestay.entity.*;
import com.homestay.homestay.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiAndSepayIntegrationTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ServiceItemRepository serviceItemRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceService invoiceService;

    private AiChatService aiChatService;
    private SepayPaymentService sepayPaymentService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatService(roomTypeRepository, roomRepository, serviceItemRepository);
        sepayPaymentService = new SepayPaymentService(bookingRepository, invoiceRepository, invoiceService);
        ReflectionTestUtils.setField(sepayPaymentService, "bankName", "MBBank");
        ReflectionTestUtils.setField(sepayPaymentService, "bankAccount", "060105797979");
        ReflectionTestUtils.setField(sepayPaymentService, "accountName", "VO NHUT THANH");
        ReflectionTestUtils.setField(sepayPaymentService, "transferPrefix", "HOMESTAY");
    }

    @Test
    @DisplayName("Trợ lý AI Chatbot tư vấn đúng loại phòng, giá và số phòng trống cho đoàn 2 người")
    void testAiChatBotPeopleRecommendation() {
        RoomType doubleRoom = new RoomType();
        doubleRoom.setId(2L);
        doubleRoom.setName("Phòng đôi");
        doubleRoom.setCapacity(2);
        doubleRoom.setPricePerNight(new BigDecimal("500000"));
        doubleRoom.setDescription("Giường đôi King-size, ban công");

        Room r102 = new Room();
        r102.setRoomNumber("102");
        r102.setRoomType(doubleRoom);
        r102.setStatus("AVAILABLE");

        when(roomTypeRepository.findAll()).thenReturn(List.of(doubleRoom));
        when(roomRepository.findAll()).thenReturn(List.of(r102));
        when(serviceItemRepository.findAll()).thenReturn(List.of());

        String reply = aiChatService.reply("Tư vấn phòng cho 2 người");

        assertNotNull(reply);
        assertTrue(reply.contains("2 người"));
        assertTrue(reply.contains("Phòng đôi"));
        assertTrue(reply.contains("102"));
    }

    @Test
    @DisplayName("SePay tạo đúng đường dẫn VietQR MB Bank kèm số tiền và nội dung chuyển khoản")
    void testGenerateSepayQrUrl() {
        String qrUrl = sepayPaymentService.generateVietQrUrl(4L, new BigDecimal("900000"));

        assertNotNull(qrUrl);
        assertTrue(qrUrl.contains("qr.sepay.vn/img?acc=060105797979"));
        assertTrue(qrUrl.contains("bank=MBBank"));
        assertTrue(qrUrl.contains("amount=900000"));
        assertTrue(qrUrl.contains("des=HOMESTAY4"));
    }

    @Test
    @DisplayName("SePay Webhook tự động xác nhận thanh toán khi nhận đủ số tiền và đúng mã đơn HOMESTAY4")
    void testProcessSepayWebhookSuccess() {
        Booking booking = new Booking();
        booking.setId(4L);
        booking.setStatus("CHECKED_IN");

        Invoice mockInvoice = new Invoice();
        mockInvoice.setId(10L);

        when(bookingRepository.findById(4L)).thenReturn(Optional.of(booking));
        when(invoiceService.checkOutAndCreateInvoice(eq(4L), anyString())).thenReturn(mockInvoice);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);

        SepayWebhookPayload payload = new SepayWebhookPayload();
        payload.setTransferType("in");
        payload.setTransferAmount(new BigDecimal("900000"));
        payload.setContent("MBVCB.123456.HOMESTAY4 chuyen khoan");
        payload.setGateway("MBBank");
        payload.setReferenceCode("FT26308");

        boolean processed = sepayPaymentService.processWebhook(payload, null);

        assertTrue(processed);
        verify(invoiceService).checkOutAndCreateInvoice(eq(4L), eq("QR SePay (MB Bank)"));
        assertEquals("FT26308", mockInvoice.getPaymentReference());
    }
}
