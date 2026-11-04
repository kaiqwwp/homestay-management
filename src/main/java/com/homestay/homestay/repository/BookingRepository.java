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
import java.util.Optional;

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

    @Query("SELECT b FROM Booking b WHERE " +
           "LOWER(b.customer.email) = LOWER(:email) AND " +
           "(:status IS NULL OR :status = '' OR b.status = :status) AND " +
           "(:fromDate IS NULL OR b.checkIn >= :fromDate) AND " +
           "(:toDate IS NULL OR b.checkOut <= :toDate) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " b.room.roomNumber LIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY b.createdAt DESC")
    Page<Booking> filterBookingsByCustomerEmail(@Param("email") String email,
                                                @Param("status") String status,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate,
                                                @Param("keyword") String keyword,
                                                Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status IN ('PENDING', 'BOOKED', 'CHECKED_IN', 'PAID') " +
           "AND b.checkIn < :checkOut AND b.checkOut > :checkIn " +
           "AND (:excludeBookingId IS NULL OR b.id <> :excludeBookingId)")
    List<Booking> findConflictingBookings(@Param("roomId") Long roomId,
                                         @Param("checkIn") LocalDate checkIn,
                                         @Param("checkOut") LocalDate checkOut,
                                         @Param("excludeBookingId") Long excludeBookingId);

    @Query("SELECT b FROM Booking b WHERE b.status IN ('PENDING', 'BOOKED', 'CHECKED_IN', 'PAID') " +
           "AND b.checkIn < :endDate AND b.checkOut > :startDate")
    List<Booking> findBookingsForSchedule(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    long countByCreatedAtGreaterThanEqual(LocalDateTime startOfDay);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.checkIn = :today")
    long countByCheckIn(@Param("today") LocalDate today);

    // Tìm đơn đang lưu trú hiện tại của phòng (CHECKED_IN hoặc PAID đang ở)
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId AND b.status IN ('CHECKED_IN', 'PAID')")
    List<Booking> findActiveCheckedInByRoomId(@Param("roomId") Long roomId);

    // Tìm các đơn sắp tới của phòng để tính ngày trống tiếp theo
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId AND b.status IN ('PENDING', 'BOOKED', 'CHECKED_IN', 'PAID') AND b.checkOut >= :today ORDER BY b.checkIn ASC")
    List<Booking> findUpcomingBookingsByRoomId(@Param("roomId") Long roomId, @Param("today") LocalDate today);

    // Tìm mã giảm giá đền bù chưa sử dụng theo mã code
    @Query("SELECT b FROM Booking b WHERE UPPER(b.compensationCode) = UPPER(:code) AND (b.compensationUsed IS NULL OR b.compensationUsed = false)")
    Optional<Booking> findUnusedCompensationVoucher(@Param("code") String code);

    // Tìm danh sách các mã giảm giá đền bù chưa sử dụng của 1 khách hàng theo email
    @Query("SELECT b FROM Booking b WHERE LOWER(b.customer.email) = LOWER(:email) AND b.compensationCode IS NOT NULL AND (b.compensationUsed IS NULL OR b.compensationUsed = false) ORDER BY b.rejectedAt DESC")
    List<Booking> findAvailableVouchersByEmail(@Param("email") String email);
}
