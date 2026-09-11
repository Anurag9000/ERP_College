#!/usr/bin/env python3
"""Emit the ERP_College fail-closed no-training certificate."""
from __future__ import annotations

import json
from pathlib import Path
import sys
import uuid

ROOT = Path(__file__).resolve().parents[1]
CONTROL = ROOT / "training_control"
if str(CONTROL) not in sys.path:
    sys.path.insert(0, str(CONTROL))

from erp_college_no_training_authority_v1 import audit_no_trainable_surface  # noqa: E402

OUTPUT = ROOT / "artifacts" / "training_control" / "no_trainable_surface_v1.json"


def _atomic_json(path: Path, payload: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f".{path.name}.tmp-{uuid.uuid4().hex}")
    temporary.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    temporary.replace(path)


def main() -> int:
    audit = audit_no_trainable_surface()
    payload = audit.to_dict()
    payload["status"] = "PASS" if audit.no_trainable_surface else "FAIL"
    payload["scanned_file_count"] = len(audit.scanned_files)
    _atomic_json(OUTPUT, payload)
    print(json.dumps(payload, indent=2, sort_keys=True))
    return 0 if audit.no_trainable_surface else 2


if __name__ == "__main__":
    raise SystemExit(main())
