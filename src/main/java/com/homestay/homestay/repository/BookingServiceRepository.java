package com.homestay.homestay.repository;

import com.homestay.homestay.entity.BookingServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingServiceRepository extends JpaRepository<BookingServiceItem, Long> {
    List<BookingServiceItem> findByBookingId(Long bookingId);
}
