package quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class QuoteResolver {
    public Batch resolve(List<PriceSchedule> prices, List<Tables.Row> rows) {
        var quotes = new ArrayList<Quote>();
        var errors = new ArrayList<QuoteError>();
        for (Tables.Row row : rows) {
            QuoteRequest request;
            try {
                request = Tables.request(row);
            } catch (InputException exception) {
                errors.add(new QuoteError(Tables.requestId(row), "INVALID_REQUEST"));
                continue;
            }
            Optional<PriceSchedule> price = firstPrice(prices, request);
            if (price.isEmpty()) {
                errors.add(new QuoteError(request.requestId(), "NO_PRICE"));
            } else {
                quotes.add(calculate(request, price.orElseThrow()));
            }
        }
        return new Batch(quotes, errors);
    }

    private Optional<PriceSchedule> firstPrice(List<PriceSchedule> prices, QuoteRequest request) {
        return prices.stream()
                .filter(price -> price.sku().equals(request.sku()))
                .filter(price -> price.currency().equals(request.currency()))
                .filter(price -> price.partner().equals("*") || price.partner().equals(request.partner()))
                .filter(price -> !request.onDate().isBefore(price.validFrom()))
                .filter(price -> price.validTo() == null || !request.onDate().isAfter(price.validTo()))
                .findFirst();
    }

    private Quote calculate(QuoteRequest request, PriceSchedule price) {
        BigDecimal factor = BigDecimal.ONE.subtract(price.discountPercent().movePointLeft(2));
        BigDecimal rawUnit = price.retailPrice().multiply(factor);
        BigDecimal unit = rawUnit.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = rawUnit.multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        return new Quote(request, price.priceId(), unit, total);
    }
}
