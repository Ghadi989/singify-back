package com.gray.singifyback.config;

import com.gray.singifyback.model.Song;
import com.gray.singifyback.repository.SongRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs once on startup. Inserts sample songs only if the table is empty.
 * audioUrl → will be replaced with real GCS URLs once bucket is set up.
 * coverUrl → Spotify CDN album art URLs (free to use for display).
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final SongRepository songRepository;

    public DataSeeder(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (songRepository.count() > 0) {
            return; // already seeded
        }

        List<Song> songs = List.of(
            song("Hard Times",
                 "Paramore",
                 "https://i.scdn.co/image/ab67616d0000b273b4f8f9dce36f7fc7e0fd5e6e",
                 "https://storage.googleapis.com/singify-bucket/audio/paramore-hard-times.mp3",
                 "2:57"),

            song("Bohemian Rhapsody",
                 "Queen",
                 "https://i.scdn.co/image/ab67616d0000b273ce4f1737bc8a646c8c4bd25a",
                 "https://storage.googleapis.com/singify-bucket/audio/queen-bohemian-rhapsody.mp3",
                 "5:55"),

            song("Blinding Lights",
                 "The Weeknd",
                 "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36",
                 "https://storage.googleapis.com/singify-bucket/audio/weeknd-blinding-lights.mp3",
                 "3:20"),

            song("Rolling in the Deep",
                 "Adele",
                 "https://i.scdn.co/image/ab67616d0000b273e319baafd16e84f0408af2a0",
                 "https://storage.googleapis.com/singify-bucket/audio/adele-rolling-in-the-deep.mp3",
                 "3:48"),

            song("bad guy",
                 "Billie Eilish",
                 "https://i.scdn.co/image/ab67616d0000b27350a3147b4edd7701a876c6ce",
                 "https://storage.googleapis.com/singify-bucket/audio/billie-bad-guy.mp3",
                 "3:14"),

            song("Shape of You",
                 "Ed Sheeran",
                 "https://i.scdn.co/image/ab67616d0000b273ba5db46f4b838ef6027e6f96",
                 "https://storage.googleapis.com/singify-bucket/audio/ed-shape-of-you.mp3",
                 "3:53"),

            song("Levitating",
                 "Dua Lipa",
                 "https://i.scdn.co/image/ab67616d0000b2734bc66095f8a70bc4e6593f4f",
                 "https://storage.googleapis.com/singify-bucket/audio/dualipa-levitating.mp3",
                 "3:23"),

            song("Stay With Me",
                 "Sam Smith",
                 "https://i.scdn.co/image/ab67616d0000b273a0d10484514f0ef89871d0cb",
                 "https://storage.googleapis.com/singify-bucket/audio/samsmith-stay-with-me.mp3",
                 "2:52"),

            song("Someone Like You",
                 "Adele",
                 "https://i.scdn.co/image/ab67616d0000b273e319baafd16e84f0408af2a0",
                 "https://storage.googleapis.com/singify-bucket/audio/adele-someone-like-you.mp3",
                 "4:45"),

            song("Watermelon Sugar",
                 "Harry Styles",
                 "https://i.scdn.co/image/ab67616d0000b27310536fe9fb5cb96b021ae6e3",
                 "https://storage.googleapis.com/singify-bucket/audio/harry-watermelon-sugar.mp3",
                 "2:54")
        );

        songRepository.saveAll(songs);
        System.out.println(">> DataSeeder: inserted " + songs.size() + " songs.");
    }

    private Song song(String title, String artist, String coverUrl, String audioUrl, String duration) {
        Song s = new Song();
        s.setTitle(title);
        s.setArtist(artist);
        s.setCoverUrl(coverUrl);
        s.setAudioUrl(audioUrl);
        s.setDuration(duration);
        return s;
    }
}
