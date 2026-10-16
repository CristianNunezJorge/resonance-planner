package com.resonanceplanner.farming;

import com.resonanceplanner.catalog.AscensionCost;
import com.resonanceplanner.catalog.AscensionCostRepository;
import com.resonanceplanner.catalog.FarmingDrop;
import com.resonanceplanner.catalog.FarmingSource;
import com.resonanceplanner.catalog.FarmingSourceRepository;
import com.resonanceplanner.catalog.GameRules;
import com.resonanceplanner.catalog.LevelExp;
import com.resonanceplanner.catalog.LevelExpRepository;
import com.resonanceplanner.catalog.Material;
import com.resonanceplanner.catalog.MaterialRepository;
import com.resonanceplanner.farming.FarmingCatalog.Ascension;
import com.resonanceplanner.farming.FarmingCatalog.Drop;
import com.resonanceplanner.farming.FarmingCatalog.MaterialInfo;
import com.resonanceplanner.farming.FarmingCatalog.Source;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * Reads from the database the part of the catalog that the farming calculation needs, in four queries. It must run
 * inside a transaction: the drops of the sources are entities that become plain values here.
 */
@Component
public class FarmingCatalogLoader {

    private static final int MINUTES_PER_DAY = 24 * 60;

    private final LevelExpRepository levelExpRepository;
    private final AscensionCostRepository ascensionCostRepository;
    private final MaterialRepository materialRepository;
    private final FarmingSourceRepository farmingSourceRepository;
    private final GameRules gameRules;

    public FarmingCatalogLoader(LevelExpRepository levelExpRepository,
            AscensionCostRepository ascensionCostRepository, MaterialRepository materialRepository,
            FarmingSourceRepository farmingSourceRepository, GameRules gameRules) {
        this.levelExpRepository = levelExpRepository;
        this.ascensionCostRepository = ascensionCostRepository;
        this.materialRepository = materialRepository;
        this.farmingSourceRepository = farmingSourceRepository;
        this.gameRules = gameRules;
    }

    public FarmingCatalog load() {
        Map<Integer, Integer> expToNext = new HashMap<>();
        for (LevelExp level : levelExpRepository.findAll()) {
            expToNext.put(level.getLevel(), level.getExpToNext());
        }
        Map<String, MaterialInfo> materials = new HashMap<>();
        for (Material material : materialRepository.findAll()) {
            materials.put(material.getId(),
                    new MaterialInfo(material.getId(), material.getName(), material.getCategory(), material.getExp()));
        }
        List<Source> sources = farmingSourceRepository.findAllByOrderByTypeAscNameAsc().stream()
                .map(FarmingCatalogLoader::toSource)
                .toList();
        // 240: the Waveplate that regenerate in a day, one every 6 minutes (D28)
        int waveplatesPerDay = MINUTES_PER_DAY / gameRules.waveplate().regenMinutes();
        return new FarmingCatalog(expToNext, ascensions(), materials, sources, gameRules.shellCreditPerExp(),
                waveplatesPerDay);
    }

    // The costs are one row per material: grouped by character, then by rank
    private Map<String, List<Ascension>> ascensions() {
        Map<String, Map<Integer, Map<String, Integer>>> costs = new HashMap<>();
        for (AscensionCost cost : ascensionCostRepository.findAllByOrderByIdAsc()) {
            costs.computeIfAbsent(cost.getCharacter().getId(), id -> new TreeMap<>())
                    .computeIfAbsent(cost.getToRank(), rank -> new HashMap<>())
                    .put(cost.getMaterial().getId(), cost.getQuantity());
        }
        Map<String, List<Ascension>> ascensions = new HashMap<>();
        costs.forEach((characterId, byRank) -> {
            List<Ascension> list = new ArrayList<>();
            byRank.forEach((rank, cost) -> list.add(new Ascension(rank, cost)));
            ascensions.put(characterId, list);
        });
        return ascensions;
    }

    private static Source toSource(FarmingSource source) {
        List<Drop> drops = new ArrayList<>();
        for (FarmingDrop drop : source.getDrops()) {
            drops.add(new Drop(drop.getMaterial().getId(), drop.getAvgQuantity(), drop.isEstimated()));
        }
        return new Source(source.getId(), source.getName(), source.getType(), source.getWaveplateCost(),
                source.getMinutesPerRun(), drops);
    }
}
