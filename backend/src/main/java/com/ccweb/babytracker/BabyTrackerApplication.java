package com.ccweb.babytracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BabyTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BabyTrackerApplication.class, args);
    }

}
