package com.resonanceplanner.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.planning.dto.TaskResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

// The Excel file of the plan (D133), read back with POI as a spreadsheet program would.
class PlanSpreadsheetTest {

    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 14);
    private static final LocalDate TUESDAY = LocalDate.of(2030, 1, 15);
    private static final Map<String, String> CHARACTERS = Map.of("jinhsi", "Jinhsi");
    private static final Map<String, String> SOURCES = Map.of(
            "sim-resonance-potion", "Simulation Training: Resonance Potion");
    private static final DataFormatter SHOWN = new DataFormatter(Locale.ITALIAN);

    private static final TaskResponse FARMING = task(1L, TaskOrigin.AGENT, TaskKind.FARMING, TaskStatus.CONFIRMED,
            MONDAY, "Farming per Jinhsi", null, 30, "sim-resonance-potion", 3, 120, null, "jinhsi");
    private static final TaskResponse EVENT_DONE = task(2L, TaskOrigin.AGENT, TaskKind.EVENT, TaskStatus.DONE,
            MONDAY, "Torneo di prova: Drill 1", null, 15, null, null, 0, 7L, null);
    // Text written by the user that a spreadsheet program could take for a formula
    private static final TaskResponse MANUAL = task(3L, TaskOrigin.MANUAL, TaskKind.CUSTOM, TaskStatus.CONFIRMED,
            TUESDAY, "=HYPERLINK(\"https://evil.example\",\"clic\")", "+cmd|' /C calc'!A0", 20, null, null, 0,
            null, null);

    @Test
    void writesOneRowPerTaskWithTheTextsInItalian() {
        Sheet plan = plan(List.of(FARMING, EVENT_DONE, MANUAL), List.of()).getSheet("Piano");

        assertThat(texts(plan.getRow(0))).containsExactly(
                "Data", "Giorno", "Tipo", "Attività", "Personaggio", "Fonte", "Run", "Waveplate", "Minuti", "Stato",
                "Note");
        assertThat(texts(plan.getRow(1))).containsExactly(
                "14/01/2030", "lunedì", "Farming", "Farming per Jinhsi", "Jinhsi",
                "Simulation Training: Resonance Potion", "3", "120", "30", "Da fare", "");
        assertThat(texts(plan.getRow(2))).containsExactly(
                "14/01/2030", "lunedì", "Evento", "Torneo di prova: Drill 1", "", "", "", "0", "15", "Fatta", "");
        assertThat(texts(plan.getRow(3)).subList(0, 4)).containsExactly("15/01/2030", "martedì", "Manuale",
                "=HYPERLINK(\"https://evil.example\",\"clic\")");
    }

    @Test
    void writesTheDaysAsRealDatesAndTheQuantitiesAsNumbers() {
        Row farming = plan(List.of(FARMING), List.of()).getSheet("Piano").getRow(1);

        Cell day = farming.getCell(0);
        assertThat(day.getCellType()).isEqualTo(CellType.NUMERIC);
        assertThat(DateUtil.isCellDateFormatted(day)).isTrue();
        assertThat(day.getLocalDateTimeCellValue().toLocalDate()).isEqualTo(MONDAY);
        assertThat(farming.getCell(6).getNumericCellValue()).isEqualTo(3);
        assertThat(farming.getCell(7).getNumericCellValue()).isEqualTo(120);
        assertThat(farming.getCell(8).getNumericCellValue()).isEqualTo(30);
    }

    @Test
    void keepsTheTextsOfUsersAndAdminsAsTextNeverAsFormulas() {
        Row manual = plan(List.of(MANUAL), List.of()).getSheet("Piano").getRow(1);

        assertThat(manual.getCell(3).getCellType()).isEqualTo(CellType.STRING);
        assertThat(manual.getCell(3).getStringCellValue()).isEqualTo("=HYPERLINK(\"https://evil.example\",\"clic\")");
        assertThat(manual.getCell(10).getCellType()).isEqualTo(CellType.STRING);
        assertThat(manual.getCell(10).getStringCellValue()).isEqualTo("+cmd|' /C calc'!A0");
    }

    @Test
    void endsWithTheTotalsAsFormulasAlreadyCalculated() {
        Row total = plan(List.of(FARMING, EVENT_DONE, MANUAL), List.of()).getSheet("Piano").getRow(4);

        assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Totale");
        assertThat(total.getCell(7).getCellFormula()).isEqualTo("SUM(H2:H4)");
        assertThat(total.getCell(7).getNumericCellValue()).isEqualTo(120);
        assertThat(total.getCell(8).getCellFormula()).isEqualTo("SUM(I2:I4)");
        assertThat(total.getCell(8).getNumericCellValue()).isEqualTo(65);
        assertThat(total.getSheet().getLastRowNum()).isEqualTo(4);
    }

    @Test
    void keepsTheHeaderInSightWithAFilterOnTheTasks() {
        XSSFSheet plan = plan(List.of(FARMING, EVENT_DONE, MANUAL), List.of()).getSheet("Piano");

        assertThat(plan.getWorkbook().getFontAt(plan.getRow(0).getCell(0).getCellStyle().getFontIndex()).getBold())
                .isTrue();
        assertThat(plan.getPaneInformation().isFreezePane()).isTrue();
        assertThat(plan.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 1);
        // The filter covers the tasks, not the totals
        assertThat(plan.getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A1:K4");
    }

    @Test
    void showsTheIdWhenANameIsMissingFromTheCatalog() {
        TaskResponse unknown = task(4L, TaskOrigin.AGENT, TaskKind.FARMING, TaskStatus.CONFIRMED, MONDAY,
                "Farming", null, 10, "old-source", 1, 40, null, "old-character");

        Row row = plan(List.of(unknown), List.of()).getSheet("Piano").getRow(1);

        assertThat(row.getCell(4).getStringCellValue()).isEqualTo("old-character");
        assertThat(row.getCell(5).getStringCellValue()).isEqualTo("old-source");
    }

    @Test
    void saysSoWhenThePeriodHasNoTasks() {
        Sheet plan = plan(List.of(), List.of()).getSheet("Piano");

        assertThat(plan.getRow(1).getCell(0).getStringCellValue())
                .isEqualTo("Nessuna attività confermata o fatta in questo periodo.");
        assertThat(plan.getLastRowNum()).isEqualTo(1);
        // The message runs over the empty cells: the column of the dates stays as wide as its header
        assertThat(plan.getColumnWidth(0)).isEqualTo(("Data".length() + 2) * 256);
    }

    @Test
    void fitsTheColumnsToTheirTextsUpToAMaximum() {
        TaskResponse longNote = task(5L, TaskOrigin.MANUAL, TaskKind.CUSTOM, TaskStatus.CONFIRMED, MONDAY,
                "Giro dei boss", "x".repeat(300), 30, null, null, 0, null, null);

        Sheet plan = plan(List.of(longNote), List.of()).getSheet("Piano");

        // "Giro dei boss" plus a margin of two characters; the note stops at 50
        assertThat(plan.getColumnWidth(3)).isEqualTo(("Giro dei boss".length() + 2) * 256);
        assertThat(plan.getColumnWidth(10)).isEqualTo(50 * 256);
    }

    @Test
    void listsTheEventsWithTheirActiveDaysInThePeriod() {
        EventResponse event = new EventResponse(7L, "Torneo di prova", EventType.COMBAT_STAGES, "Solo martedì e sabato.",
                MONDAY, LocalDate.of(2030, 1, 20), List.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY),
                List.of(TUESDAY, LocalDate.of(2030, 1, 19)), List.of(), List.of(), null);

        Sheet events = plan(List.of(), List.of(event)).getSheet("Eventi");

        assertThat(texts(events.getRow(0))).containsExactly("Evento", "Tipo", "Inizio", "Fine",
                "Giorni attivi nel periodo");
        assertThat(texts(events.getRow(1))).containsExactly("Torneo di prova", "Combattimento", "14/01/2030",
                "20/01/2030", "15/01, 19/01");
        assertThat(DateUtil.isCellDateFormatted(events.getRow(1).getCell(2))).isTrue();
    }

    @Test
    void saysSoWhenThePeriodHasNoEvents() {
        Sheet events = plan(List.of(FARMING), List.of()).getSheet("Eventi");

        assertThat(events.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Nessun evento attivo in questo periodo.");
    }

    @Test
    void writesThePlanSheetFirst() {
        XSSFWorkbook workbook = plan(List.of(FARMING), List.of());

        assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
        assertThat(workbook.getSheetName(0)).isEqualTo("Piano");
        assertThat(workbook.getSheetName(1)).isEqualTo("Eventi");
    }

    // Read from memory: nothing to close but the workbook, which the tests read until the end
    private static XSSFWorkbook plan(List<TaskResponse> tasks, List<EventResponse> events) {
        byte[] file = PlanSpreadsheet.write(tasks, events, CHARACTERS, SOURCES);
        try {
            return new XSSFWorkbook(new ByteArrayInputStream(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // What a spreadsheet program shows in each cell, empty cells included
    private static List<String> texts(Row row) {
        return IntStream.range(0, row.getLastCellNum())
                .mapToObj(column -> SHOWN.formatCellValue(row.getCell(column)))
                .toList();
    }

    private static TaskResponse task(Long id, TaskOrigin origin, TaskKind kind, TaskStatus status, LocalDate day,
            String title, String notes, int minutes, String sourceId, Integer runs, int waveplates, Long eventId,
            String characterId) {
        Instant completedAt = status == TaskStatus.DONE ? Instant.parse("2030-01-14T20:00:00Z") : null;
        return new TaskResponse(id, origin, kind, status, day, title, notes, minutes, sourceId, runs, waveplates,
                eventId, characterId, completedAt);
    }
}
