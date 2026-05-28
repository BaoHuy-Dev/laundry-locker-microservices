package com.huynqb.laundrylocker.staff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"com.huynqb.laundrylocker.staff", "com.huynqb.laundrylocker.common"})
public class StaffServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(StaffServiceApplication.class, args);
  }
}
