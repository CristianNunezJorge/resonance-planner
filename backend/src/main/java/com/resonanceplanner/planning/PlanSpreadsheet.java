package com.resonanceplanner.planning;

import static java.util.stream.Collectors.joining;

import com.resonanceplanner.event.EventType;
import com.resonanceplanner.event.dto.EventResponse;
import com.resonanceplanner.planning.dto.TaskResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The plan of a range as an Excel file (D133): the sheet "Piano" with one row per task and the totals, then the sheet
 * "Eventi" with the events active in the range. It only turns the data of the calendar into bytes. The texts written
 * by users and admins always go in text cells: a title that starts with "=" stays text and never becomes a formula.
 */
public final class PlanSpreadsheet {

    /** The media type of an .xlsx file. */
    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final List<String> PLAN_HEADERS = List.of("Data", "Giorno", "Tipo", "Attività", "Personaggio",
            "Fonte", "Run", "Waveplate", "Minuti", "Stato", "Note");
    private static final List<String> EVENT_HEADERS = List.of("Evento", "Tipo", "Inizio", "Fine",
            "Giorni attivi nel periodo");
    private static final int WAVEPLATE_COLUMN = 7;
    private static final int MINUTES_COLUMN = 8;
    private static final Locale ITALIAN = Locale.ITALIAN;
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("dd/MM");
    // Column widths in characters: the longest text plus a margin, but a long note does not fill the screen.
    // Measured on the texts, because autoSizeColumn needs the fonts of the system, which a server image may not have.
    private static final int WIDTH_MARGIN = 2;
    private static final int MAX_WIDTH = 50;
    private static final int UNITS_PER_CHARACTER = 256;

    private PlanSpreadsheet() {
    }

