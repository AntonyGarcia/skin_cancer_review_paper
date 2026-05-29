from __future__ import annotations

import argparse
import json
import shutil
from datetime import datetime
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
COMPLIANT_DIR = BASE_DIR / "json" / "compliant"
NON_COMPLIANT_DIR = BASE_DIR / "json" / "non_compliant"
KEEP_METRICS = {
    "95% Confidence Interval (CI)",
    "AUC-ROC",
    "PR AUC",
    "Statistical Tests",
}
TIMESTAMP = datetime.now().strftime("%Y%m%d_%H%M%S")
REPORT_PATH = BASE_DIR / f"move_non_target_metric_papers_report_{TIMESTAMP}.json"


def normalized_metric_set(record: object) -> set[str]:
    if not isinstance(record, dict):
        return set()

    metrics = record.get("metrics_used_to_evaluate_results")
    if not isinstance(metrics, list):
        return set()

    return {
        metric.strip()
        for metric in metrics
        if isinstance(metric, str) and metric.strip() and metric.strip().lower() != "null"
    }


def unique_destination(source: Path) -> Path:
    destination = NON_COMPLIANT_DIR / source.name
    if not destination.exists():
        return destination

    stem = source.stem
    suffix = source.suffix
    counter = 1
    while True:
        candidate = NON_COMPLIANT_DIR / f"{stem}__dup{counter}{suffix}"
        if not candidate.exists():
            return candidate
        counter += 1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Move compliant JSON files to non_compliant when "
            "metrics_used_to_evaluate_results does not contain a target metric."
        )
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Write the report without moving files.",
    )
    parser.add_argument(
        "--require-all",
        action="store_true",
        help="Keep a paper only if it contains all target metrics instead of any target metric.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if not COMPLIANT_DIR.is_dir():
        print(f"Missing compliant folder: {COMPLIANT_DIR}")
        return 1

    NON_COMPLIANT_DIR.mkdir(parents=True, exist_ok=True)

    scanned = 0
    kept = 0
    moved = 0
    skipped = []
    kept_files = []
    moved_files = []
    target_metrics_sorted = sorted(KEEP_METRICS)

    for source in sorted(COMPLIANT_DIR.rglob("*.json")):
        scanned += 1

        try:
            record = json.loads(source.read_text(encoding="utf-8"))
        except Exception as exc:
            destination = unique_destination(source)
            skipped.append(
                {
                    "source": str(source.relative_to(BASE_DIR)),
                    "intended_destination": str(destination.relative_to(BASE_DIR)),
                    "reason": f"{type(exc).__name__}: {exc}",
                }
            )
            continue

        metrics = normalized_metric_set(record)
        matching_metrics = sorted(metrics & KEEP_METRICS)
        should_keep = (
            set(matching_metrics) == KEEP_METRICS
            if args.require_all
            else bool(matching_metrics)
        )

        if should_keep:
            kept += 1
            kept_files.append(
                {
                    "path": str(source.relative_to(BASE_DIR)),
                    "matching_metrics": matching_metrics,
                }
            )
            continue

        destination = unique_destination(source)
        moved += 1
        moved_files.append(
            {
                "source": str(source.relative_to(BASE_DIR)),
                "destination": str(destination.relative_to(BASE_DIR)),
                "metrics_found": sorted(metrics),
            }
        )

        if not args.dry_run:
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(source), str(destination))

    report = {
        "dry_run": args.dry_run,
        "rule": "require_all" if args.require_all else "require_any",
        "target_metrics": target_metrics_sorted,
        "compliant_dir": str(COMPLIANT_DIR.relative_to(BASE_DIR)),
        "non_compliant_dir": str(NON_COMPLIANT_DIR.relative_to(BASE_DIR)),
        "files_scanned": scanned,
        "files_kept": kept,
        "files_moved": moved,
        "files_skipped": len(skipped),
        "kept_files": kept_files,
        "moved_files": moved_files,
        "skipped_files": skipped,
    }
    REPORT_PATH.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(f"Rule: {'all target metrics required' if args.require_all else 'any target metric required'}")
    print("Target metrics:")
    for metric in target_metrics_sorted:
        print(f"  - {metric}")
    print(f"Compliant JSON files scanned: {scanned}")
    print(f"Files kept compliant: {kept}")
    print(f"Files moved to non_compliant: {moved}")
    print(f"Files skipped due to read/JSON errors: {len(skipped)}")
    print(f"Report written: {REPORT_PATH}")
    if args.dry_run:
        print("Dry run only: no files were moved.")

    return 1 if skipped else 0


if __name__ == "__main__":
    raise SystemExit(main())
