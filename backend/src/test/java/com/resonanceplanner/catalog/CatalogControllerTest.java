package com.resonanceplanner.catalog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Integration test: real security, real catalog loaded from the JSON files into the test database
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsTheCharactersSortedByNameWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/game/characters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.data[0].id").value("aalto"))
                .andExpect(jsonPath("$.data[?(@.id == 'jinhsi')].element").value(contains("SPECTRO")))
                .andExpect(jsonPath("$.data[?(@.id == 'jinhsi')].icon").value(contains("characters/jinhsi.webp")));
    }

    @Test
    void showsACharacterWithItsAscensionCosts() throws Exception {
        mockMvc.perform(get("/api/game/characters/jinhsi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Jinhsi"))
                .andExpect(jsonPath("$.data.ascensions", hasSize(2)))
                .andExpect(jsonPath("$.data.ascensions[0].toRank").value(1))
                .andExpect(jsonPath("$.data.ascensions[0].atLevel").value(20))
                .andExpect(jsonPath("$.data.ascensions[0].costs[?(@.materialId == 'shell-credit')].quantity")
                        .value(contains(5000)))
                .andExpect(jsonPath("$.data.ascensions[0].costs[?(@.materialId == 'shell-credit')].materialName")
                        .value(contains("Shell Credit")))
                .andExpect(jsonPath("$.data.ascensions[1].atLevel").value(40));
    }

    @Test
    void answersNotFoundForAnUnknownCharacter() throws Exception {
        mockMvc.perform(get("/api/game/characters/rover"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Personaggio non trovato."));
    }

    @Test
    void listsTheMaterials() throws Exception {
        mockMvc.perform(get("/api/game/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(22)))
                .andExpect(jsonPath("$.data[?(@.id == 'premium-resonance-potion')].category").value(contains("CHARACTER_EXP")))
                .andExpect(jsonPath("$.data[?(@.id == 'premium-resonance-potion')].exp").value(contains(20000)));
    }

    @Test
    void listsTheFarmingSourcesWithTheirDrops() throws Exception {
        mockMvc.perform(get("/api/game/farming-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(16)))
                .andExpect(jsonPath("$.data[?(@.id == 'sim-resonance-potion')].waveplateCost").value(contains(40)))
                .andExpect(jsonPath("$.data[?(@.id == 'sim-resonance-potion')].drops[?(@.materialId == 'premium-resonance-potion')].avgQuantity")
                        .value(contains(1.1)))
                .andExpect(jsonPath("$.data[?(@.id == 'overworld-howlers')].drops[0].estimated").value(contains(true)));
    }

    @Test
    void showsTheGameRules() throws Exception {
        mockMvc.perform(get("/api/game/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waveplate.max").value(240))
                .andExpect(jsonPath("$.data.waveplate.regenMinutes").value(6))
                .andExpect(jsonPath("$.data.waveplateCrystals.max").value(480))
                .andExpect(jsonPath("$.data.ascensionLevels", contains(20, 40)))
                .andExpect(jsonPath("$.data.maxLevel").value(40))
                .andExpect(jsonPath("$.data.playTimeOptionsMinutes", contains(30, 60, 120)))
                // The admin form counts the offset of a recurring event from this day (D29, D122)
                .andExpect(jsonPath("$.data.demoEventsAnchorDate").value("2026-10-01"));
    }
}
