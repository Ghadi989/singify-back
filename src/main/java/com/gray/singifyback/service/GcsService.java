package com.gray.singifyback.service;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GcsService {

    private static final Logger log = LoggerFactory.getLogger(GcsService.class);

    private final Storage storage;
    private final String bucketName;
    private final boolean enabled;

    public GcsService(
            @Value("${gcs.bucket-name:}") String bucketName,
            @Value("${gcs.credentials-path:}") String credentialsPath) {

        Storage s = null;
        boolean ok = false;

        if (!bucketName.isBlank() && !credentialsPath.isBlank()) {
            try {
                String resolvedPath = credentialsPath.replace("classpath:", "");
                InputStream credStream;
                if (credentialsPath.startsWith("classpath:")) {
                    credStream = getClass().getClassLoader().getResourceAsStream(resolvedPath);
                } else {
                    credStream = new FileInputStream(resolvedPath);
                }
                if (credStream != null) {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(credStream)
                            .createScoped("https://www.googleapis.com/auth/cloud-platform");
                    s = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
                    ok = true;
                    log.info("GCS enabled — bucket: {}", bucketName);
                }
            } catch (Exception e) {
                log.warn("GCS unavailable ({}), audio will stream directly from YouTube", e.getMessage());
            }
        } else {
            log.info("GCS not configured — audio will stream directly from YouTube");
        }

        this.storage = s;
        this.bucketName = bucketName;
        this.enabled = ok;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean exists(String key) {
        try {
            return storage.get(bucketName, key) != null;
        } catch (Exception e) {
            log.warn("GCS exists check failed for {}: {}", key, e.getMessage());
            return false;
        }
    }

    public void upload(String key, byte[] data, String contentType) {
        BlobId blobId = BlobId.of(bucketName, key);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, data);
        log.info("Uploaded {} bytes to GCS key: {}", data.length, key);
    }

    public String getSignedUrl(String key) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, key)).build();
        URL url = storage.signUrl(blobInfo, 6, TimeUnit.HOURS,
                Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }

    public List<Map<String, Object>> listFiles() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!enabled) return result;
        try {
            Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix("audio/"));
            for (Blob blob : blobs.iterateAll()) {
                result.add(Map.of(
                        "key",  blob.getName(),
                        "size", blob.getSize(),
                        "updated", blob.getUpdateTimeOffsetDateTime().toString()
                ));
            }
        } catch (Exception e) {
            log.error("GCS list failed: {}", e.getMessage());
        }
        return result;
    }

    public boolean delete(String key) {
        if (!enabled) return false;
        try {
            boolean deleted = storage.delete(bucketName, key);
            if (deleted) log.info("Deleted GCS key: {}", key);
            return deleted;
        } catch (Exception e) {
            log.error("GCS delete failed for {}: {}", key, e.getMessage());
            return false;
        }
    }

    /** Normalise artist+title into a safe GCS object key. */
    public static String toKey(String artist, String title) {
        String safe = (artist + "-" + title)
                .toLowerCase()
                .replaceAll("[^a-z0-9\\-]", "_")
                .replaceAll("_+", "_");
        return "audio/" + safe + ".mp3";
    }
}
