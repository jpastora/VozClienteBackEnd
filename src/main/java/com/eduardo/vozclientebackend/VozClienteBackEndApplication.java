package com.eduardo.vozclientebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class VozClienteBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(VozClienteBackEndApplication.class, args);
    }
}
