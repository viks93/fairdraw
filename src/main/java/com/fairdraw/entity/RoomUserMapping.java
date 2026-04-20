package com.fairdraw.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "room_users",
        indexes = @Index(name = "idx_room_users_room", columnList = "roomCode"))
public class RoomUserMapping {

    @Id
    private String userCode;

    @Column(nullable = false)
    private String roomCode;

    protected RoomUserMapping() {}

    public RoomUserMapping(String userCode, String roomCode) {
        this.userCode = userCode;
        this.roomCode = roomCode;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getRoomCode() {
        return roomCode;
    }
}
