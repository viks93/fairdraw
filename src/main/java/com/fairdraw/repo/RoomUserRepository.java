package com.fairdraw.repo;

import com.fairdraw.entity.RoomUserMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomUserRepository extends JpaRepository<RoomUserMapping, String> {

    Optional<RoomUserMapping> findByUserCode(String userCode);

    boolean existsByUserCode(String userCode);
}
