"""Execute the Room 3->4 period-balance migration SQL against SQLite.

Verifies that:
  * the migration's CREATE TABLE matches the schema Room generates for v4,
  * existing income periods (and other rows) survive,
  * the foreign key to income_periods is enforced with ON DELETE CASCADE,
  * no foreign key violations remain.
"""
from __future__ import annotations

import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATABASE_SOURCE = ROOT / "app/src/main/java/com/benjamin/controlfinanciero/data/FinanceDatabase.kt"
SCHEMA_DIR = ROOT / "app/schemas/com.benjamin.controlfinanciero.data.FinanceDatabase"
SCHEMA_V3 = SCHEMA_DIR / "3.json"
SCHEMA_V4 = SCHEMA_DIR / "4.json"
NEW_TABLE = "period_balance_adjustments"


def migration_sql() -> list[str]:
    source = DATABASE_SOURCE.read_text(encoding="utf-8")
    block = source.split("val MIGRATION_3_4:", 1)[1].split("fun create(context", 1)[0]
    literals = re.findall(r'database\.execSQL\("((?:\\.|[^"\\])*)"\)', block)
    return [json.loads(f'"{literal}"') for literal in literals]


def entity(schema_path: Path, table: str) -> dict:
    schema = json.loads(schema_path.read_text(encoding="utf-8"))["database"]
    return next(e for e in schema["entities"] if e["tableName"] == table)


def normalize(sql: str, table: str) -> str:
    # SQLite drops the optional "IF NOT EXISTS" from stored DDL and the schema
    # file keeps a space before the closing paren; both are cosmetic.
    sql = sql.replace("`${TABLE_NAME}`", f"`{table}`").replace("IF NOT EXISTS ", "")
    sql = re.sub(r"\s+\)", ")", sql)
    return " ".join(sql.split())


def main() -> None:
    expected = entity(SCHEMA_V4, NEW_TABLE)
    v3_schema = json.loads(SCHEMA_V3.read_text(encoding="utf-8"))["database"]

    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys=ON")
    for e in v3_schema["entities"]:
        db.execute(normalize(e["createSql"], e["tableName"]))

    db.executemany(
        "INSERT INTO income_periods (id,startEpochDay,endEpochDay,incomeMinor,currency,frequency)"
        " VALUES (?,?,?,?,?,?)",
        [
            (1, 20000, 20006, 500000, "MXN", "SEMANAL"),
            (2, 20007, 20013, 480000, "MXN", "SEMANAL"),
        ],
    )
    db.executemany(
        "INSERT INTO expenses (concept,category,amountMinor,currency,exchangeRate,"
        "dateEpochMillis,cardId,note) VALUES (?,?,?,?,?,?,?,?)",
        [("Tacos", "Alimentos", 12050, "MXN", 1.0, 1728000000000, None, "")],
    )
    periods_before = db.execute("SELECT COUNT(*) FROM income_periods").fetchone()[0]

    statements = migration_sql()
    assert len(statements) == 1, f"Expected 1 migration statement, found {len(statements)}"
    for statement in statements:
        db.execute(statement)

    # The migrated table must be identical to the schema Room generates for v4.
    actual_sql = db.execute(
        "SELECT sql FROM sqlite_master WHERE type='table' AND name=?", (NEW_TABLE,)
    ).fetchone()[0]
    assert normalize(actual_sql, NEW_TABLE) == normalize(expected["createSql"], NEW_TABLE), (
        "Migration table does not match the Room v4 schema\n"
        f"  migrated: {normalize(actual_sql, NEW_TABLE)}\n"
        f"  expected: {normalize(expected['createSql'], NEW_TABLE)}"
    )

    columns_before = {r[1] for r in db.execute(f"PRAGMA table_info({NEW_TABLE})")}
    assert columns_before == {
        "periodId",
        "manualAdjustmentMinor",
        "carryoverMinor",
        "pendingCarryoverMinor",
        "rolloverResolved",
    }, f"Unexpected columns: {columns_before}"

    periods_after = db.execute("SELECT COUNT(*) FROM income_periods").fetchone()[0]
    assert periods_after == periods_before == 2, (
        f"Migration lost periods: before={periods_before} after={periods_after}"
    )

    # Rows can be stored, and deleting a period cascades its balance state.
    db.execute(
        f"INSERT INTO {NEW_TABLE} (periodId,manualAdjustmentMinor,carryoverMinor,"
        "pendingCarryoverMinor,rolloverResolved) VALUES (?,?,?,?,?)",
        (2, -5000, 30000, 0, 1),
    )
    stored = db.execute(
        f"SELECT periodId,carryoverMinor,manualAdjustmentMinor FROM {NEW_TABLE}"
    ).fetchall()
    assert stored == [(2, 30000, -5000)], f"Unexpected stored balance state: {stored!r}"

    # A balance row cannot reference a non-existent period.
    try:
        db.execute(
            f"INSERT INTO {NEW_TABLE} (periodId,manualAdjustmentMinor,carryoverMinor,"
            "pendingCarryoverMinor,rolloverResolved) VALUES (?,?,?,?,?)",
            (99, 0, 0, 0, 1),
        )
    except sqlite3.IntegrityError:
        pass
    else:
        raise AssertionError("Foreign key to income_periods was not enforced")

    db.execute("DELETE FROM income_periods WHERE id=2")
    remaining = db.execute(f"SELECT COUNT(*) FROM {NEW_TABLE}").fetchone()[0]
    assert remaining == 0, "Deleting a period did not cascade its balance state"

    assert not db.execute("PRAGMA foreign_key_check").fetchall()
    print(
        "PASS: 3->4 migration created the period balance table matching the Room v4 schema; "
        f"{periods_after} periods preserved, carryover and manual adjustment stored, "
        "foreign key enforced with cascade"
    )


if __name__ == "__main__":
    main()
