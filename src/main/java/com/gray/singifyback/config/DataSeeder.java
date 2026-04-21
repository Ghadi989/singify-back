package com.gray.singifyback.config;

import com.gray.singifyback.repository.SongRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Intentionally empty — DB songs removed in favour of Spotify search + yt-dlp pipeline.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    public DataSeeder(SongRepository songRepository) {}

    @Override
    public void run(ApplicationArguments args) {
        // Nothing to seed — all songs come from Spotify search
    }
}
