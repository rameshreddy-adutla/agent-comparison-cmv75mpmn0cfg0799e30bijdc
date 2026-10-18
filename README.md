# Wholesale quote resolver

A small, synthetic Java CLI for quoting wholesale orders from effective-dated
price schedules. All data and code were created for this comparison. There's
no workplace code, customer data, external service or network dependency.

## Build and use

Prerequisites: Python 3.10+ and a JDK 17+ with `java`, `javac` and `jar` on PATH.
The build targets Java 17 bytecode and uses only the JDK standard library.

```sh
python3 scripts/build.py
java -jar build/quote.jar --prices fixtures/prices.tsv --requests fixtures/requests.tsv
python3 -m unittest discover -s tests -p 'test_*.py' -v
```

The CLI accepts `--prices PATH --requests PATH` in either order, exactly once
each. `--help` prints usage and exits 0. Missing, repeated or unknown options
exit 2 with a diagnostic on stderr and no stdout.

## Files and contracts

- `src/main/java/quote/`: CLI, TSV ingestion, pricing and JSON serialization.
- `fixtures/`: synthetic supported-input smoke cases.
- `schema.json`: version 1 output shape.
- `tests/test_compatibility.py`: existing black-box regression checks.
- `CONTRACT.md`: target behavior for overlapping schedules and validation.

The baseline implements the legacy, simple first-match behavior. It isn't an
implementation of every requirement in `CONTRACT.md`. Existing tests document
the supported non-overlapping cases. The comparison task is to extend/fix that
behavior without breaking the CLI or JSON contract. Add your own tests for
the new requirements.

Build outputs go in ignored `build/`. No downloads, Maven, Gradle, credentials
or environment variables are needed.
