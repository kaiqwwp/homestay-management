package com.homestay.homestay.controller;

import com.homestay.homestay.entity.RoomType;
import com.homestay.homestay.repository.RoomTypeRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class TestController {
    private final RoomTypeRepository repo;
    public TestController(RoomTypeRepository repo) { this.repo = repo; }

    @GetMapping("/test/room-types")
    public List<RoomType> all() { return repo.findAll(); }
}