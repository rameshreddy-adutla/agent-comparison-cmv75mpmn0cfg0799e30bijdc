package quote;

import java.math.BigDecimal;

public record Quote(QuoteRequest request, String priceId, BigDecimal unitPrice, BigDecimal totalPrice) {
}
