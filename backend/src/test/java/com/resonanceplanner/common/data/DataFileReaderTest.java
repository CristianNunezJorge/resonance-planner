package com.resonanceplanner.common.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class DataFileReaderTest {

    // Integer, not int: a missing int already fails on its own (Jackson 3 rejects null for primitives)
    record Sample(String name, Integer quantity) {}

    private final DataFileReader reader = new DataFileReader(JsonMapper.builder().build());

    @Test
    void readsAFileOfTheDataFolder() {
        Sample sample = reader.read("test-valid.json", new TypeReference<Sample>() {});

        assertThat(sample).isEqualTo(new Sample("Shell Credit", 3));
    }

    @Test
    void stopsOnAMistypedKeyAndNamesTheFile() {
        assertThatThrownBy(() -> reader.read("test-unknown-key.json", new TypeReference<Sample>() {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("test-unknown-key.json")
                .hasMessageContaining("quantiy");
    }

    @Test
    void stopsOnMalformedJsonAndNamesTheFile() {
        assertThatThrownBy(() -> reader.read("test-malformed.json", new TypeReference<Sample>() {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("test-malformed.json");
    }
}
