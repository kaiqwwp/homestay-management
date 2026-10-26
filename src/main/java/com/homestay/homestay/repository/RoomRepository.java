package com.homestay.homestay.repository;

import com.homestay.homestay.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomNumber(String roomNumber);
    boolean existsByRoomNumber(String roomNumber);

    long countByStatus(String status);

    @Query("SELECT r FROM Room r WHERE " +
           "(:status IS NULL OR :status = '' OR r.status = :status) AND " +
           "(:roomTypeId IS NULL OR r.roomType.id = :roomTypeId) AND " +
           "(:keyword IS NULL OR :keyword = '' OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Room> filterRooms(@Param("status") String status, 
                           @Param("roomTypeId") Long roomTypeId, 
                           @Param("keyword") String keyword, 
                           Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.status <> 'MAINTENANCE' AND r.id NOT IN (" +
           "SELECT b.room.id FROM Booking b " +
           "WHERE b.status IN ('BOOKED', 'CHECKED_IN') " +
           "AND b.checkIn < :checkOut AND b.checkOut > :checkIn)")
    List<Room> findAvailableRooms(@Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);
}
