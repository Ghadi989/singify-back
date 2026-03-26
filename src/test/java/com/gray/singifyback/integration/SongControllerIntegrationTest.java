package com.gray.singifyback.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SongControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void getAllSongs_returns200AndSeededSongs() throws Exception {
        mockMvc.perform(get("/api/songs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getRecommendedSongs_returns200AndSeededSongs() throws Exception {
        mockMvc.perform(get("/api/songs/recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void searchSongs_withKnownArtist_returnsResults() throws Exception {
        mockMvc.perform(get("/api/songs/search").param("q", "Adele"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void searchSongs_withNoMatch_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/songs/search").param("q", "xyznotexist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void libraryEndpoint_withoutAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/library"))
                .andExpect(status().is4xxClientError());
    }
}
