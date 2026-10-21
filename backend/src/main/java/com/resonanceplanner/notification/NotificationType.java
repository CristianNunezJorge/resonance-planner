package com.resonanceplanner.notification;

/** What a real-time notification is about (D135): the frontend reacts to EVENTS_CHANGED by reloading the calendar. */
public enum NotificationType {
    WAVEPLATE_ALMOST_FULL,
    WAVEPLATE_FULL,
    EVENT_ENDING,
    EVENTS_CHANGED
}
