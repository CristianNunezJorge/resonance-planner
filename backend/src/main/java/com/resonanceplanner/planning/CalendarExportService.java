package com.resonanceplanner.planning;

import com.resonanceplanner.catalog.CatalogService;
import com.resonanceplanner.planning.dto.CalendarResponse;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * The plan of a range as an Excel file (D133). The data are exactly those of the calendar, read in their own
 * read-only transactions; the file is written after them, so it does not keep a database connection busy.
 */
@Service
public class CalendarExportService {

    private final CalendarService calendarService;
    private final CatalogService catalogService;

    public CalendarExportService(CalendarService calendarService, CatalogService catalogService) {
        this.calendarService = calendarService;
        this.catalogService = catalogService;
    }

    /** The file of the user of the token; the range has the rules of the calendar (at most 62 days). */
    public PlanFile export(Long userId, LocalDate from, LocalDate to) {
        CalendarResponse calendar = calendarService.between(userId, from, to);
        byte[] content = PlanSpreadsheet.write(calendar.tasks(), calendar.events(), catalogService.characterNames(),
                catalogService.farmingSourceNames());
        return new PlanFile(fileName(from, to), content);
    }

    // Only the dates, which the server has already checked: nothing written by a user goes in a header
    static String fileName(LocalDate from, LocalDate to) {
        return "resonance-planner_%s_%s.xlsx".formatted(from, to);
    }

    /** The name the file is saved with and its bytes. */
    public record PlanFile(String name, byte[] content) {
    }
}
