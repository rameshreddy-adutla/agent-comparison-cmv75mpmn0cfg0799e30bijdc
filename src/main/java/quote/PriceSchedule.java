package quote;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceSchedule(
        String priceId,
        String sku,
        String partner,
        String currency,
        LocalDate validFrom,
        LocalDate validTo,
        int revision,
        BigDecimal retailPrice,
        BigDecimal discountPercent) {
}
