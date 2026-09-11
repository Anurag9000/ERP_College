#!/usr/bin/env python3
"""Fail-closed retained-source classifier for ERP_College.

ERP_College is a Java/Maven + frontend application, not an ML training repository.
This authority proves that classification from retained source/build manifests.  A
future ML framework dependency, optimizer/trainer API, or model-fitting primitive
causes the central runner to fail until that surface is explicitly catalogued.
"""
from __future__ import annotations

from dataclasses import asdict, dataclass
from pathlib import Path
import re
from typing import Iterator

ROOT = Path(__file__).resolve().parents[1]
CODE_SUFFIXES = {".py", ".java", ".js", ".jsx", ".ts", ".tsx", ".kt", ".scala"}
EXCLUDED_PREFIXES = ("training_control/", ".training_control/", ".git/")
FRAMEWORK_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("pytorch", re.compile(r"\b(torch|pytorch)\b", re.I)),
    ("tensorflow", re.compile(r"\b(tensorflow|keras)\b", re.I)),
    ("scikit-learn", re.compile(r"\b(sklearn|scikit[- ]learn)\b", re.I)),
    ("jax", re.compile(r"\b(jax|flax|optax)\b", re.I)),
    ("xgboost", re.compile(r"\bxgboost\b", re.I)),
    ("lightgbm", re.compile(r"\blightgbm\b", re.I)),
    ("catboost", re.compile(r"\bcatboost\b", re.I)),
    ("deeplearning4j", re.compile(r"\b(deeplearning4j|nd4j)\b", re.I)),
    ("tribuo", re.compile(r"\borg\.tribuo\b", re.I)),
    ("weka", re.compile(r"\bweka\b", re.I)),
    ("smile", re.compile(r"\bsmile\.classification|smile\.regression\b", re.I)),
    ("tensorflow-js", re.compile(r"@tensorflow/tfjs", re.I)),
    ("brain-js", re.compile(r"\bbrain\.js\b", re.I)),
    ("ml5", re.compile(r"\bml5\b", re.I)),
)
TRAINING_PRIMITIVE = re.compile(
    r"\.(fit|partial_fit|backward|train_step|minimize)\s*\(|\b(Optimizer|Trainer|TrainingArguments)\s*[<(]",
    re.I,
)


@dataclass(frozen=True, slots=True)
class Finding:
    path: str
    line: int
    kind: str
    token: str


@dataclass(frozen=True, slots=True)
class Audit:
    scanned_files: tuple[str, ...]
    findings: tuple[Finding, ...]

    @property
    def no_trainable_surface(self) -> bool:
        return not self.findings

    def to_dict(self) -> dict[str, object]:
        return {
            "schema_version": 1,
            "repository": "Anurag9000/ERP_College",
            "classification": "non_training_application" if self.no_trainable_surface else "trainable_surface_detected",
            "scanned_files": list(self.scanned_files),
            "findings": [asdict(row) for row in self.findings],
            "no_trainable_surface": self.no_trainable_surface,
            "source_configuration_only": True,
            "execution_claim_emitted": False,
            "training_claim_emitted": False,
        }


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def _candidate_files() -> Iterator[Path]:
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file():
            continue
        relative = _relative(path)
        if relative.startswith(EXCLUDED_PREFIXES):
            continue
        if path.suffix.lower() in CODE_SUFFIXES or relative in {"pom.xml", "package.json"}:
            yield path


def audit_no_trainable_surface() -> Audit:
    scanned: list[str] = []
    findings: list[Finding] = []
    for path in _candidate_files():
        relative = _relative(path)
        scanned.append(relative)
        text = path.read_text(encoding="utf-8", errors="replace")
        for line_number, line in enumerate(text.splitlines(), start=1):
            for label, pattern in FRAMEWORK_PATTERNS:
                if pattern.search(line):
                    findings.append(Finding(relative, line_number, "framework", label))
            if TRAINING_PRIMITIVE.search(line):
                findings.append(Finding(relative, line_number, "training_primitive", line.strip()[:160]))
    if not scanned:
        raise RuntimeError("ERP_College retained-source audit found no code/build files")
    return Audit(tuple(scanned), tuple(findings))


def require_no_trainable_surface() -> Audit:
    audit = audit_no_trainable_surface()
    if audit.findings:
        rendered = ", ".join(
            f"{row.path}:{row.line}:{row.token}" for row in audit.findings[:50]
        )
        raise RuntimeError(
            "ERP_College gained a retained trainable surface; wire it into the central "
            f"scientific DAG before continuing: {rendered}"
        )
    return audit
