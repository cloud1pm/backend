// === EncouragementMessageRepository.java ===
package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.EncouragementMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EncouragementMessageRepository extends JpaRepository<EncouragementMessage, Long> {
    Optional<EncouragementMessage> findByUserIdAndDate(Long userId, LocalDate date);
    List<EncouragementMessage> findByUserIdOrderByDateDesc(Long userId);
}