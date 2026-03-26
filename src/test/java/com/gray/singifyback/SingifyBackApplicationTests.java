package com.gray.singifyback;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
class SingifyBackApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring context starts successfully with H2 + test profile
    }
}
