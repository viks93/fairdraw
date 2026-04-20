package com.fairdraw;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** In-memory representation of a room; rebuilt from {@link RoomState} after each load. */
public class Room {

    static final int POOL_MIN = 1;
    static final int POOL_MAX = 50;

    private final String roomCode;
    private final List<String> userCodes;
    private final List<Integer> deck;
    private final List<DrawEvent> history = new ArrayList<>();

    /** New room: build and shuffle deck. */
    public Room(String roomCode, List<String> userCodes, SecureRandom secureRandom) {
        this.roomCode = roomCode;
        this.userCodes = List.copyOf(userCodes);
        List<Integer> nums =
                IntStream.rangeClosed(POOL_MIN, POOL_MAX)
                        .boxed()
                        .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(nums, secureRandom);
        this.deck = nums;
    }

    /** Restore from persisted state. */
    public Room(RoomState state) {
        this.roomCode = state.roomCode;
        this.userCodes = new ArrayList<>(state.userCodes);
        this.deck = new ArrayList<>(state.deck);
        for (DrawEventRecord dr : state.history) {
            history.add(new DrawEvent(dr.userCode, dr.number, Instant.parse(dr.timestamp)));
        }
    }

    public RoomState toState() {
        RoomState s = new RoomState();
        s.roomCode = roomCode;
        s.userCodes = new ArrayList<>(userCodes);
        s.deck = new ArrayList<>(deck);
        s.history =
                history.stream()
                        .map(
                                e ->
                                        new DrawEventRecord(
                                                e.getUserCode(), e.getNumber(), e.getTimestamp()))
                        .collect(Collectors.toCollection(ArrayList::new));
        return s;
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
}
