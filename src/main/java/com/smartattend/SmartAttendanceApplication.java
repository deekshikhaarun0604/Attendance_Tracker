package com.smartattend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class SmartAttendanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartAttendanceApplication.class, args);
    }
}
