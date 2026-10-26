package com.homestay.homestay.controller.api;

import com.homestay.homestay.dto.BookingDto;
import com.homestay.homestay.dto.BookingSearchDto;
import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {

    private final BookingService bookingService;

    public BookingApiController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<Page<Booking>> getBookings(
            @ModelAttribute BookingSearchDto searchDto,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.filterBookings(searchDto, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody BookingDto dto) {
        return ResponseEntity.ok(bookingService.create(dto));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@PathVariable("id") Long id) {
        Booking booking = bookingService.checkIn(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Nhận phòng thành công",
                "status", booking.getStatus()
        ));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable("id") Long id) {
        Booking booking = bookingService.cancel(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã hủy đơn đặt phòng",
                "status", booking.getStatus()
        ));
    }
}