    /**
     * The file of the tasks (in the order of the calendar) and of the events of a range; the names come from the
     * catalog, and an id without a name is written as it is.
     */
    public static byte[] write(List<TaskResponse> tasks, List<EventResponse> events, Map<String, String> characterNames,
            Map<String, String> sourceNames) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream file = new ByteArrayOutputStream()) {
            Styles styles = Styles.of(workbook);
            writePlan(workbook.createSheet("Piano"), styles, tasks, characterNames, sourceNames);
            writeEvents(workbook.createSheet("Eventi"), styles, events);
            // The totals already have their value, also for programs that do not calculate when they open a file
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(file);
            return file.toByteArray();
        } catch (IOException e) {
            // Everything is written in memory: an error here is a bug, not something the user can fix
            throw new UncheckedIOException(e);
        }
    }

    private static void writePlan(Sheet sheet, Styles styles, List<TaskResponse> tasks,
            Map<String, String> characterNames, Map<String, String> sourceNames) {
        header(sheet, styles, PLAN_HEADERS);
        for (int i = 0; i < tasks.size(); i++) {
            taskRow(sheet.createRow(i + 1), styles, tasks.get(i), characterNames, sourceNames);
        }
        if (!tasks.isEmpty()) {
            // The filter covers the tasks; the totals stay out of it
            sheet.setAutoFilter(new CellRangeAddress(0, tasks.size(), 0, PLAN_HEADERS.size() - 1));
            totals(sheet.createRow(tasks.size() + 1), styles, tasks.size() + 1);
        }
        fitColumns(sheet, PLAN_HEADERS.size());
        if (tasks.isEmpty()) {
            emptyMessage(sheet, "Nessuna attività confermata o fatta in questo periodo.");
        }
    }

    private static void taskRow(Row row, Styles styles, TaskResponse task, Map<String, String> characterNames,
            Map<String, String> sourceNames) {
        date(row, 0, task.taskDate(), styles);
        text(row, 1, task.taskDate().getDayOfWeek().getDisplayName(TextStyle.FULL, ITALIAN));
        text(row, 2, kindText(task.kind()));
        text(row, 3, task.title());
        text(row, 4, nameOf(task.characterId(), characterNames));
        text(row, 5, nameOf(task.farmingSourceId(), sourceNames));
        number(row, 6, task.runs());
        number(row, WAVEPLATE_COLUMN, task.waveplates());
        number(row, MINUTES_COLUMN, task.minutes());
        text(row, 9, task.status() == TaskStatus.DONE ? "Fatta" : "Da fare");
        text(row, 10, task.notes());
    }

    // Formulas written by us, the only ones in the file: they stay right if the user changes the numbers
    private static void totals(Row row, Styles styles, int lastTaskRow) {
        Cell label = row.createCell(0);
        label.setCellValue("Totale");
        label.setCellStyle(styles.bold());
        for (int column : List.of(WAVEPLATE_COLUMN, MINUTES_COLUMN)) {
            String letter = CellReference.convertNumToColString(column);
            Cell sum = row.createCell(column);
            sum.setCellFormula("SUM(%s2:%s%d)".formatted(letter, letter, lastTaskRow));
            sum.setCellStyle(styles.bold());
        }
    }

    private static void writeEvents(Sheet sheet, Styles styles, List<EventResponse> events) {
        header(sheet, styles, EVENT_HEADERS);
        for (int i = 0; i < events.size(); i++) {
            EventResponse event = events.get(i);
            Row row = sheet.createRow(i + 1);
            text(row, 0, event.name());
            text(row, 1, eventTypeText(event.type()));
            date(row, 2, event.startDate(), styles);
            date(row, 3, event.endDate(), styles);
            text(row, 4, event.activeDates().stream().map(SHORT_DAY::format).collect(joining(", ")));
        }
        if (!events.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, events.size(), 0, EVENT_HEADERS.size() - 1));
        }
        fitColumns(sheet, EVENT_HEADERS.size());
        if (events.isEmpty()) {
            emptyMessage(sheet, "Nessun evento attivo in questo periodo.");
        }
    }

    // Written after the widths, so the first column stays as wide as its header: the text runs over the empty cells
    private static void emptyMessage(Sheet sheet, String message) {
        text(sheet.createRow(1), 0, message);
    }

    // Bold, and always in sight while the rows scroll
    private static void header(Sheet sheet, Styles styles, List<String> headers) {
        Row row = sheet.createRow(0);
        for (int column = 0; column < headers.size(); column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(styles.bold());
        }
        sheet.createFreezePane(0, 1);
    }

    // A text cell, or an empty one: setCellValue(String) never reads the text as a formula
    private static void text(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private static void number(Row row, int column, Integer value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private static void date(Row row, int column, LocalDate value, Styles styles) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(styles.date());
    }

    private static void fitColumns(Sheet sheet, int columns) {
        DataFormatter shown = new DataFormatter(ITALIAN);
        for (int column = 0; column < columns; column++) {
            int widest = 0;
            for (Row row : sheet) {
                widest = Math.max(widest, shown.formatCellValue(row.getCell(column)).length());
            }
            sheet.setColumnWidth(column, Math.min(widest + WIDTH_MARGIN, MAX_WIDTH) * UNITS_PER_CHARACTER);
        }
    }

    private static String nameOf(String id, Map<String, String> names) {
        return id == null ? null : names.getOrDefault(id, id);
    }

    private static String kindText(TaskKind kind) {
        return switch (kind) {
            case FARMING -> "Farming";
            case EVENT -> "Evento";
            case CUSTOM -> "Manuale";
        };
    }

    // The same words as the calendar page
    private static String eventTypeText(EventType type) {
        return switch (type) {
            case COMBAT_STAGES -> "Combattimento";
            case DOUBLE_DROP -> "Ricompense doppie";
            case POINTS_SHOP -> "Negozio a punti";
        };
    }

    private record Styles(CellStyle bold, CellStyle date) {

        static Styles of(Workbook workbook) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle bold = workbook.createCellStyle();
            bold.setFont(boldFont);
            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
            return new Styles(bold, date);
        }
    }
}
