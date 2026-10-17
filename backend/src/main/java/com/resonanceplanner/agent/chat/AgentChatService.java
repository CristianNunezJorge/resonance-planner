package com.resonanceplanner.agent.chat;

import com.resonanceplanner.agent.AgentLoop;
import com.resonanceplanner.agent.AgentReply;
import com.resonanceplanner.agent.ChatMessage;
import com.resonanceplanner.agent.chat.dto.AgentAnswerResponse;
import com.resonanceplanner.agent.chat.dto.AgentMessageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * A message to the agent from start to end (D52, D129): one turn at a time for each user, the daily limits, the loop
 * of the agent and then the saving. No transaction around the loop (D128): it would stay open while the model thinks.
 * If the loop fails, nothing is saved and the message does not count in the limit of the user (D61).
 */
@Service
public class AgentChatService {

    private final AgentTurnGate gate;
    private final AgentMessageService messageService;
    private final AgentQuota quota;
    private final AgentLoop agentLoop;
    private final Clock clock;

    public AgentChatService(AgentTurnGate gate, AgentMessageService messageService, AgentQuota quota,
            AgentLoop agentLoop, Clock clock) {
        this.gate = gate;
        this.messageService = messageService;
        this.quota = quota;
        this.agentLoop = agentLoop;
        this.clock = clock;
    }

    public AgentAnswerResponse send(Long userId, String content) {
        try (AgentTurnGate.Pass turn = gate.enter(userId)) {
            List<ChatMessage> history = messageService.recent(userId);
            quota.requireRoom(userId);
            Instant sentAt = clock.instant();
            AgentReply reply = agentLoop.answer(userId, history, content);
            AgentMessageResponse answer = messageService.saveTurn(userId, content, sentAt, reply);
            return new AgentAnswerResponse(answer, reply.proposal(), reply.steps(), quota.usage(userId));
        }
    }
}
