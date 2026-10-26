package com.homestay.homestay.repository;

import com.homestay.homestay.entity.ServiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    Page<ServiceItem> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
