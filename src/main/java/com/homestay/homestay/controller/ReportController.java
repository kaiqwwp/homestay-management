package com.homestay.homestay.controller;

import com.homestay.homestay.repository.RoomTypeRepository;
import com.homestay.homestay.service.ReportService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final RoomTypeRepository roomTypeRepo;

    public ReportController(ReportService reportService, RoomTypeRepository roomTypeRepo) {
        this.reportService = reportService;
        this.roomTypeRepo = roomTypeRepo;
    }

    @GetMapping("/room-types.pdf")
    public ResponseEntity<byte[]> roomTypes() {
        byte[] pdf = reportService.exportPdf(
                "room_types.jrxml",
                Map.of("title", "DANH SÁCH LOẠI PHÒNG"),
                roomTypeRepo.findAll());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=room-types.pdf")
                .body(pdf);
    }
}