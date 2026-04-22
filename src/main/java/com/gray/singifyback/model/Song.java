package com.gray.singifyback.model;

import jakarta.persistence.*;

@Entity
@Table(name = "songs")
public class Song {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    private String coverUrl;
    private String audioUrl;
    private String duration;
    private String previewUrl;

    /** Spotify track ID — used to deduplicate when saving processed songs. */
    @Column(unique = true)
    private String spotifyId;

    /** Epoch-ms set on first persist — used to order "recent" songs. */
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = System.currentTimeMillis();
    }

    public Song() {}

    // ── Getters & setters ──────────────────────────────────────────────────────

    public String getId()          { return id; }
    public void   setId(String v)  { this.id = v; }

    public String getTitle()           { return title; }
    public void   setTitle(String v)   { this.title = v; }

    public String getArtist()          { return artist; }
    public void   setArtist(String v)  { this.artist = v; }

    public String getCoverUrl()            { return coverUrl; }
    public void   setCoverUrl(String v)    { this.coverUrl = v; }

    public String getAudioUrl()            { return audioUrl; }
    public void   setAudioUrl(String v)    { this.audioUrl = v; }

    public String getDuration()            { return duration; }
    public void   setDuration(String v)    { this.duration = v; }

    public String getPreviewUrl()          { return previewUrl; }
    public void   setPreviewUrl(String v)  { this.previewUrl = v; }

    public String getSpotifyId()           { return spotifyId; }
    public void   setSpotifyId(String v)   { this.spotifyId = v; }

    public Long getCreatedAt()             { return createdAt; }
    public void setCreatedAt(Long v)       { this.createdAt = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Song other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }
}
