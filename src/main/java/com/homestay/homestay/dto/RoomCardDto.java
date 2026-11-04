package com.homestay.homestay.dto;

import com.homestay.homestay.entity.Booking;
import com.homestay.homestay.entity.Room;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RoomCardDto {
    private Long id;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private BigDecimal pricePerNight;
    private Integer capacity;
    private String status; // AVAILABLE, OCCUPIED, PAID, MAINTENANCE
    private String imageUrl;

    // Thông tin thời gian thực
    private boolean currentlyOccupied; // Đang có khách ở hôm nay
    private boolean currentBookingPaid; // Khách đang ở đã thanh toán chưa
    private String currentGuestName;   // Tên khách đang ở (nếu có)
    private LocalDate occupiedUntil;   // Đang ở đến ngày nào
    private LocalDate nextAvailableDate; // Thời điểm có thể nhận phòng tiếp theo
    private Long activeBookingId;      // ID đơn đang ở (để chuyển nhanh tới chi tiết/trả phòng)

    public static RoomCardDto fromRoom(Room room, Booking currentActiveBooking, LocalDate nextAvailable) {
        RoomCardDto dto = new RoomCardDto();
        dto.setId(room.getId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setStatus(room.getStatus());
        dto.setImageUrl(room.getImageUrl());

        if (room.getRoomType() != null) {
            dto.setRoomTypeId(room.getRoomType().getId());
            dto.setRoomTypeName(room.getRoomType().getName());
            dto.setPricePerNight(room.getRoomType().getPricePerNight());
            dto.setCapacity(room.getRoomType().getCapacity());
        }

        if (currentActiveBooking != null) {
            dto.setCurrentlyOccupied(true);
            dto.setCurrentBookingPaid("PAID".equalsIgnoreCase(currentActiveBooking.getStatus()) || "PAID".equalsIgnoreCase(room.getStatus()));
            dto.setCurrentGuestName(currentActiveBooking.getCustomer() != null ? currentActiveBooking.getCustomer().getFullName() : "Khách lưu trú");
            dto.setOccupiedUntil(currentActiveBooking.getCheckOut());
            dto.setActiveBookingId(currentActiveBooking.getId());
        } else {
            dto.setCurrentlyOccupied(false);
            dto.setCurrentBookingPaid(false);
        }

        dto.setNextAvailableDate(nextAvailable != null ? nextAvailable : LocalDate.now());
        return dto;
    }
}
