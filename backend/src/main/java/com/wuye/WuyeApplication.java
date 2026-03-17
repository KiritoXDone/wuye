package com.wuye;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WuyeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WuyeApplication.class, args);
    }
}
