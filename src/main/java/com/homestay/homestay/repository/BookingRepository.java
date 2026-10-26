package com.homestay.homestay.repository;

import com.homestay.homestay.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b WHERE " +
           "(:status IS NULL OR :status = '' OR b.status = :status) AND " +
           "(:fromDate IS NULL OR b.checkIn >= :fromDate) AND " +
           "(:toDate IS NULL OR b.checkOut <= :toDate) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(b.customer.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " b.customer.phone LIKE CONCAT('%', :keyword, '%') OR " +
           " b.room.roomNumber LIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY b.createdAt DESC")
    Page<Booking> filterBookings(@Param("status") String status,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status IN ('BOOKED', 'CHECKED_IN') " +
           "AND b.checkIn < :checkOut AND b.checkOut > :checkIn " +
           "AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)")
    List<Booking> findConflictingBookings(@Param("roomId") Long roomId,
                                         @Param("checkIn") LocalDate checkIn,
                                         @Param("checkOut") LocalDate checkOut,
                                         @Param("excludeBookingId") Long excludeBookingId);

    @Query("SELECT b FROM Booking b WHERE b.status IN ('BOOKED', 'CHECKED_IN') " +
           "AND b.checkIn < :endDate AND b.checkOut > :startDate")
    List<Booking> findBookingsForSchedule(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    long countByCreatedAtGreaterThanEqual(LocalDateTime startOfDay);

    long countByCheckIn(LocalDate today);
}
