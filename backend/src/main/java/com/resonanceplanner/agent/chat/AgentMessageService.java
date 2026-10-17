package com.resonanceplanner.agent.chat;

import com.resonanceplanner.agent.AgentLoop;
import com.resonanceplanner.agent.AgentReply;
import com.resonanceplanner.agent.ChatMessage;
import com.resonanceplanner.agent.chat.dto.AgentMessagePage;
import com.resonanceplanner.agent.chat.dto.AgentMessageResponse;
import com.resonanceplanner.common.api.PageMeta;
import com.resonanceplanner.common.error.UnauthorizedException;
import com.resonanceplanner.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The saved chat of the user of the token (D40): the history for the agent and for the page, and the two messages of
 * a turn, saved in one short transaction after the answer (D128). It answers 401 if the user does not exist anymore
 * (D102).
 */
@Service
@Transactional
public class AgentMessageService {

    /** The saved answer goes back to the model with every new message, so it has a ceiling (D129). */
    static final int MAX_SAVED_REPLY_CHARS = 4_000;
    static final String CUT_MARK = "…";

    private final AgentMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AgentMessageService(AgentMessageRepository messageRepository, UserRepository userRepository, Clock clock) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /** The last messages of the user from the oldest, the history the agent receives (D58). */
    @Transactional(readOnly = true)
    public List<ChatMessage> recent(Long userId) {
        requireUser(userId);
        List<AgentMessage> newest =
                messageRepository.findByUserIdOrderByIdDesc(userId, Limit.of(AgentLoop.MAX_HISTORY));
        return newest.reversed().stream().map(AgentMessage::toChatMessage).toList();
    }

    /**
     * Saves the message of the user and the answer of the agent, linked to the proposal of the turn if there is one.
     * The user is locked (D118), so the account cannot be deleted halfway.
     */
    public AgentMessageResponse saveTurn(Long userId, String content, Instant sentAt, AgentReply reply) {
        userRepository.lockById(userId).orElseThrow(UnauthorizedException::new);
        messageRepository.save(AgentMessage.ofUser(userId, content, sentAt));
        Long proposalId = reply.proposal() == null ? null : reply.proposal().id();
        AgentMessage answer = messageRepository.save(
                AgentMessage.ofAgent(userId, capped(reply.text()), proposalId, clock.instant()));
        return AgentMessageResponse.from(answer);
    }

    /** One page of the history, from the newest message; before is the id of the oldest message already shown. */
    @Transactional(readOnly = true)
    public AgentMessagePage page(Long userId, Long before, int limit) {
        requireUser(userId);
        // One more than asked tells whether older messages follow
        Limit oneMore = Limit.of(limit + 1);
        List<AgentMessage> found = before == null
                ? messageRepository.findByUserIdOrderByIdDesc(userId, oneMore)
                : messageRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, before, oneMore);
        boolean hasMore = found.size() > limit;
        List<AgentMessageResponse> messages =
                found.stream().limit(limit).map(AgentMessageResponse::from).toList();
        return new AgentMessagePage(messages, new PageMeta(limit, hasMore));
    }

    /** The answer within the ceiling, with a mark when it is cut; a character made of two halves is never split. */
    static String capped(String text) {
        if (text.length() <= MAX_SAVED_REPLY_CHARS) {
            return text;
        }
        int end = MAX_SAVED_REPLY_CHARS - CUT_MARK.length();
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end) + CUT_MARK;
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException();
        }
    }
}
