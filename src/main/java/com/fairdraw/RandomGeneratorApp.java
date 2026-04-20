package com.fairdraw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class RandomGeneratorApp {

    private static final int POOL_MIN = 1;
    private static final int POOL_MAX = 50;
    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();

    public static void main(String[] args) {
        SpringApplication.run(RandomGeneratorApp.class, args);
    }

    @PostMapping("/create")
    public Map<String, Object> createRoom(@RequestParam int participantCount) {
        if (participantCount < 2 || participantCount > POOL_MAX) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "participantCount must be between 2 and " + POOL_MAX);
        }
        String roomCode = uniqueRoomCode();
        List<String> userCodes = new ArrayList<>(participantCount);
        for (int i = 0; i < participantCount; i++) {
            String code;
            do {
                code = randomCode();
            } while (userCodes.contains(code) || findRoomByUserCode(code) != null);
            userCodes.add(code);
        }
        rooms.put(roomCode, new Room(roomCode, userCodes));
        return Map.of("roomCode", roomCode, "userCodes", userCodes);
    }

    @PostMapping("/draw")
    public Map<String, Object> draw(@RequestParam String userCode) {
        Room room = findRoomByUserCode(userCode);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code");
        }
        return room.draw(userCode);
    }

    @GetMapping("/status")
    public Room getStatus(@RequestParam String userCode) {
        Room room = findRoomByUserCode(userCode);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code");
        }
        return room;
    }

    private static Room findRoomByUserCode(String code) {
        return rooms.values().stream()
                .filter(r -> r.getUserCodes().contains(code))
                .findFirst()
                .orElse(null);
    }

    private static String randomCode() {
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String uniqueRoomCode() {
        String code;
        do {
            code = randomCode();
        } while (rooms.containsKey(code));
        return code;
    }

    public static final class DrawEvent {
        private final String userCode;
        private final int number;
        private final String timestamp;

        public DrawEvent(String userCode, int number, Instant when) {
            this.userCode = userCode;
            this.number = number;
            this.timestamp = when.toString();
        }

        public String getUserCode() {
            return userCode;
        }

        public int getNumber() {
            return number;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }

    public static final class Room {
        private final String roomCode;
        private final List<String> userCodes;
        private final List<Integer> deck;
        private final List<DrawEvent> history = new ArrayList<>();

        public Room(String roomCode, List<String> userCodes) {
            this.roomCode = roomCode;
            this.userCodes = List.copyOf(userCodes);
            List<Integer> nums = IntStream.rangeClosed(POOL_MIN, POOL_MAX)
                    .boxed()
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(nums, secureRandom);
            this.deck = nums;
        }

        public synchronized Map<String, Object> draw(String userCode) {
            if (!this.userCodes.contains(userCode)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid code");
            }
            if (deck.isEmpty()) {
                Map<String, Object> done = new LinkedHashMap<>();
                done.put("roomCode", roomCode);
                done.put("number", null);
                done.put("done", true);
                done.put("remaining", 0);
                done.put("history", List.copyOf(history));
                return done;
            }
            int n = deck.remove(0);
            history.add(new DrawEvent(userCode, n, Instant.now()));
            return Map.of(
                    "roomCode", roomCode,
                    "number", n,
                    "done", false,
                    "remaining", deck.size(),
                    "history", List.copyOf(history));
        }

        public String getRoomCode() {
            return roomCode;
        }

        public List<String> getUserCodes() {
            return userCodes;
        }

        public synchronized List<DrawEvent> getHistory() {
            return List.copyOf(history);
        }

        public synchronized int getRemaining() {
            return deck.size();
        }
    }
}
