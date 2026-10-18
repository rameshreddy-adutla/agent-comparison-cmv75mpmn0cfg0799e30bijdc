package quote;

import java.util.stream.Collectors;

public final class JsonOutput {
    private JsonOutput() {
    }

    public static String render(Batch batch) {
        String quotes = batch.quotes().stream().map(JsonOutput::quote).collect(Collectors.joining(","));
        String errors = batch.errors().stream().map(JsonOutput::error).collect(Collectors.joining(","));
        return "{\"quotes\":[" + quotes + "],\"errors\":[" + errors + "]}";
    }

    private static String quote(Quote quote) {
        QuoteRequest request = quote.request();
        return "{\"request_id\":" + string(request.requestId())
                + ",\"sku\":" + string(request.sku())
                + ",\"partner\":" + string(request.partner())
                + ",\"currency\":" + string(request.currency())
                + ",\"price_id\":" + string(quote.priceId())
                + ",\"on_date\":" + string(request.onDate().toString())
                + ",\"quantity\":" + request.quantity()
                + ",\"unit_price\":" + string(quote.unitPrice().toPlainString())
                + ",\"total_price\":" + string(quote.totalPrice().toPlainString()) + "}";
    }

    private static String error(QuoteError error) {
        return "{\"request_id\":" + string(error.requestId()) + ",\"code\":" + string(error.code()) + "}";
    }

    private static String string(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }
}
