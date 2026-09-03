package com.adobe.printservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PrintServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrintServiceApplication.class, args);
    }
}
