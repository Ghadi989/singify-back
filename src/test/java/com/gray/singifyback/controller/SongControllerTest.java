package com.gray.singifyback.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gray.singifyback.dto.SongCreateDTO;
import com.gray.singifyback.dto.SongDTO;
import com.gray.singifyback.exception.GlobalExceptionHandler;
import com.gray.singifyback.exception.ResourceNotFoundException;
import com.gray.singifyback.service.SongService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SongController.class)
@Import(GlobalExceptionHandler.class)
class SongControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SongService songService;

    private SongDTO songDTO;
    private SongCreateDTO createDTO;

    @BeforeEach
    void setUp() {
        songDTO = SongDTO.builder()
                .id(1L)
                .spotifyId("spotify-123")
                .title("Bohemian Rhapsody")
                .artist("Queen")
                .genre("Rock")
                .releaseYear(1975)
                .albumCoverUrl("http://example.com/cover.jpg")
                .durationMs(354000L)
                .build();

        createDTO = SongCreateDTO.builder()
                .spotifyId("spotify-123")
                .title("Bohemian Rhapsody")
                .artist("Queen")
                .genre("Rock")
                .releaseYear(1975)
                .albumCoverUrl("http://example.com/cover.jpg")
                .durationMs(354000L)
                .build();
    }

    @Test
    @WithMockUser
    void getAllSongs_returns200WithList() throws Exception {
        when(songService.getAllSongs()).thenReturn(List.of(songDTO));

        mockMvc.perform(get("/api/songs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Bohemian Rhapsody"))
                .andExpect(jsonPath("$[0].artist").value("Queen"));
    }

    @Test
    @WithMockUser
    void getSongById_returns200WithSong() throws Exception {
        when(songService.getSongById(1L)).thenReturn(songDTO);

        mockMvc.perform(get("/api/songs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Bohemian Rhapsody"));
    }

    @Test
    @WithMockUser
    void getSongById_returns404WhenNotFound() throws Exception {
        when(songService.getSongById(999L))
                .thenThrow(new ResourceNotFoundException("Song not found with id: 999"));

        mockMvc.perform(get("/api/songs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Song not found with id: 999"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSong_withValidBody_returns201() throws Exception {
        when(songService.createSong(any(SongCreateDTO.class))).thenReturn(songDTO);

        mockMvc.perform(post("/api/songs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Bohemian Rhapsody"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSong_withMissingTitle_returns400() throws Exception {
        SongCreateDTO invalid = SongCreateDTO.builder()
                .artist("Queen")
                .releaseYear(1975)
                .durationMs(354000L)
                .build();

        mockMvc.perform(post("/api/songs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSong_returns200() throws Exception {
        when(songService.updateSong(eq(1L), any(SongCreateDTO.class))).thenReturn(songDTO);

        mockMvc.perform(put("/api/songs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Bohemian Rhapsody"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSong_returns204() throws Exception {
        doNothing().when(songService).deleteSong(1L);

        mockMvc.perform(delete("/api/songs/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void searchByTitle_returns200() throws Exception {
        when(songService.searchByTitle("Bohemian")).thenReturn(List.of(songDTO));

        mockMvc.perform(get("/api/songs/search").param("title", "Bohemian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Bohemian Rhapsody"));
    }

    @Test
    @WithMockUser
    void searchByArtist_returns200() throws Exception {
        when(songService.searchByArtist("Queen")).thenReturn(List.of(songDTO));

        mockMvc.perform(get("/api/songs/search").param("artist", "Queen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].artist").value("Queen"));
    }

    @Test
    @WithMockUser
    void search_withNoParams_returns400() throws Exception {
        mockMvc.perform(get("/api/songs/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void filterByGenre_returns200() throws Exception {
        when(songService.filterByGenre("Rock")).thenReturn(List.of(songDTO));

        mockMvc.perform(get("/api/songs/filter").param("genre", "Rock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genre").value("Rock"));
    }
}
