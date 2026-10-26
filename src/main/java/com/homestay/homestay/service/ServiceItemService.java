package com.homestay.homestay.service;

import com.homestay.homestay.dto.ServiceItemDto;
import com.homestay.homestay.entity.ServiceItem;
import com.homestay.homestay.exception.BusinessException;
import com.homestay.homestay.exception.ResourceNotFoundException;
import com.homestay.homestay.repository.BookingServiceRepository;
import com.homestay.homestay.repository.ServiceItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final BookingServiceRepository bookingServiceRepository;

    public ServiceItemService(ServiceItemRepository serviceItemRepository, 
                              BookingServiceRepository bookingServiceRepository) {
        this.serviceItemRepository = serviceItemRepository;
        this.bookingServiceRepository = bookingServiceRepository;
    }

    public Page<ServiceItem> searchServices(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return serviceItemRepository.findAll(pageable);
        }
        return serviceItemRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
    }

    public List<ServiceItem> findAll() {
        return serviceItemRepository.findAll();
    }

    public ServiceItem findById(Long id) {
        return serviceItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ với mã: " + id));
    }

    @Transactional
    public ServiceItem create(ServiceItemDto dto) {
        ServiceItem item = new ServiceItem();
        item.setName(dto.getName().trim());
        item.setUnitPrice(dto.getUnitPrice());
        item.setUnit(dto.getUnit().trim());
        return serviceItemRepository.save(item);
    }

    @Transactional
    public ServiceItem update(Long id, ServiceItemDto dto) {
        ServiceItem item = findById(id);
        item.setName(dto.getName().trim());
        item.setUnitPrice(dto.getUnitPrice());
        item.setUnit(dto.getUnit().trim());
        return serviceItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        ServiceItem item = findById(id);
        long usedCount = bookingServiceRepository.findAll().stream()
                .filter(bs -> bs.getService().getId().equals(id))
                .count();
        if (usedCount > 0) {
            throw new BusinessException("Không thể xóa dịch vụ đã được sử dụng trong các đơn đặt phòng.");
        }
        serviceItemRepository.delete(item);
    }
}
