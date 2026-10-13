"""Execute the Room 2->3 goal migration SQL against SQLite with preserved rows."""
from __future__ import annotations

import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATABASE_SOURCE = ROOT / "app/src/main/java/com/benjamin/controlfinanciero/data/FinanceDatabase.kt"
SCHEMA_V2 = ROOT / "app/schemas/com.benjamin.controlfinanciero.data.FinanceDatabase/2.json"


def migration_sql() -> list[str]:
    source = DATABASE_SOURCE.read_text(encoding="utf-8")
    block = source.split("val MIGRATION_2_3:", 1)[1].split("fun create(context", 1)[0]
    literals = re.findall(r'database\.execSQL\("((?:\\.|[^"\\])*)"\)', block)
    return [json.loads(f'"{literal}"') for literal in literals]


def main() -> None:
    schema = json.loads(SCHEMA_V2.read_text(encoding="utf-8"))["database"]
    goal = next(entity for entity in schema["entities"] if entity["tableName"] == "goals")
    db = sqlite3.connect(":memory:")
    db.execute(goal["createSql"].replace("`${TABLE_NAME}`", "`goals`"))
    db.executemany(
        "INSERT INTO goals (id,name,targetMinor,savedMinor,currency,exchangeRate,deadlineEpochDay) VALUES (?,?,?,?,?,?,?)",
        [
            (1, "Fondo activo", 10000, 2000, "MXN", 1.0, 21000),
            (2, "Meta alcanzada", 10000, 10000, "MXN", 1.0, 21000),
            (3, "Meta superada", 10000, 12000, "MXN", 1.0, 21000),
        ],
    )

    statements = migration_sql()
    assert len(statements) == 2, f"Expected 2 migration statements, found {len(statements)}"
    for statement in statements:
        db.execute(statement)

    actual = db.execute("SELECT id,status FROM goals ORDER BY id").fetchall()
    expected = [(1, "ACTIVE"), (2, "COMPLETED"), (3, "COMPLETED")]
    assert actual == expected, f"Unexpected migrated goals: {actual!r}"
    assert not db.execute("PRAGMA foreign_key_check").fetchall()
    print(f"PASS: 2->3 migration kept {len(actual)} goals and assigned correct statuses")


if __name__ == "__main__":
    main()
