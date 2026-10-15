package com.resonanceplanner.planning;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.planning.dto.CalendarResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The calendar of the logged user: no user in the URL, it is the one of the token (D44). */
@RestController
@RequestMapping("/api/me/calendar")
@Tag(name = "Calendario", description = "Eventi e attività di un intervallo di giorni, in una sola risposta.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    @Operation(summary = "Il mio calendario", description = "Tra `from` e `to` (date `AAAA-MM-GG`, comprese, al massimo 62 giorni): `events` sono le edizioni degli eventi, come in `/api/events`, con i giorni in cui sono attivi; `tasks` sono le tue attività confermate e fatte, per giorno e poi in ordine di creazione. Stessi errori 400 di `/api/events` per le date.")
    public ApiResponse<CalendarResponse> between(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ApiResponse.success(calendarService.between(user.id(), from, to));
    }
}
