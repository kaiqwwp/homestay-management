package com.homestay.homestay.repository;

import com.homestay.homestay.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByBookingId(Long bookingId);

    @Query("SELECT i FROM Invoice i WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(i.booking.customer.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " i.booking.room.roomNumber LIKE CONCAT('%', :keyword, '%') OR " +
           " i.paymentMethod LIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY i.paidAt DESC")
    Page<Invoice> filterInvoices(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.paidAt >= :startOfMonth AND i.paidAt <= :endOfMonth")
    BigDecimal sumRevenueBetween(@Param("startOfMonth") LocalDateTime startOfMonth,
                                 @Param("endOfMonth") LocalDateTime endOfMonth);
}
