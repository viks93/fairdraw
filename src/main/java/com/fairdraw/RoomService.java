package com.fairdraw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fairdraw.entity.RoomEntity;
import com.fairdraw.entity.RoomUserMapping;
import com.fairdraw.repo.RoomRepository;
import com.fairdraw.repo.RoomUserRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoomService {

    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public RoomService(
            RoomRepository roomRepository,
            RoomUserRepository roomUserRepository,
            ObjectMapper objectMapper) {
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> createRoom(int participantCount) {
        if (participantCount < 2 || participantCount > Room.POOL_MAX) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "participantCount must be between 2 and " + Room.POOL_MAX);
        }
        String roomCode = uniqueRoomCode();
        List<String> userCodes = new ArrayList<>(participantCount);
        for (int i = 0; i < participantCount; i++) {
            String code;
            do {
                code = randomCode();
            } while (userCodes.contains(code) || roomUserRepository.existsByUserCode(code));
            userCodes.add(code);
        }
        Room room = new Room(roomCode, userCodes, secureRandom);
        String json = writeState(room.toState());
        roomRepository.save(new RoomEntity(roomCode, json));
        for (String uc : userCodes) {
            roomUserRepository.save(new RoomUserMapping(uc, roomCode));
        }
        return Map.of("roomCode", roomCode, "userCodes", userCodes);
    }

    @Transactional
    public Map<String, Object> draw(String userCode) {
        RoomUserMapping mapping =
                roomUserRepository
                        .findByUserCode(userCode)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Invalid code"));
        RoomEntity entity =
                roomRepository
                        .findByRoomCodeForUpdate(mapping.getRoomCode())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Invalid code"));
        RoomState state = readState(entity.getStateJson());
        Room room = new Room(state);
        Map<String, Object> result = room.draw(userCode);
        entity.setStateJson(writeState(room.toState()));
        roomRepository.save(entity);
        return result;
    }

    @Transactional(readOnly = true)
    public Room getStatus(String userCode) {
        RoomUserMapping mapping =
                roomUserRepository
                        .findByUserCode(userCode)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Invalid code"));
        RoomEntity entity =
                roomRepository
                        .findById(mapping.getRoomCode())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Invalid code"));
        RoomState state = readState(entity.getStateJson());
        return new Room(state);
    }

    private String uniqueRoomCode() {
        String code;
        do {
            code = randomCode();
        } while (roomRepository.existsById(code));
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private String writeState(RoomState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private RoomState readState(String json) {
        try {
            return objectMapper.readValue(json, RoomState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
