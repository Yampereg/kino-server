package com.example.kino;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.kino")
@EntityScan(basePackages = "com.example.kino")
public class KinoApplication {

    public static void main(String[] args) {
        SpringApplication.run(KinoApplication.class, args);
    }

}
