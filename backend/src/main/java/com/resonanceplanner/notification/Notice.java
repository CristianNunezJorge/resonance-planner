package com.resonanceplanner.notification;

/**
 * A notification for one user (D135). The subject says what it is about (the Waveplate, one edition of an event) and
 * the occasion when: the server sends one notice per subject and occasion, so the same one never arrives twice.
 */
public record Notice(NotificationType type, String message, String subject, String occasion) {}
