package com.resonanceplanner.seed;

import com.resonanceplanner.event.EventEffect;
import com.resonanceplanner.event.GameEvent;
import com.resonanceplanner.event.GameEventRepository;
import com.resonanceplanner.seed.SeedData.EffectData;
import com.resonanceplanner.seed.SeedData.EventData;
import com.resonanceplanner.seed.SeedData.ScheduleData;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts the demo events of events.json only when their code is missing (D39):
 * the changes the admin makes to a demo event survive the frequent restarts of the free hosting.
 */
@Component
public class DemoEventSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoEventSeeder.class);

    private final GameEventRepository gameEventRepository;

    public DemoEventSeeder(GameEventRepository gameEventRepository) {
        this.gameEventRepository = gameEventRepository;
    }

    @Transactional
    public void seed(List<EventData> items) {
        List<GameEvent> missing = items.stream()
                .filter(item -> !gameEventRepository.existsByCode(item.id()))
                .map(DemoEventSeeder::toEvent)
                .toList();
        gameEventRepository.saveAll(missing);
        log.info("Demo events: {} added, {} already present", missing.size(), items.size() - missing.size());
    }

    private static GameEvent toEvent(EventData item) {
        ScheduleData schedule = item.schedule();
        GameEvent event = GameEvent.recurring(item.id(), item.name(), item.type(), item.description(),
                schedule.startOffsetDays(), schedule.durationDays(), schedule.repeatEveryDays());
        if (schedule.activeWeekdays() != null) {
            event.setActiveWeekdays(Set.copyOf(schedule.activeWeekdays()));
        }
        event.setRewards(item.rewards());
        item.activities().forEach(activity ->
                event.addActivity(activity.title(), activity.minutes(), activity.frequency(), activity.unlockDay()));
        if (item.effect() != null) {
            event.setEffect(toEffect(item.effect()));
        }
        return event;
    }

    private static EventEffect toEffect(EffectData effect) {
        return new EventEffect(effect.type(), effect.sourceType(), effect.multiplier(), effect.dailyLimit());
    }
}
