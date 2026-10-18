#!/usr/bin/env python3
"""Build the Java 17 CLI with the installed JDK and no downloads."""

import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    classes = ROOT / "build/classes"
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    sources = sorted((ROOT / "src/main/java").rglob("*.java"))
    if not sources:
        raise SystemExit("No Java sources found")
    subprocess.run(
        ["javac", "--release", "17", "-Xlint:all", "-Werror", "-d", str(classes),
         *(str(path) for path in sources)],
        check=True,
    )
    subprocess.run(
        ["jar", "--create", "--file", str(ROOT / "build/quote.jar"),
         "--main-class", "quote.Main", "-C", str(classes), "."],
        check=True,
    )
    print("Built build/quote.jar (Java 17 bytecode)")


if __name__ == "__main__":
    main()
