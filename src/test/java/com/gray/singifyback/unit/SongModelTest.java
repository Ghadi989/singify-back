package com.gray.singifyback.unit;

import com.gray.singifyback.model.Song;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SongModelTest {

    @Test
    void equals_sameId_areEqual() {
        Song a = new Song("id-1", "Hard Times", "Paramore", null, "url", "2:57");
        Song b = new Song("id-1", "Hard Times", "Paramore", null, "url", "2:57");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentId_areNotEqual() {
        Song a = new Song("id-1", "Hard Times", "Paramore", null, "url", "2:57");
        Song b = new Song("id-2", "Hard Times", "Paramore", null, "url", "2:57");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_nullId_notEqualToOther() {
        Song a = new Song();
        Song b = new Song("id-1", "Hard Times", "Paramore", null, "url", "2:57");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_sameId_isSame() {
        Song a = new Song("id-1", "Hard Times", "Paramore", null, "url", "2:57");
        Song b = new Song("id-1", "Hard Times", "Paramore", null, "url", "2:57");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
