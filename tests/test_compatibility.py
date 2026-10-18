"""Existing supported CLI behavior; agents should add separate new tests."""

import json
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PRICE_HEADER = "price_id\tsku\tpartner\tcurrency\tvalid_from\tvalid_to\trevision\tretail_price\tdiscount_percent\n"
REQUEST_HEADER = "request_id\tsku\tpartner\tcurrency\ton_date\tquantity\n"


class CompatibilityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        subprocess.run(["python3", "scripts/build.py"], cwd=ROOT, check=True)

    def invoke(self, prices=None, requests=None, options=None):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            price_path = directory / "prices.tsv"
            request_path = directory / "requests.tsv"
            price_path.write_text(
                prices if prices is not None else (ROOT / "fixtures/prices.tsv").read_text(),
                encoding="utf-8",
            )
            request_path.write_text(
                requests if requests is not None else (ROOT / "fixtures/requests.tsv").read_text(),
                encoding="utf-8",
            )
            args = options if options is not None else [
                "--prices", str(price_path), "--requests", str(request_path),
            ]
            return subprocess.run(
                ["java", "-jar", str(ROOT / "build/quote.jar"), *args],
                text=True, capture_output=True, timeout=10,
            )

    def test_supplied_fixture_and_schema_shape(self):
        result = self.invoke()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr, "")
        self.assertTrue(result.stdout.endswith("\n"))
        payload = json.loads(result.stdout)
        schema = json.loads((ROOT / "schema.json").read_text())
        self.assertEqual(set(payload), set(schema["required"]))
        self.assertEqual(payload["errors"], [])
        self.assertEqual(len(payload["quotes"]), 2)
        first, second = payload["quotes"]
        self.assertEqual(set(first), set(schema["properties"]["quotes"]["items"]["required"]))
        self.assertEqual(first, {
            "request_id": "q1", "sku": "SKU1", "partner": "PARTNER1",
            "currency": "GBP", "price_id": "standard", "on_date": "2026-03-01",
            "quantity": 3, "unit_price": "8.00", "total_price": "24.00",
        })
        self.assertEqual((second["price_id"], second["unit_price"], second["total_price"]),
                         ("fallback", "5.00", "10.00"))

    def test_no_price_is_a_batch_error(self):
        result = self.invoke(requests=REQUEST_HEADER + "missing\tUNKNOWN\tPARTNER1\tGBP\t2026-03-01\t1\n")
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stderr, "")
        self.assertEqual(json.loads(result.stdout), {
            "quotes": [], "errors": [{"request_id": "missing", "code": "NO_PRICE"}],
        })

    def test_invalid_request_does_not_abort_later_requests(self):
        result = self.invoke(requests=REQUEST_HEADER
                             + "bad\tSKU1\tPARTNER1\tGBP\t2026-03-01\t0\n"
                             + "good\tSKU1\tPARTNER1\tGBP\t2026-03-01\t1\n")
        self.assertEqual(result.returncode, 1)
        payload = json.loads(result.stdout)
        self.assertEqual(payload["errors"], [{"request_id": "bad", "code": "INVALID_REQUEST"}])
        self.assertEqual([q["request_id"] for q in payload["quotes"]], ["good"])
        self.assertEqual(result.stderr, "")

    def test_empty_request_batch(self):
        result = self.invoke(requests=REQUEST_HEADER)
        self.assertEqual(result.returncode, 0)
        self.assertEqual(json.loads(result.stdout), {"quotes": [], "errors": []})

    def test_currency_does_not_cross_match(self):
        result = self.invoke(requests=REQUEST_HEADER + "q\tSKU1\tPARTNER1\tEUR\t2026-03-01\t1\n")
        self.assertEqual(result.returncode, 1)
        self.assertEqual(json.loads(result.stdout)["errors"][0]["code"], "NO_PRICE")

    def test_invalid_schedule_width_is_fatal(self):
        result = self.invoke(prices=PRICE_HEADER + "bad\trow\n")
        self.assertEqual(result.returncode, 2)
        self.assertEqual(result.stdout, "")
        self.assertIn("line 2", result.stderr)
        self.assertIn("row", result.stderr)

    def test_missing_options_is_usage_error(self):
        result = self.invoke(options=[])
        self.assertEqual(result.returncode, 2)
        self.assertEqual(result.stdout, "")
        self.assertIn("Usage:", result.stderr)

    def test_help(self):
        result = self.invoke(options=["--help"])
        self.assertEqual(result.returncode, 0)
        self.assertIn("--prices PATH --requests PATH", result.stdout)
        self.assertEqual(result.stderr, "")


if __name__ == "__main__":
    unittest.main()
