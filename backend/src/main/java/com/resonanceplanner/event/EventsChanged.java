package com.resonanceplanner.event;

/**
 * The admin created, changed or deleted an event (D114). Published inside the transaction: the notifications (D135)
 * tell the connected users only after the commit, so nobody reloads the calendar before the change is visible.
 */
public record EventsChanged() {}
