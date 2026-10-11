package com.resonanceplanner.seed;

import com.resonanceplanner.catalog.AscensionCost;
import com.resonanceplanner.catalog.AscensionCostRepository;
import com.resonanceplanner.catalog.FarmingDrop;
import com.resonanceplanner.catalog.FarmingDropRepository;
import com.resonanceplanner.catalog.FarmingSource;
import com.resonanceplanner.catalog.FarmingSourceRepository;
import com.resonanceplanner.catalog.GameCharacter;
import com.resonanceplanner.catalog.GameCharacterRepository;
import com.resonanceplanner.catalog.LevelExp;
import com.resonanceplanner.catalog.LevelExpRepository;
import com.resonanceplanner.catalog.Material;
import com.resonanceplanner.catalog.MaterialRepository;
import com.resonanceplanner.seed.SeedData.CharacterData;
import com.resonanceplanner.seed.SeedData.FarmingSourceData;
import com.resonanceplanner.seed.SeedData.LevelExpData;
import com.resonanceplanner.seed.SeedData.MaterialData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brings the catalog in line with the JSON files at every startup (D39).
 * Materials, sources, levels and characters are updated by id, so the rows that user data points to
 * never change identity. Drops and ascension costs are deleted and inserted again: no other table uses them.
 * Entries removed from the JSON files stay in the database, because user data may still refer to them:
 * a warning in the log lists them.
 */
@Component
public class CatalogSeeder {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private final MaterialRepository materialRepository;
    private final FarmingSourceRepository farmingSourceRepository;
    private final FarmingDropRepository farmingDropRepository;
    private final LevelExpRepository levelExpRepository;
    private final GameCharacterRepository gameCharacterRepository;
    private final AscensionCostRepository ascensionCostRepository;

    public CatalogSeeder(MaterialRepository materialRepository, FarmingSourceRepository farmingSourceRepository,
            FarmingDropRepository farmingDropRepository, LevelExpRepository levelExpRepository,
            GameCharacterRepository gameCharacterRepository, AscensionCostRepository ascensionCostRepository) {
        this.materialRepository = materialRepository;
        this.farmingSourceRepository = farmingSourceRepository;
        this.farmingDropRepository = farmingDropRepository;
        this.levelExpRepository = levelExpRepository;
        this.gameCharacterRepository = gameCharacterRepository;
        this.ascensionCostRepository = ascensionCostRepository;
    }

    /** All or nothing: if a file is wrong, the catalog stays as it was and the application stops. */
    @Transactional
    public void seed(SeedData data) {
        Map<String, Material> materials = seedMaterials(data.materials());
        seedFarmingSources(data.farmingSources(), materials);
        seedLevels(data.levels());
        seedCharacters(data.characters(), materials);
        warnAboutEntriesNoLongerInTheFiles(data);
        log.info("Catalog loaded: {} materials, {} farming sources, {} levels, {} characters",
                materials.size(), data.farmingSources().size(), data.levels().size(), data.characters().size());
    }

    // A renamed or removed entry stays in the database without drops or costs: the log says which ones
    private void warnAboutEntriesNoLongerInTheFiles(SeedData data) {
        warnIfMissing("Materials", materialRepository.findAll().stream().map(Material::getId),
                data.materials().stream().map(MaterialData::id));
        warnIfMissing("Farming sources", farmingSourceRepository.findAll().stream().map(FarmingSource::getId),
                data.farmingSources().stream().map(FarmingSourceData::id));
        warnIfMissing("Characters", gameCharacterRepository.findAll().stream().map(GameCharacter::getId),
                data.characters().stream().map(CharacterData::id));
    }

    private static void warnIfMissing(String kind, Stream<String> idsInDatabase, Stream<String> idsInFiles) {
        Set<String> inFiles = idsInFiles.collect(Collectors.toSet());
        List<String> missing = idsInDatabase.filter(id -> !inFiles.contains(id)).sorted().toList();
        if (!missing.isEmpty()) {
            log.warn("{} in the database but no longer in the JSON files: {}", kind, missing);
        }
    }

    private Map<String, Material> seedMaterials(List<MaterialData> items) {
        Map<String, Material> materialsById = new HashMap<>();
        for (MaterialData item : items) {
            Material material = materialRepository.findById(item.id()).orElseGet(() -> new Material(item.id()));
            material.update(item.name(), item.category(), item.exp(), item.icon());
            materialsById.put(item.id(), materialRepository.save(material));
        }
        return materialsById;
    }

    private void seedFarmingSources(List<FarmingSourceData> items, Map<String, Material> materials) {
        farmingDropRepository.deleteAllInBatch();
        List<FarmingDrop> drops = new ArrayList<>();
        for (FarmingSourceData item : items) {
            FarmingSource source = farmingSourceRepository.findById(item.id()).orElseGet(() -> new FarmingSource(item.id()));
            source.update(item.name(), item.type(), item.waveplateCost(), item.minutesPerRun());
            FarmingSource saved = farmingSourceRepository.save(source);
            item.drops().forEach(drop -> drops.add(new FarmingDrop(saved,
                    requireMaterial(materials, drop.materialId(), "farming source " + item.id()),
                    drop.avgQuantity(), drop.estimated())));
        }
        farmingDropRepository.saveAll(drops);
    }

    private void seedLevels(List<LevelExpData> items) {
        for (LevelExpData item : items) {
            LevelExp level = levelExpRepository.findById(item.level()).orElseGet(() -> new LevelExp(item.level()));
            level.update(item.expToNext());
            levelExpRepository.save(level);
        }
    }

    private void seedCharacters(List<CharacterData> items, Map<String, Material> materials) {
        ascensionCostRepository.deleteAllInBatch();
        List<AscensionCost> costs = new ArrayList<>();
        for (CharacterData item : items) {
            GameCharacter character = gameCharacterRepository.findById(item.id()).orElseGet(() -> new GameCharacter(item.id()));
            character.update(item.name(), item.element(), item.weaponType(), item.rarity(), item.role(), item.icon());
            GameCharacter saved = gameCharacterRepository.save(character);
            item.ascensions().forEach(ascension -> ascension.cost().forEach(cost -> costs.add(new AscensionCost(saved,
                    ascension.toRank(),
                    requireMaterial(materials, cost.materialId(), "character " + item.id()),
                    cost.quantity()))));
        }
        ascensionCostRepository.saveAll(costs);
    }

    private static Material requireMaterial(Map<String, Material> materials, String materialId, String usedBy) {
        Material material = materials.get(materialId);
        if (material == null) {
            throw new IllegalStateException(
                    "Unknown material '" + materialId + "' in " + usedBy + ": check materials.json and the files that use it");
        }
        return material;
    }
}
