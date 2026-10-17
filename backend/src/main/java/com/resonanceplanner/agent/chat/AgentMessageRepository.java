package com.resonanceplanner.agent.chat;

import com.resonanceplanner.agent.ChatMessage.Speaker;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.repository.Repository;

/** The chat of a user: like the other data of the user, every query takes the user (D44). */
public interface AgentMessageRepository extends Repository<AgentMessage, Long> {

    AgentMessage save(AgentMessage message);

    /** The newest messages of the user, from the newest. */
    List<AgentMessage> findByUserIdOrderByIdDesc(Long userId, Limit limit);

    /** The messages of the user older than the given one, from the newest: the next page of the history. */
    List<AgentMessage> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long before, Limit limit);

    /** The messages the user wrote since a moment: the daily limit counts them (D40, D129). */
    long countByUserIdAndRoleAndCreatedAtGreaterThanEqual(Long userId, Speaker role, Instant since);
}
