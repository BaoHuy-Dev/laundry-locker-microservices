package com.huynqb.laundrylocker.user.repository;

import com.huynqb.laundrylocker.user.model.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

  Optional<UserProfile> findFirstByPhoneNumber(String phoneNumber);
}
