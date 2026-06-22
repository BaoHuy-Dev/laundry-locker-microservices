package com.huynqb.laundrylocker.auth.repository;

import com.huynqb.laundrylocker.auth.model.SocialIdentity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentity, Long> {
  Optional<SocialIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
  List<SocialIdentity> findByAccountId(Long accountId);
}
