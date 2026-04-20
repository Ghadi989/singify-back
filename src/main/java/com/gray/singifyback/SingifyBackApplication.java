package com.gray.singifyback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class SingifyBackApplication {

    private static final Logger log = LoggerFactory.getLogger(SingifyBackApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SingifyBackApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("");
        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║           SINGIFY  —  App is ready!              ║");
        log.info("╠══════════════════════════════════════════════════╣");
        log.info("║  Frontend     →  http://localhost:3000           ║");
        log.info("║  Backend API  →  http://localhost:8080/api/songs ║");
        log.info("║  Kafka UI     →  http://localhost:8090           ║");
        log.info("╚══════════════════════════════════════════════════╝");
        log.info("");
    }

}
