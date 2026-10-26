package com.homestay.homestay.service;

import com.homestay.homestay.dto.RoomDto;
import com.homestay.homestay.entity.Room;
import com.homestay.homestay.entity.RoomType;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.exception.ResourceNotFoundException;
import com.homestay.homestay.repository.BookingRepository;
import com.homestay.homestay.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeService roomTypeService;
    private final BookingRepository bookingRepository;

    private final String uploadDir = "uploads/rooms";

    public RoomService(RoomRepository roomRepository, 
                       RoomTypeService roomTypeService, 
                       BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeService = roomTypeService;
        this.bookingRepository = bookingRepository;
    }

    public Page<Room> filterRooms(String status, Long roomTypeId, String keyword, Pageable pageable) {
        return roomRepository.filterRooms(status, roomTypeId, keyword, pageable);
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    public Room findById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng với mã: " + id));
    }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BusinessException("Ngày trả phòng phải sau ngày nhận phòng.");
        }
        return roomRepository.findAvailableRooms(checkIn, checkOut);
    }

    @Transactional
    public Room create(RoomDto dto) {
        if (roomRepository.existsByRoomNumber(dto.getRoomNumber())) {
            throw new BusinessException("Số phòng " + dto.getRoomNumber() + " đã tồn tại trong hệ thống.");
        }

        Room room = new Room();
        room.setRoomNumber(dto.getRoomNumber().trim());
        room.setStatus(dto.getStatus() != null ? dto.getStatus() : "AVAILABLE");

        RoomType roomType = roomTypeService.findById(dto.getRoomTypeId());
        room.setRoomType(roomType);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            room.setImageUrl(saveImageFile(dto.getImageFile()));
        } else {
            room.setImageUrl("/images/room-default.svg");
        }

        return roomRepository.save(room);
    }

    @Transactional
    public Room update(Long id, RoomDto dto) {
        Room room = findById(id);

        if (!room.getRoomNumber().equalsIgnoreCase(dto.getRoomNumber().trim())) {
            if (roomRepository.existsByRoomNumber(dto.getRoomNumber().trim())) {
                throw new BusinessException("Số phòng " + dto.getRoomNumber() + " đã tồn tại.");
            }
            room.setRoomNumber(dto.getRoomNumber().trim());
        }

        room.setStatus(dto.getStatus());
        RoomType roomType = roomTypeService.findById(dto.getRoomTypeId());
        room.setRoomType(roomType);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            room.setImageUrl(saveImageFile(dto.getImageFile()));
        }

        return roomRepository.save(room);
    }

    @Transactional
    public void delete(Long id) {
        Room room = findById(id);
        long activeBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getRoom().getId().equals(id) && 
                        ("BOOKED".equals(b.getStatus()) || "CHECKED_IN".equals(b.getStatus())))
                .count();
        if (activeBookings > 0) {
            throw new BusinessException("Không thể xóa phòng đang có đơn đặt hoặc khách đang ở.");
        }
        roomRepository.delete(room);
    }

    public long countByStatus(String status) {
        return roomRepository.countByStatus(status);
    }

    private String saveImageFile(MultipartFile file) {
        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + ext;
            Path targetPath = Paths.get(uploadDir, newFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/rooms/" + newFilename;
        } catch (IOException e) {
            throw new BusinessException("Không thể lưu ảnh tải lên: " + e.getMessage());
        }
    }
}
