package com.homestay.homestay.service;

import com.homestay.homestay.dto.AddBookingServiceDto;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.BookingServiceItem;
import com.homestay.homestay.entity.Invoice;
import com.homestay.homestay.entity.Room;
import com.homestay.homestay.entity.ServiceItem;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.exception.ResourceNotFoundException;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.BookingServiceRepository;
import com.homestay.homestay.repository.InvoiceRepository;
import com.homestay.homestay.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final ServiceItemService serviceItemService;
    private final RoomRepository roomRepository;
    private final EmailService emailService;

    @Autowired(required = false)
    private RealtimeEventService realtimeEventService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          BookingRepository bookingRepository,
                          BookingServiceRepository bookingServiceRepository,
                          ServiceItemService serviceItemService,
                          RoomRepository roomRepository,
                          EmailService emailService) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.bookingServiceRepository = bookingServiceRepository;
        this.serviceItemService = serviceItemService;
        this.roomRepository = roomRepository;
        this.emailService = emailService;
    }

    public Page<Invoice> filterInvoices(String keyword, Pageable pageable) {
        return invoiceRepository.filterInvoices(keyword, pageable);
    }

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với mã: " + id));
    }

    public Invoice findByBookingId(Long bookingId) {
        return invoiceRepository.findByBookingId(bookingId)
                .orElse(null);
    }

    public List<BookingServiceItem> getBookingServices(Long bookingId) {
        return bookingServiceRepository.findByBookingId(bookingId);
    }

    @Transactional
    public BookingServiceItem addServiceToBooking(Long bookingId, AddBookingServiceDto dto) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt phòng: " + bookingId));

        if (!"CHECKED_IN".equals(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể thêm dịch vụ khi khách đang nhận phòng (CHECKED_IN) và chưa thanh toán.");
        }

        ServiceItem serviceItem = serviceItemService.findById(dto.getServiceId());

        BookingServiceItem bs = new BookingServiceItem();
        bs.setBooking(booking);
        bs.setService(serviceItem);
        bs.setQuantity(dto.getQuantity());
        bs.setUnitPrice(serviceItem.getUnitPrice());
        bs.setUsedAt(LocalDateTime.now());

        return bookingServiceRepository.save(bs);
    }

    @Transactional
    public void removeServiceFromBooking(Long bookingServiceId) {
        BookingServiceItem bs = bookingServiceRepository.findById(bookingServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ đã gắn mã: " + bookingServiceId));

        if (!"CHECKED_IN".equals(bs.getBooking().getStatus())) {
            throw new BusinessException("Chỉ có thể xóa dịch vụ khi đơn đặt phòng đang ở trạng thái CHECKED_IN.");
        }

        bookingServiceRepository.delete(bs);
    }

    public BigDecimal calculateRoomTotal(Booking booking) {
        long nights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
        if (nights <= 0) {
            nights = 1;
        }
        BigDecimal pricePerNight = booking.getRoom().getRoomType().getPricePerNight();
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    public BigDecimal calculateServiceTotal(Booking booking) {
        List<BookingServiceItem> services = bookingServiceRepository.findByBookingId(booking.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (BookingServiceItem bs : services) {
            BigDecimal lineTotal = bs.getUnitPrice().multiply(BigDecimal.valueOf(bs.getQuantity()));
            total = total.add(lineTotal);
        }
        return total;
    }

    public BigDecimal calculateDiscountAmount(Booking booking, BigDecimal roomTotal, BigDecimal serviceTotal) {
        int pct = (booking.getDiscountPercent() != null) ? booking.getDiscountPercent() : 0;
        if (pct <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal subTotal = roomTotal.add(serviceTotal);
        return subTotal.multiply(BigDecimal.valueOf(pct))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    @Transactional
    public Invoice checkOutAndCreateInvoice(Long bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt phòng: " + bookingId));

        if (!"CHECKED_IN".equals(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể thanh toán khi khách đã nhận phòng (CHECKED_IN).");
        }

        if (invoiceRepository.findByBookingId(bookingId).isPresent()) {
            throw new BusinessException("Đơn đặt phòng này đã được tạo hóa đơn thanh toán.");
        }

        BigDecimal roomTotal = calculateRoomTotal(booking);
        BigDecimal serviceTotal = calculateServiceTotal(booking);
        BigDecimal discountAmount = calculateDiscountAmount(booking, roomTotal, serviceTotal);
        BigDecimal total = roomTotal.add(serviceTotal).subtract(discountAmount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        invoice.setRoomTotal(roomTotal);
        invoice.setServiceTotal(serviceTotal);
        invoice.setDiscountAmount(discountAmount);
        invoice.setTotal(total);
        invoice.setPaidAt(now);
        invoice.setPaymentMethod(paymentMethod != null ? paymentMethod : "Tiền mặt");

        Invoice savedInvoice = invoiceRepository.save(invoice);

        booking.setPaidAt(now);
        Room room = booking.getRoom();

        // Nếu đã đến hoặc qua ngày trả phòng (checkOut <= today) -> chuyển về CHECKED_OUT và phòng về AVAILABLE ngay
        // Nếu chưa hết hạn ngày trả phòng (checkOut > today) -> chuyển sang PAID (Đã thanh toán), khi hết hạn ngày sẽ tự động về AVAILABLE
        if (!booking.getCheckOut().isAfter(LocalDate.now())) {
            booking.setStatus("CHECKED_OUT");
            booking.setCheckedOutAt(now);
            room.setStatus("AVAILABLE");
            roomRepository.save(room);
        } else {
            booking.setStatus("PAID");
            room.setStatus("OCCUPIED");
            roomRepository.save(room);
        }
        bookingRepository.save(booking);

        emailService.sendInvoiceReceipt(savedInvoice);

        if (realtimeEventService != null) {
            String msg;
            if ("PAID".equals(booking.getStatus())) {
                msg = String.format("Đã thanh toán đơn #%d: Phòng %s đã đổi sang trạng thái ĐÃ THANH TOÁN (Sẽ tự động về Trống khi hết hạn ngày %s)",
                        booking.getId(), room.getRoomNumber(), booking.getCheckOut());
                realtimeEventService.publishBookingEvent("BOOKING_PAID", booking, msg);
            } else {
                msg = String.format("Thanh toán & Trả phòng #%d: Phòng %s hiện đã TRỐNG (AVAILABLE) và lưu lịch sử.",
                        booking.getId(), room.getRoomNumber());
                realtimeEventService.publishBookingEvent("BOOKING_CHECKED_OUT", booking, msg);
            }
        }

        return savedInvoice;
    }
}
