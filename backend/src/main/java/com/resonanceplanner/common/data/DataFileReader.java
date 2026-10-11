package com.resonanceplanner.common.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the JSON files of the data/ folder (D26). Stricter than the API: a mistyped key stops the
 * startup instead of becoming an empty value, and every error names the file.
 */
@Component
public class DataFileReader {

    private static final String DATA_FOLDER = "data/";

    private final JsonMapper jsonMapper;

    public DataFileReader(JsonMapper jsonMapper) {
        // Copy of Spring's mapper: Spring ignores unknown keys, our own files must not have any
        this.jsonMapper = jsonMapper.rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public <T> T read(String fileName, TypeReference<T> type) {
        String path = DATA_FOLDER + fileName;
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return jsonMapper.readValue(input, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        } catch (JacksonException e) {
            throw new IllegalStateException(path + " is not valid: " + e.getOriginalMessage(), e);
        }
    }
}
