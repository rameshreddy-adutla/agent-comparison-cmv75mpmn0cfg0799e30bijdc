package quote;

import java.time.LocalDate;

public record QuoteRequest(
        String requestId,
        String sku,
        String partner,
        String currency,
        LocalDate onDate,
        int quantity) {
}
