package com.resonanceplanner.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.agent.client.ToolDefinition;
import com.resonanceplanner.agent.tools.AgentToolDefinitions;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AgentPromptTest {

    // The instructions go with every call to the model, up to 6 per message (D58, D128): they stay short
    private static final int MAX_PROMPT_CHARS = 4_000;

    private static final Pattern TOOL_NAME = Pattern.compile("\\b[a-z]+(?:_[a-z]+)+\\b");

    private final List<String> toolNames = new AgentToolDefinitions(JsonMapper.builder().build()).all().stream()
            .map(ToolDefinition::name)
            .toList();

    @Test
    void tellsWhenToUseEachOfTheFiveTools() {
        assertThat(toolNames).hasSize(5).allSatisfy(name -> assertThat(AgentPrompt.SYSTEM).contains(name));
    }

    @Test
    void namesOnlyToolsThatExist() {
        List<String> named = TOOL_NAME.matcher(AgentPrompt.SYSTEM).results().map(MatchResult::group).toList();

        // A renamed tool must be renamed in the instructions too, or the model asks for a tool that does not exist
        assertThat(named).isNotEmpty().allSatisfy(name -> assertThat(toolNames).contains(name));
    }

    @Test
    void usesNoMarkdownItself() {
        // The model tends to copy the style of the instructions, and the chat shows plain text (D130)
        assertThat(AgentPrompt.SYSTEM).doesNotContain("**", "__", "`", "#");
    }

    @Test
    void staysShort() {
        assertThat(AgentPrompt.SYSTEM).hasSizeLessThanOrEqualTo(MAX_PROMPT_CHARS);
    }
}
