package com.fairdraw.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    private String roomCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String stateJson;

    protected RoomEntity() {}

    public RoomEntity(String roomCode, String stateJson) {
        this.roomCode = roomCode;
        this.stateJson = stateJson;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getStateJson() {
        return stateJson;
    }

    public void setStateJson(String stateJson) {
        this.stateJson = stateJson;
    }
}
