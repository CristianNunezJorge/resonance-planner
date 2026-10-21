package com.resonanceplanner.notification;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * What the server already sent (D135), in memory: for each user and subject, the last occasion. A new occasion (a new
 * reading, a new day) replaces the old one, so the memory does not grow with time. After a restart it is empty, and
 * a notice can arrive a second time.
 */
@Component
public class SentNotices {

    private final Map<Long, Map<String, String>> lastOccasions = new ConcurrentHashMap<>();

    /** True only the first time: the job and the check after AUTH can ask at the same moment, and one of them wins. */
    public boolean firstTime(Long userId, Notice notice) {
        Map<String, String> subjects = lastOccasions.computeIfAbsent(userId, id -> new ConcurrentHashMap<>());
        return !notice.occasion().equals(subjects.put(notice.subject(), notice.occasion()));
    }

    /**
     * The notice reached no connection (the tab closed in the meantime): it can be sent again. Only that occasion is
     * undone, so a late undo never erases a newer one.
     */
    public void undo(Long userId, Notice notice) {
        Map<String, String> subjects = lastOccasions.get(userId);
        if (subjects != null) {
            subjects.remove(notice.subject(), notice.occasion());
        }
    }

    public void forget(Collection<Long> userIds) {
        userIds.forEach(lastOccasions::remove);
    }
}
