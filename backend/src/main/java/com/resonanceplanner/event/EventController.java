package com.resonanceplanner.event;

import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.event.dto.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The events of the game, readable without login (D49). */
@RestController
@RequestMapping("/api/events")
@Tag(name = "Eventi", description = "Gli eventi del gioco con le date delle edizioni e i giorni in cui sono attivi.")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Eventi di un intervallo", description = "Una voce per ogni edizione che ha giorni attivi tra `from` e `to` (date `AAAA-MM-GG`, comprese, al massimo 62 giorni): `startDate` e `endDate` sono le date intere dell'edizione, `activeDates` i giorni attivi dentro l'intervallo. Lo stesso evento può comparire più volte. `activeWeekdays` è `null` se l'evento è attivo tutti i giorni.")
    public ApiResponse<List<EventResponse>> between(
            @Parameter(description = "Primo giorno, compreso", example = "2026-10-19") @RequestParam LocalDate from,
            @Parameter(description = "Ultimo giorno, compreso (al massimo 62 giorni)", example = "2026-10-25")
            @RequestParam LocalDate to) {
        return ApiResponse.success(eventService.between(from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dettaglio di un evento", description = "L'edizione in corso oggi (ora italiana) oppure la prossima, con tutti i suoi giorni attivi. Un evento a date fisse già finito mantiene le sue date.")
    public ApiResponse<EventResponse> get(
            @Parameter(description = "Identificativo dell'evento", example = "1") @PathVariable Long id) {
        return ApiResponse.success(eventService.get(id));
    }
}
