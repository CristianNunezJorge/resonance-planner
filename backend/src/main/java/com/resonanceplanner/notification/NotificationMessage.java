package com.resonanceplanner.notification;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** What the server sends on the WebSocket (D135): the frontend shows the message as plain text. */
@JsonPropertyOrder({"type", "message"})
public record NotificationMessage(NotificationType type, String message) {}
