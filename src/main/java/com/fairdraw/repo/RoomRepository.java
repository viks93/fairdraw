package com.fairdraw.repo;

import com.fairdraw.entity.RoomEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<RoomEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RoomEntity r WHERE r.roomCode = :code")
    Optional<RoomEntity> findByRoomCodeForUpdate(@Param("code") String roomCode);
}
