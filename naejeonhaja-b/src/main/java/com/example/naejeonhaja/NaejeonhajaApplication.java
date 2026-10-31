package com.example.naejeonhaja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class NaejeonhajaApplication {
    public static void main(String[] args) {
        SpringApplication.run(NaejeonhajaApplication.class, args);
    }
}
