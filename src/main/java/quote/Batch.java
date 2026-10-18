package quote;

import java.util.List;

public record Batch(List<Quote> quotes, List<QuoteError> errors) {
    public Batch {
        quotes = List.copyOf(quotes);
        errors = List.copyOf(errors);
    }
}
