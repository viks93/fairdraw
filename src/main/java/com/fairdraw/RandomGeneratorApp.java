package com.fairdraw;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class RandomGeneratorApp {

    private final RoomService roomService;

    public RandomGeneratorApp(RoomService roomService) {
        this.roomService = roomService;
    }

    public static void main(String[] args) {
        SpringApplication.run(RandomGeneratorApp.class, args);
    }

    @PostMapping("/create")
    public Map<String, Object> createRoom(@RequestParam int participantCount) {
        return roomService.createRoom(participantCount);
    }

    @PostMapping("/draw")
    public Map<String, Object> draw(@RequestParam String userCode) {
        return roomService.draw(userCode);
    }

    @GetMapping("/status")
    public Room getStatus(@RequestParam String userCode) {
        return roomService.getStatus(userCode);
    }
}
