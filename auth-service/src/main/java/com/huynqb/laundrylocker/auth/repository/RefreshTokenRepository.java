package com.huynqb.laundrylocker.auth.repository;

import com.huynqb.laundrylocker.auth.model.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);
}
