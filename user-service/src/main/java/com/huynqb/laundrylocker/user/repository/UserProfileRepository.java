package com.huynqb.laundrylocker.user.repository;

import com.huynqb.laundrylocker.user.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {}
