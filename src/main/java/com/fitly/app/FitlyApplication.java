package com.fitly.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Fitly backend. */
@SpringBootApplication
@SuppressWarnings("PMD.UseUtilityClass") // Spring Boot needs an instantiable configuration class
public class FitlyApplication {
    /** Starts the Spring Boot application. */
    public static void main(String[] args) {
        SpringApplication.run(FitlyApplication.class, args);
    }
}
