package com.resonanceplanner.seed;

import com.resonanceplanner.catalog.Element;
import com.resonanceplanner.catalog.FarmingSourceType;
import com.resonanceplanner.catalog.MaterialCategory;
import com.resonanceplanner.catalog.WeaponType;
import com.resonanceplanner.event.ActivityFrequency;
import com.resonanceplanner.event.EffectType;
import com.resonanceplanner.event.EventType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

/**
 * Content of the JSON files in data/, with the same names and shape (docs/03-dati-demo.md, D26).
 * Lists missing from a file become empty and read-only.
 */
public record SeedData(
        List<MaterialData> materials,
        List<FarmingSourceData> farmingSources,
        List<LevelExpData> levels,
        List<CharacterData> characters,
        List<EventData> events) {

    public SeedData {
        materials = readOnly(materials);
        farmingSources = readOnly(farmingSources);
        levels = readOnly(levels);
        characters = readOnly(characters);
        events = readOnly(events);
    }

    public record MaterialData(String id, String name, MaterialCategory category, Integer exp, String icon) {}

    public record FarmingSourceData(String id, String name, FarmingSourceType type, Integer waveplateCost,
            Integer minutesPerRun, List<DropData> drops) {

        public FarmingSourceData {
            drops = readOnly(drops);
        }
    }

    public record DropData(String materialId, BigDecimal avgQuantity, boolean estimated) {}

    public record LevelExpData(Integer level, Integer expToNext) {}

    public record CharacterData(String id, String name, Element element, WeaponType weaponType, Integer rarity,
            String role, String icon, List<AscensionData> ascensions) {

        public CharacterData {
            ascensions = readOnly(ascensions);
        }
    }

    // atLevel documents the file; the levels used by the calculations are GameRules.ascensionLevels
    public record AscensionData(Integer toRank, Integer atLevel, List<CostData> cost) {

        public AscensionData {
            cost = readOnly(cost);
        }
    }

    public record CostData(String materialId, Integer quantity) {}

    // "id" is the code that identifies a demo event in the database
    public record EventData(String id, String name, EventType type, String description, ScheduleData schedule,
            List<String> rewards, List<ActivityData> activities, EffectData effect) {

        public EventData {
            rewards = readOnly(rewards);
            activities = readOnly(activities);
        }
    }

    // activeWeekdays null = active every day (D96)
    public record ScheduleData(int startOffsetDays, int durationDays, int repeatEveryDays,
            List<DayOfWeek> activeWeekdays) {}

    public record ActivityData(String title, int minutes, ActivityFrequency frequency, Integer unlockDay) {}

    public record EffectData(EffectType type, FarmingSourceType sourceType, BigDecimal multiplier, Integer dailyLimit) {}

    private static <T> List<T> readOnly(List<T> list) {
        return list == null ? List.of() : List.copyOf(list);
    }
}
