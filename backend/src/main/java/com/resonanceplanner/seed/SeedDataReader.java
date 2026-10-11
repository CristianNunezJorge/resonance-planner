package com.resonanceplanner.seed;

import com.resonanceplanner.common.data.DataFileReader;
import com.resonanceplanner.seed.SeedData.CharacterData;
import com.resonanceplanner.seed.SeedData.EventData;
import com.resonanceplanner.seed.SeedData.FarmingSourceData;
import com.resonanceplanner.seed.SeedData.LevelExpData;
import com.resonanceplanner.seed.SeedData.MaterialData;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/** Reads the five JSON files of the catalog and of the demo events (D26). */
@Component
public class SeedDataReader {

    private final DataFileReader dataFileReader;

    public SeedDataReader(DataFileReader dataFileReader) {
        this.dataFileReader = dataFileReader;
    }

    public SeedData read() {
        return new SeedData(
                dataFileReader.read("materials.json", new TypeReference<List<MaterialData>>() {}),
                dataFileReader.read("farming-sources.json", new TypeReference<List<FarmingSourceData>>() {}),
                dataFileReader.read("level-exp.json", new TypeReference<List<LevelExpData>>() {}),
                dataFileReader.read("characters.json", new TypeReference<List<CharacterData>>() {}),
                dataFileReader.read("events.json", new TypeReference<List<EventData>>() {}));
    }
}
