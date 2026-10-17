package com.resonanceplanner.agent.chat;

import com.resonanceplanner.agent.ChatMessage;
import com.resonanceplanner.agent.ChatMessage.Speaker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A message of the chat with the agent, only its text (D40): one of the user or one answer of the agent, which links
 * the proposal it made, if any. A message never changes after it is saved.
 */
@Entity
@Table(name = "agent_message")
public class AgentMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Speaker role;

    @Column(nullable = false, updatable = false)
    private String content;

    @Column(updatable = false)
    private Long proposalId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentMessage() {
        // for JPA
    }

    public static AgentMessage ofUser(Long userId, String content, Instant createdAt) {
        return create(userId, Speaker.USER, content, null, createdAt);
    }

    public static AgentMessage ofAgent(Long userId, String content, Long proposalId, Instant createdAt) {
        return create(userId, Speaker.ASSISTANT, content, proposalId, createdAt);
    }

    private static AgentMessage create(Long userId, Speaker role, String content, Long proposalId, Instant createdAt) {
        AgentMessage message = new AgentMessage();
        message.userId = userId;
        message.role = role;
        message.content = content;
        message.proposalId = proposalId;
        message.createdAt = createdAt;
        return message;
    }

    /** The message as the agent receives it in the history (D58). */
    public ChatMessage toChatMessage() {
        return new ChatMessage(role, content);
    }

    public Long getId() {
        return id;
    }

    public Speaker getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
