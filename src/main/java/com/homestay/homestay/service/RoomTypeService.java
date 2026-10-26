package com.homestay.homestay.service;

import com.homestay.homestay.dto.RoomTypeDto;
import com.homestay.homestay.entity.RoomType;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.exception.ResourceNotFoundException;
import com.homestay.homestay.repository.RoomRepository;
import com.homestay.homestay.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
    }

    public List<RoomType> findAll() {
        return roomTypeRepository.findAll();
    }

    public RoomType findById(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại phòng với mã: " + id));
    }

    @Transactional
    public RoomType create(RoomTypeDto dto) {
        RoomType roomType = new RoomType();
        roomType.setName(dto.getName());
        roomType.setPricePerNight(dto.getPricePerNight());
        roomType.setCapacity(dto.getCapacity());
        roomType.setDescription(dto.getDescription());
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public RoomType update(Long id, RoomTypeDto dto) {
        RoomType roomType = findById(id);
        roomType.setName(dto.getName());
        roomType.setPricePerNight(dto.getPricePerNight());
        roomType.setCapacity(dto.getCapacity());
        roomType.setDescription(dto.getDescription());
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public void delete(Long id) {
        RoomType roomType = findById(id);
        long roomCount = roomRepository.findAll().stream()
                .filter(r -> r.getRoomType().getId().equals(id))
                .count();
        if (roomCount > 0) {
            throw new BusinessException("Không thể xóa loại phòng đang có " + roomCount + " phòng sử dụng.");
        }
        roomTypeRepository.delete(roomType);
    }
}
