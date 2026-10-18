# Quote contract, version 1

All requirements here are visible to both agents. Evaluator checks may use
different synthetic values but won't introduce additional requirements.

## Input tables

Inputs are UTF-8, tab-separated text with the exact headers below. LF and CRLF
are supported. Empty physical lines after the header are ignored. Fields aren't
trimmed or quoted; tabs/newlines inside a field aren't supported. Every other
row must have exactly the header's column count. Empty files and wrong headers
are fatal input errors.

Prices header:

```text
price_id	sku	partner	currency	valid_from	valid_to	revision	retail_price	discount_percent
```

Requests header:

```text
request_id	sku	partner	currency	on_date	quantity
```

Identifiers (`price_id`, `request_id`, `sku`, non-wildcard `partner`) match
`[A-Za-z0-9][A-Za-z0-9_-]{0,63}`. Only a price's partner may be `*`, meaning
any partner. Currency matches `[A-Z]{3}`; no external currency list is needed.
Dates are valid calendar dates written exactly `yyyy-MM-dd`, years 0001-9999.
Revision is an unsigned base-10 integer from 0 to 2147483647. Quantity is an
unsigned base-10 integer from 1 to 1000000. Leading zeros are permitted for
integers; signs, whitespace and exponents aren't.

`valid_from` is required; an empty `valid_to` means no upper bound. A nonempty
`valid_to` must be strictly later than `valid_from`. Price IDs must be unique
across the whole prices file, even for different SKUs or inactive schedules.
Repeated request IDs are allowed and each row is processed independently.

Retail price and discount are base-10 decimal strings matching
`[0-9]+(\.[0-9]+)?`. Leading zeros and arbitrary fractional precision are
allowed. Retail price must be nonnegative. Discount is a percentage from 0
through 100 inclusive. No exponent, sign, whitespace, NaN or infinity.

## Schedule selection

For each valid request:

1. Consider only schedules with the same SKU and currency whose partner is
   either exactly the request's partner or `*`, and with
   `valid_from <= on_date < valid_to`. A blank end date is unbounded.
2. If any eligible exact-partner schedules exist, discard eligible wildcard
   schedules. Ineligible exact-partner schedules don't suppress wildcards.
3. Within the remaining candidates, prefer the latest `valid_from`.
4. Within that start date, prefer the highest numeric revision.
5. If more than one distinct price ID remains, return `AMBIGUOUS_PRICE`, even
   if the candidates have identical monetary values. Don't break ties by file
   order, end date or price ID.

An empty candidate set gives `NO_PRICE`. Selection must be independent of
price-row order. These are per-request outcomes; other requests still run.

## Money

Use exact decimal arithmetic, not floating point.

```text
unrounded_unit = retail_price * (1 - discount_percent / 100)
unit_price = unrounded_unit rounded once to 2 places using HALF_EVEN
total_price = unit_price * quantity, represented to exactly 2 places
```

Round the unit before multiplying, not the total. No intermediate rounding.
Both money fields are JSON strings with exactly two fractional digits, no
exponent and no grouping separators, including zero. No currency conversion.

## JSON, errors and process status

Keep existing flags and `schema.json` unchanged. A processed batch emits
exactly one JSON object followed by a newline on stdout:

```json
{
  "quotes": [
    {
      "request_id": "q1",
      "sku": "SKU1",
      "partner": "PARTNER1",
      "currency": "GBP",
      "price_id": "p1",
      "on_date": "2026-03-01",
      "quantity": 2,
      "unit_price": "1.00",
      "total_price": "2.00"
    }
  ],
  "errors": [
    {"request_id": "q2", "code": "NO_PRICE"}
  ]
}
```

Quote objects and error objects have exactly the shown keys and types. JSON
object key order isn't significant. Within each array preserve request input
order, retaining repeated request IDs. Every nonempty request row contributes
exactly one quote or error.

An invalid request field or row width gives `INVALID_REQUEST` and doesn't abort
the batch. Its error `request_id` is the first field if that field is a valid
identifier, otherwise null. This applies even to rows with the wrong width.

Domain errors (`INVALID_REQUEST`, `NO_PRICE`, `AMBIGUOUS_PRICE`) live in JSON,
not stderr. Exit 0 if there are no request errors, including a header-only
requests file. Exit 1 if at least one request error occurs. Stderr stays empty
for either processed-batch exit.

CLI misuse, unreadable/undecodable input, bad headers, or any invalid schedule
is fatal: exit 2, stdout empty and a concise stderr diagnostic with no Java
stack trace. Schedule diagnostics must name the physical line number and
offending column, or `row` for width and `price_id` for duplicate IDs.
Validate the whole schedule table before emitting any quote; don't skip bad
schedules just because a request wouldn't select them. A header-only prices
file is valid. Request header/read errors are fatal too, with no partial JSON.

## Scope and delivery

Work locally, without external services. No concurrency, streaming, persistence,
VAT, currency conversion, CSV escaping or benchmark throughput is required.
Inputs are small enough to load into memory. Improve clarity without introducing
third-party runtime/build dependencies.

Edit production code and documentation as needed, add tests and fix all stated
requirements. Don't weaken or edit `schema.json`, `CONTRACT.md`, supplied
fixtures or `tests/test_compatibility.py`. Adding new fixture/test files is fine.
Preserve `python3 scripts/build.py` and `java -jar build/quote.jar` entry points.
Deliver the changed files, tests and a concise final report with commands run,
results and any remaining limitations.
