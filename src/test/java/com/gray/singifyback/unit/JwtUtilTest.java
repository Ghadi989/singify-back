package com.gray.singifyback.unit;

import com.gray.singifyback.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "testSecretKeyForSingifyThatIsLongEnough123456789");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken("user@test.com");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("user@test.com");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    void isTokenValid_freshToken_returnsTrue() {
        String token = jwtUtil.generateToken("user@test.com");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_garbage_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.valid.token")).isFalse();
    }
}
