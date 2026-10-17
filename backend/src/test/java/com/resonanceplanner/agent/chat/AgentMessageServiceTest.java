package com.resonanceplanner.agent.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// The saved answer has at most 4,000 characters, because it goes back to the model with every new message (D129)
class AgentMessageServiceTest {

    @Test
    void keepsAnAnswerThatFits() {
        String answer = "a".repeat(AgentMessageService.MAX_SAVED_REPLY_CHARS);

        assertThat(AgentMessageService.capped(answer)).isEqualTo(answer);
    }

    @Test
    void cutsALongerAnswerAndSaysThatItWasCut() {
        String capped = AgentMessageService.capped("a".repeat(5_000));

        assertThat(capped).hasSize(AgentMessageService.MAX_SAVED_REPLY_CHARS).isEqualTo("a".repeat(3_999) + "…");
    }

    @Test
    void neverCutsACharacterMadeOfTwoHalves() {
        // An emoji is two chars in Java: a cut between them would leave half a character
        String answer = "a".repeat(3_998) + "😀" + "b".repeat(10);

        String capped = AgentMessageService.capped(answer);

        assertThat(capped).isEqualTo("a".repeat(3_998) + "…");
    }
}
