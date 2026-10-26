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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public InvoiceService(InvoiceRepository invoiceRepository,
                          BookingRepository bookingRepository,
                          BookingServiceRepository bookingServiceRepository,
                          ServiceItemService serviceItemService,
                          RoomRepository roomRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.bookingServiceRepository = bookingServiceRepository;
        this.serviceItemService = serviceItemService;
        this.roomRepository = roomRepository;
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
            throw new BusinessException("Chỉ có thể thêm dịch vụ khi khách đang nhận phòng (CHECKED_IN).");
        }

        ServiceItem serviceItem = serviceItemService.findById(dto.getServiceId());

        BookingServiceItem bs = new BookingServiceItem();
        bs.setBooking(booking);
        bs.setService(serviceItem);
        bs.setQuantity(dto.getQuantity());
        // Chốt đơn giá tại thời điểm dùng
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

    @Transactional
    public Invoice checkOutAndCreateInvoice(Long bookingId, String paymentMethod) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt phòng: " + bookingId));

        if (!"CHECKED_IN".equals(booking.getStatus())) {
            throw new BusinessException("Chỉ có thể trả phòng và thanh toán khi đơn ở trạng thái 'Đang ở' (CHECKED_IN).");
        }

        if (invoiceRepository.findByBookingId(bookingId).isPresent()) {
            throw new BusinessException("Đơn đặt phòng này đã được tạo hóa đơn.");
        }

        BigDecimal roomTotal = calculateRoomTotal(booking);
        BigDecimal serviceTotal = calculateServiceTotal(booking);
        BigDecimal total = roomTotal.add(serviceTotal);

        Invoice invoice = new Invoice();
        invoice.setBooking(booking);
        invoice.setRoomTotal(roomTotal);
        invoice.setServiceTotal(serviceTotal);
        invoice.setTotal(total);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setPaymentMethod(paymentMethod != null ? paymentMethod : "Tiền mặt");

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Chuyển trạng thái booking sang CHECKED_OUT
        booking.setStatus("CHECKED_OUT");
        bookingRepository.save(booking);

        // Chuyển trạng thái phòng về AVAILABLE
        Room room = booking.getRoom();
        room.setStatus("AVAILABLE");
        roomRepository.save(room);

        return savedInvoice;
    }
}
