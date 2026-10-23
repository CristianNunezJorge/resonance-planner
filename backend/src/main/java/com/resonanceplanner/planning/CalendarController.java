package com.resonanceplanner.planning;

import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import com.resonanceplanner.planning.CalendarExportService.PlanFile;
import com.resonanceplanner.planning.dto.CalendarResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final CalendarExportService calendarExportService;

    public CalendarController(CalendarService calendarService, CalendarExportService calendarExportService) {
        this.calendarService = calendarService;
        this.calendarExportService = calendarExportService;
    }

    @GetMapping
    @Operation(summary = "Il mio calendario", description = "Tra `from` e `to` (date `AAAA-MM-GG`, comprese, al massimo 62 giorni): `events` sono le edizioni degli eventi, come in `/api/events`, con i giorni in cui sono attivi; `tasks` sono le tue attività confermate e fatte, per giorno e poi in ordine di creazione. Stessi errori 400 di `/api/events` per le date.")
    public ApiResponse<CalendarResponse> between(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Primo giorno, compreso", example = "2026-10-19") @RequestParam LocalDate from,
            @Parameter(description = "Ultimo giorno, compreso (al massimo 62 giorni)", example = "2026-10-25")
            @RequestParam LocalDate to) {
        return ApiResponse.success(calendarService.between(user.id(), from, to));
    }

    // No "produces": the errors are answered in JSON, which a request for the file only must also accept
    @GetMapping("/export")
    @Operation(summary = "Il mio piano in Excel", description = "Un file `.xlsx` con lo stesso intervallo e gli stessi errori di `GET /api/me/calendar` (D133): il foglio \"Piano\" con le attività confermate e fatte e i totali, il foglio \"Eventi\" con gli eventi attivi. La proposta in attesa non c'è. Gli errori arrivano in JSON: l'header `Accept` deve ammettere anche `application/json`.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Il file del piano",
            content = @Content(mediaType = PlanSpreadsheet.CONTENT_TYPE,
                    schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Primo giorno, compreso", example = "2026-10-19") @RequestParam LocalDate from,
            @Parameter(description = "Ultimo giorno, compreso (al massimo 62 giorni)", example = "2026-10-25")
            @RequestParam LocalDate to) {
        PlanFile file = calendarExportService.export(user.id(), from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(PlanSpreadsheet.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.name()).build().toString())
                .body(file.content());
    }
}
