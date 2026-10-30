package com.eduardo.vozclientebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VozClienteBackEndApplication {

    public static void main(String[] args) {
        // 1. Arranca el backend de Spring Boot primero
        SpringApplication.run(VozClienteBackEndApplication.class, args);

        // 2. Ejecuta el menú interactivo de tu consola
        ConsolaPruebas.main(new String[]{});
    }

}
