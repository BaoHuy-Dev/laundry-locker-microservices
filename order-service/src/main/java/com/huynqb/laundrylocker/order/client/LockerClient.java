package com.huynqb.laundrylocker.order.client;

import com.huynqb.laundrylocker.common.dto.ApiResponse;
import com.huynqb.laundrylocker.common.dto.LockerBoxSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "locker-service", path = "/internal/boxes")
public interface LockerClient {

  @GetMapping("/{id}")
  ApiResponse<LockerBoxSummary> getBox(@PathVariable Long id);

  @PostMapping("/{id}/reserve")
  ApiResponse<LockerBoxSummary> reserveBox(@PathVariable Long id);

  @PostMapping("/{id}/occupy")
  ApiResponse<LockerBoxSummary> occupyBox(@PathVariable Long id);

  @PostMapping("/{id}/release")
  ApiResponse<LockerBoxSummary> releaseBox(@PathVariable Long id);
}
