package com.huynqb.laundrylocker.partner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"com.huynqb.laundrylocker.partner", "com.huynqb.laundrylocker.common"})
public class PartnerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PartnerServiceApplication.class, args);
  }
}
