// === UserRepository.java ===
package com.cloud1pm.backend.repository;

import com.cloud1pm.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}