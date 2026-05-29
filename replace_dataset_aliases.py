from __future__ import annotations

import json
import shutil
import sys
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
ALIASES_PATH = BASE_DIR / "dataset_aliases.json"
JSON_ROOT = BASE_DIR / "json"
BACKUP_ROOT = BASE_DIR / f"dataset_alias_replacement_backup_{datetime.now():%Y%m%d_%H%M%S}"
REPORT_PATH = BASE_DIR / "replace_dataset_aliases_report.json"


def normalize_text(value: object) -> str:
    if not isinstance(value, str):
        return ""
    return value.strip()


def load_alias_map() -> tuple[list[str], dict[str, str]]:
    data = json.loads(ALIASES_PATH.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise ValueError(f"{ALIASES_PATH.name} must contain a JSON array")

    kept_terms: list[str] = []
    alias_to_term: dict[str, str] = {}
    conflicts: dict[str, set[str]] = defaultdict(set)

    for entry in data:
        if not isinstance(entry, dict):
            continue

        kept_term = normalize_text(entry.get("term"))
        if not kept_term:
            continue

        kept_terms.append(kept_term)
        aliases = entry.get("aliases")
        if not isinstance(aliases, list):
            continue

        for raw_alias in aliases:
            alias = normalize_text(raw_alias)
            if not alias or alias == kept_term:
                continue

            previous_term = alias_to_term.get(alias)
            if previous_term and previous_term != kept_term:
                conflicts[alias].update({previous_term, kept_term})
            else:
                alias_to_term[alias] = kept_term

    if conflicts:
        print("Conflicting aliases found. No JSON files were changed.", file=sys.stderr)
        for alias, terms in sorted(conflicts.items()):
            joined_terms = ", ".join(sorted(terms))
            print(f"  {alias!r} -> {joined_terms}", file=sys.stderr)
        raise SystemExit(2)

    return kept_terms, alias_to_term


def replace_dataset_aliases(record: object, alias_to_term: dict[str, str]) -> list[dict[str, object]]:
    if not isinstance(record, dict):
        return []

    datasets = record.get("datasets_used")
    if not isinstance(datasets, list):
        return []

    changes: list[dict[str, object]] = []
    for index, item in enumerate(datasets):
        if isinstance(item, dict):
            old_value = item.get("name")
            if not isinstance(old_value, str):
                continue

            new_value = alias_to_term.get(old_value.strip())
            if not new_value or new_value == old_value:
                continue

            item["name"] = new_value
            changes.append({"index": index, "field": "name", "from": old_value, "to": new_value})
        elif isinstance(item, str):
            new_value = alias_to_term.get(item.strip())
            if not new_value or new_value == item:
                continue

            datasets[index] = new_value
            changes.append({"index": index, "field": "item", "from": item, "to": new_value})

    return changes


def main() -> int:
    if not ALIASES_PATH.is_file():
        print(f"Missing alias file: {ALIASES_PATH}", file=sys.stderr)
        return 1

    if not JSON_ROOT.is_dir():
        print(f"Missing JSON folder: {JSON_ROOT}", file=sys.stderr)
        return 1

    kept_terms, alias_to_term = load_alias_map()

    files_scanned = 0
    files_changed = 0
    total_replacements = 0
    replacement_counts: Counter[str] = Counter()
    changed_files: list[dict[str, object]] = []
    skipped_files: list[dict[str, str]] = []

    for json_path in sorted(JSON_ROOT.rglob("*.json")):
        files_scanned += 1

        try:
            original_text = json_path.read_text(encoding="utf-8")
            record = json.loads(original_text)
        except Exception as exc:  # Keep going so one bad file does not hide the full report.
            skipped_files.append(
                {
                    "path": str(json_path.relative_to(BASE_DIR)),
                    "reason": f"{type(exc).__name__}: {exc}",
                }
            )
            continue

        changes = replace_dataset_aliases(record, alias_to_term)
        if not changes:
            continue

        backup_path = BACKUP_ROOT / json_path.relative_to(BASE_DIR)
        backup_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(json_path, backup_path)

        json_path.write_text(
            json.dumps(record, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )

        files_changed += 1
        total_replacements += len(changes)
        for change in changes:
            replacement_counts[str(change["to"])] += 1

        changed_files.append(
            {
                "path": str(json_path.relative_to(BASE_DIR)),
                "changes": changes,
            }
        )

    report = {
        "aliases_file": str(ALIASES_PATH.relative_to(BASE_DIR)),
        "json_root": str(JSON_ROOT.relative_to(BASE_DIR)),
        "backup_root": str(BACKUP_ROOT.relative_to(BASE_DIR)) if files_changed else None,
        "kept_terms": kept_terms,
        "kept_term_count": len(kept_terms),
        "alias_count": len(alias_to_term),
        "files_scanned": files_scanned,
        "files_changed": files_changed,
        "total_replacements": total_replacements,
        "replacement_counts": dict(sorted(replacement_counts.items())),
        "changed_files": changed_files,
        "skipped_files": skipped_files,
    }
    REPORT_PATH.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(f"Kept terms loaded: {len(kept_terms)}")
    print(f"Aliases mapped: {len(alias_to_term)}")
    print(f"JSON files scanned: {files_scanned}")
    print(f"JSON files changed: {files_changed}")
    print(f"Dataset replacements: {total_replacements}")
    if files_changed:
        print(f"Backup folder: {BACKUP_ROOT}")
    print(f"Report written: {REPORT_PATH}")

    if skipped_files:
        print(f"Skipped files with JSON/read errors: {len(skipped_files)}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
