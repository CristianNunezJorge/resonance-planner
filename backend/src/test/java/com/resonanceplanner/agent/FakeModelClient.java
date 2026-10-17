package com.resonanceplanner.agent;

import com.resonanceplanner.agent.client.FinishReason;
import com.resonanceplanner.agent.client.ModelClient;
import com.resonanceplanner.agent.client.ModelReply;
import com.resonanceplanner.agent.client.ModelRequest;
import com.resonanceplanner.agent.client.TokenUsage;
import com.resonanceplanner.agent.client.ToolCall;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A model client without network (D63): it gives the prepared replies in order and keeps every request it got. */
public final class FakeModelClient implements ModelClient {

    public static final TokenUsage TEXT_TOKENS = new TokenUsage(900, 40, 600);
    public static final TokenUsage TOOL_TOKENS = new TokenUsage(800, 15, 600);

    private final Deque<ModelReply> replies = new ArrayDeque<>();
    private final List<ModelRequest> requests = new ArrayList<>();
    private RuntimeException failure;
    private Runnable onEachCall = () -> {};

    public FakeModelClient answering(ModelReply... next) {
        replies.addAll(List.of(next));
        return this;
    }

    /** After the prepared replies, every call fails with this error. */
    public FakeModelClient thenFailingWith(RuntimeException error) {
        failure = error;
        return this;
    }

    /** Runs at every call, for example to move a test clock as if the model took time. */
    public FakeModelClient onEachCall(Runnable action) {
        onEachCall = action;
        return this;
    }

    public List<ModelRequest> requests() {
        return List.copyOf(requests);
    }

    @Override
    public ModelReply send(ModelRequest request) {
        requests.add(request);
        onEachCall.run();
        if (replies.isEmpty() && failure != null) {
            throw failure;
        }
        if (replies.isEmpty()) {
            throw new AssertionError("The model was called more times than prepared");
        }
        return replies.removeFirst();
    }

    public static ModelReply text(String text) {
        return new ModelReply(text, List.of(), FinishReason.STOP, TEXT_TOKENS);
    }

    public static ModelReply ending(String text, FinishReason reason) {
        return new ModelReply(text, List.of(), reason, TEXT_TOKENS);
    }

    public static ModelReply tools(ToolCall... calls) {
        return new ModelReply("", List.of(calls), FinishReason.TOOL_CALLS, TOOL_TOKENS);
    }

    public static ToolCall call(String id, String name, String arguments) {
        return new ToolCall(id, name, arguments);
    }
}
