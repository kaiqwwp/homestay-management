package com.homestay.homestay.controller.api;

import com.homestay.homestay.dto.ServiceItemDto;
import com.homestay.homestay.entity.ServiceItem;
import com.homestay.homestay.service.ServiceItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceApiController {

    private final ServiceItemService serviceItemService;

    public ServiceApiController(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceItem>> getAllServices() {
        return ResponseEntity.ok(serviceItemService.findAll());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<ServiceItem>> getServicesPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(serviceItemService.searchServices(keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceItem> getServiceById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(serviceItemService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ServiceItem> createService(@Valid @RequestBody ServiceItemDto dto) {
        return ResponseEntity.ok(serviceItemService.create(dto));
    }
}
