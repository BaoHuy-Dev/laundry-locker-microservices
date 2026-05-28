package com.huynqb.laundrylocker.laundry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"com.huynqb.laundrylocker.laundry", "com.huynqb.laundrylocker.common"})
public class LaundryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(LaundryServiceApplication.class, args);
  }
}
