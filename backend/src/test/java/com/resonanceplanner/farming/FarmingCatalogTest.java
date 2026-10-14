package com.resonanceplanner.farming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resonanceplanner.farming.FarmingCatalog.Ascension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// The plain values of the catalog cannot change after they are built, and cannot make the estimate divide by zero
class FarmingCatalogTest {

    @Test
    void keepsItsOwnCopyOfTheAscensions() {
        List<Ascension> ascensions = new ArrayList<>(List.of(new Ascension(1, Map.of("shell-credit", 5000))));
        FarmingCatalog catalog = catalogWith(Map.of("jinhsi", ascensions), 240);

        ascensions.add(new Ascension(2, Map.of("shell-credit", 10000)));

        assertThatThrownBy(() -> catalog.ascensions().get("jinhsi").add(new Ascension(3, Map.of())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(catalog.ascensions().get("jinhsi")).hasSize(1);
    }

    @Test
    void refusesADayWithoutWaveplate() {
        assertThatThrownBy(() -> catalogWith(Map.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Waveplate");
    }

    private static FarmingCatalog catalogWith(Map<String, List<Ascension>> ascensions, int waveplatesPerDay) {
        return new FarmingCatalog(Map.of(), ascensions, Map.of(), List.of(), new BigDecimal("0.35"), waveplatesPerDay);
    }
}
