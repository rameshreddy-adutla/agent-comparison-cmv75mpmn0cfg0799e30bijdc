package quote;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Tables {
    public static final String PRICE_HEADER =
            "price_id\tsku\tpartner\tcurrency\tvalid_from\tvalid_to\trevision\tretail_price\tdiscount_percent";
    public static final String REQUEST_HEADER = "request_id\tsku\tpartner\tcurrency\ton_date\tquantity";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");

    private Tables() {
    }

    public record Row(int line, List<String> fields) {
        public Row {
            fields = List.copyOf(fields);
        }
    }

    public static List<Row> read(Path path, String header) throws IOException, InputException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(header)) {
            throw new InputException("line 1: invalid header");
        }
        var rows = new ArrayList<Row>();
        for (int index = 1; index < lines.size(); index++) {
            if (!lines.get(index).isEmpty()) {
                rows.add(new Row(index + 1, List.of(lines.get(index).split("\t", -1))));
            }
        }
        return List.copyOf(rows);
    }

    public static List<PriceSchedule> prices(List<Row> rows) throws InputException {
        var prices = new ArrayList<PriceSchedule>();
        for (Row row : rows) {
            List<String> cells = row.fields();
            if (cells.size() != 9) {
                throw invalid(row, "row");
            }
            prices.add(new PriceSchedule(
                    identifier(row, 0, "price_id"),
                    identifier(row, 1, "sku"),
                    cells.get(2).equals("*") ? "*" : identifier(row, 2, "partner"),
                    currency(row, 3),
                    date(row, 4, "valid_from"),
                    cells.get(5).isEmpty() ? null : date(row, 5, "valid_to"),
                    integer(row, 6, "revision"),
                    decimal(row, 7, "retail_price"),
                    decimal(row, 8, "discount_percent")));
        }
        return List.copyOf(prices);
    }

    public static QuoteRequest request(Row row) throws InputException {
        if (row.fields().size() != 6) {
            throw invalid(row, "row");
        }
        int quantity = integer(row, 5, "quantity");
        if (quantity < 1) {
            throw invalid(row, "quantity");
        }
        return new QuoteRequest(
                identifier(row, 0, "request_id"),
                identifier(row, 1, "sku"),
                identifier(row, 2, "partner"),
                currency(row, 3),
                date(row, 4, "on_date"),
                quantity);
    }

    public static String requestId(Row row) {
        String candidate = row.fields().get(0);
        return IDENTIFIER.matcher(candidate).matches() ? candidate : null;
    }

    private static String identifier(Row row, int index, String column) throws InputException {
        String value = row.fields().get(index);
        if (!IDENTIFIER.matcher(value).matches()) {
            throw invalid(row, column);
        }
        return value;
    }

    private static String currency(Row row, int index) throws InputException {
        String value = row.fields().get(index);
        if (!CURRENCY.matcher(value).matches()) {
            throw invalid(row, "currency");
        }
        return value;
    }

    private static LocalDate date(Row row, int index, String column) throws InputException {
        try {
            return LocalDate.parse(row.fields().get(index));
        } catch (DateTimeParseException exception) {
            throw invalid(row, column);
        }
    }

    private static int integer(Row row, int index, String column) throws InputException {
        try {
            return Integer.parseInt(row.fields().get(index));
        } catch (NumberFormatException exception) {
            throw invalid(row, column);
        }
    }

    private static BigDecimal decimal(Row row, int index, String column) throws InputException {
        try {
            return new BigDecimal(row.fields().get(index));
        } catch (NumberFormatException exception) {
            throw invalid(row, column);
        }
    }

    private static InputException invalid(Row row, String column) {
        return new InputException("line " + row.line() + ": invalid " + column);
    }
}
