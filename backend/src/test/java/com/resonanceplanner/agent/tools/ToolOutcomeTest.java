package com.resonanceplanner.agent.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ToolOutcomeTest {

    @Test
    void aToolThatRanHasAStepAndAnErrorHasNone() {
        assertThat(ToolOutcome.success("{}", "Ho calcolato il piano").step()).isEqualTo("Ho calcolato il piano");
        assertThat(ToolOutcome.failure("{\"error\": \"x\"}").step()).isNull();
        assertThatThrownBy(() -> new ToolOutcome("{}", false, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ToolOutcome("{}", true, "Ho calcolato il piano"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ToolOutcome.success(null, "Passo")).isInstanceOf(NullPointerException.class);
    }
}
