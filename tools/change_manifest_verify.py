#!/usr/bin/env python3
"""Validate a change manifest.

This verifier intentionally uses only the Python standard library. It is a small
portable first implementation for local use, CI, and plugin prototyping.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any


SUPPORTED_PROTOCOL_VERSION = "0.1"


@dataclass(frozen=True)
class Diagnostic:
    code: str
    level: str
    message: str
    path: str | None = None

    def render(self) -> str:
        location = f" [{self.path}]" if self.path else ""
        return f"{self.level.upper()} {self.code}{location}: {self.message}"

    def as_dict(self) -> dict[str, str | None]:
        return {
            "code": self.code,
            "level": self.level,
            "message": self.message,
            "path": self.path,
        }


def read_manifest(path: Path) -> tuple[dict[str, Any] | None, list[Diagnostic]]:
    if not path.exists():
        return None, [
            Diagnostic(
                "manifest.missing",
                "error",
                f"Manifest file does not exist: {path}",
                str(path),
            )
        ]

    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return None, [
            Diagnostic(
                "manifest.invalid_json",
                "error",
                f"Invalid JSON at line {exc.lineno}, column {exc.colno}: {exc.msg}",
                str(path),
            )
        ]

    if not isinstance(data, dict):
        return None, [
            Diagnostic(
                "manifest.invalid_json",
                "error",
                "Manifest root must be a JSON object.",
                str(path),
            )
        ]

    return data, []


def require_object(
    parent: dict[str, Any],
    key: str,
    path: str,
    diagnostics: list[Diagnostic],
) -> dict[str, Any] | None:
    value = parent.get(key)
    if value is None:
        diagnostics.append(
            Diagnostic(
                "manifest.missing_required_field",
                "error",
                f"Missing required field: {path}.{key}",
                f"{path}.{key}",
            )
        )
        return None
    if not isinstance(value, dict):
        diagnostics.append(
            Diagnostic(
                "manifest.invalid_type",
                "error",
                f"Expected object at {path}.{key}",
                f"{path}.{key}",
            )
        )
        return None
    return value


def require_array(
    parent: dict[str, Any],
    key: str,
    path: str,
    diagnostics: list[Diagnostic],
) -> list[Any] | None:
    value = parent.get(key)
    if value is None:
        diagnostics.append(
            Diagnostic(
                "manifest.missing_required_field",
                "error",
                f"Missing required field: {path}.{key}",
                f"{path}.{key}",
            )
        )
        return None
    if not isinstance(value, list):
        diagnostics.append(
            Diagnostic(
                "manifest.invalid_type",
                "error",
                f"Expected array at {path}.{key}",
                f"{path}.{key}",
            )
        )
        return None
    return value


def require_string(
    parent: dict[str, Any],
    key: str,
    path: str,
    diagnostics: list[Diagnostic],
) -> str | None:
    value = parent.get(key)
    if value is None:
        diagnostics.append(
            Diagnostic(
                "manifest.missing_required_field",
                "error",
                f"Missing required field: {path}.{key}",
                f"{path}.{key}",
            )
        )
        return None
    if not isinstance(value, str) or not value.strip():
        diagnostics.append(
            Diagnostic(
                "manifest.invalid_type",
                "error",
                f"Expected non-empty string at {path}.{key}",
                f"{path}.{key}",
            )
        )
        return None
    return value


def validate_manifest(data: dict[str, Any]) -> list[Diagnostic]:
    diagnostics: list[Diagnostic] = []

    protocol_version = require_string(data, "protocol_version", "$", diagnostics)
    if protocol_version and protocol_version != SUPPORTED_PROTOCOL_VERSION:
        diagnostics.append(
            Diagnostic(
                "manifest.unsupported_protocol",
                "error",
                f"Unsupported protocol version: {protocol_version}",
                "$.protocol_version",
            )
        )

    producer = require_object(data, "producer", "$", diagnostics)
    if producer is not None:
        require_string(producer, "name", "$.producer", diagnostics)

    change = require_object(data, "change", "$", diagnostics)
    if change is not None:
        for key in ("title", "intent", "scope"):
            require_string(change, key, "$.change", diagnostics)
        require_array(change, "non_goals", "$.change", diagnostics)

    workflow = require_array(data, "workflow", "$", diagnostics)
    files = require_array(data, "files", "$", diagnostics)

    manifest_paths: set[str] = set()
    if files is not None:
        for index, item in enumerate(files):
            item_path = f"$.files[{index}]"
            if not isinstance(item, dict):
                diagnostics.append(
                    Diagnostic(
                        "manifest.invalid_type",
                        "error",
                        f"Expected object at {item_path}",
                        item_path,
                    )
                )
                continue

            file_path = require_string(item, "path", item_path, diagnostics)
            role = require_string(item, "role", item_path, diagnostics)
            require_string(item, "reason", item_path, diagnostics)
            require_array(item, "review_notes", item_path, diagnostics)

            if role and role not in {"added", "modified", "deleted", "moved"}:
                diagnostics.append(
                    Diagnostic(
                        "manifest.invalid_enum",
                        "error",
                        f"Invalid file role: {role}",
                        f"{item_path}.role",
                    )
                )

            if file_path:
                if file_path in manifest_paths:
                    diagnostics.append(
                        Diagnostic(
                            "manifest.duplicate_file",
                            "error",
                            f"Duplicate file path: {file_path}",
                            f"{item_path}.path",
                        )
                    )
                manifest_paths.add(file_path)

    if workflow is not None:
        seen_steps: set[int] = set()
        for index, item in enumerate(workflow):
            item_path = f"$.workflow[{index}]"
            if not isinstance(item, dict):
                diagnostics.append(
                    Diagnostic(
                        "manifest.invalid_type",
                        "error",
                        f"Expected object at {item_path}",
                        item_path,
                    )
                )
                continue

            step = item.get("step")
            if not isinstance(step, int) or step < 1:
                diagnostics.append(
                    Diagnostic(
                        "manifest.invalid_type",
                        "error",
                        f"Expected positive integer at {item_path}.step",
                        f"{item_path}.step",
                    )
                )
            elif step in seen_steps:
                diagnostics.append(
                    Diagnostic(
                        "workflow.duplicate_step",
                        "error",
                        f"Duplicate workflow step: {step}",
                        f"{item_path}.step",
                    )
                )
            else:
                seen_steps.add(step)

            require_string(item, "title", item_path, diagnostics)
            require_string(item, "description", item_path, diagnostics)
            workflow_files = require_array(item, "files", item_path, diagnostics)

            if workflow_files is None:
                continue
            for file_index, workflow_file in enumerate(workflow_files):
                workflow_file_path = workflow_file_reference_path(workflow_file)
                if workflow_file_path is None:
                    diagnostics.append(
                        Diagnostic(
                            "workflow.invalid_file_reference",
                            "error",
                            f"Expected string path or object with path at {item_path}.files[{file_index}]",
                            f"{item_path}.files[{file_index}]",
                        )
                    )
                    continue
                if workflow_file_path not in manifest_paths:
                    diagnostics.append(
                        Diagnostic(
                            "workflow.file_not_listed",
                            "error",
                            f"Workflow references file not listed in files: {workflow_file_path}",
                            f"{item_path}.files[{file_index}]",
                        )
                    )

    verification = require_object(data, "verification", "$", diagnostics)
    if verification is not None:
        commands = require_array(verification, "commands", "$.verification", diagnostics)
        require_array(verification, "manual_checks", "$.verification", diagnostics)
        require_array(verification, "known_gaps", "$.verification", diagnostics)

        if commands is not None:
            for index, item in enumerate(commands):
                item_path = f"$.verification.commands[{index}]"
                if not isinstance(item, dict):
                    diagnostics.append(
                        Diagnostic(
                            "manifest.invalid_type",
                            "error",
                            f"Expected object at {item_path}",
                            item_path,
                        )
                    )
                    continue
                command = require_string(item, "command", item_path, diagnostics)
                status = require_string(item, "status", item_path, diagnostics)
                if command is not None and not command.strip():
                    diagnostics.append(
                        Diagnostic(
                            "verification.empty_command",
                            "error",
                            "Verification command is empty.",
                            f"{item_path}.command",
                        )
                    )
                if status and status not in {"passed", "failed", "not_run", "unknown"}:
                    diagnostics.append(
                        Diagnostic(
                            "manifest.invalid_enum",
                            "error",
                            f"Invalid verification status: {status}",
                            f"{item_path}.status",
                        )
                    )
                if status == "passed" and not str(item.get("evidence", "")).strip():
                    diagnostics.append(
                        Diagnostic(
                            "verification.passed_without_evidence",
                            "warning",
                            "Command is marked passed without evidence.",
                            f"{item_path}.evidence",
                        )
                    )

    risks = require_array(data, "risks", "$", diagnostics)
    if risks is not None:
        for index, item in enumerate(risks):
            item_path = f"$.risks[{index}]"
            if not isinstance(item, dict):
                diagnostics.append(
                    Diagnostic(
                        "manifest.invalid_type",
                        "error",
                        f"Expected object at {item_path}",
                        item_path,
                    )
                )
                continue
            for key in ("area", "risk", "mitigation"):
                require_string(item, key, item_path, diagnostics)

    agent_notes = require_object(data, "agent_notes", "$", diagnostics)
    if agent_notes is not None:
        require_array(agent_notes, "assumptions", "$.agent_notes", diagnostics)
        require_array(agent_notes, "limitations", "$.agent_notes", diagnostics)

    return diagnostics


def workflow_file_reference_path(value: Any) -> str | None:
    if isinstance(value, str) and value.strip():
        return value

    if isinstance(value, dict):
        path = value.get("path")
        if isinstance(path, str) and path.strip():
            return path

    return None


def run_git_names(command: list[str]) -> tuple[set[str] | None, Diagnostic | None]:
    try:
        result = subprocess.run(
            command,
            check=False,
            capture_output=True,
            text=True,
        )
    except OSError as exc:
        return None, (
            Diagnostic(
                "diff.unavailable",
                "warning",
                f"Could not run git diff: {exc}",
            )
        )

    if result.returncode != 0:
        message = result.stderr.strip() or result.stdout.strip() or "git diff failed"
        return None, Diagnostic("diff.unavailable", "warning", message)

    return {line.strip() for line in result.stdout.splitlines() if line.strip()}, None


def get_changed_files(base: str | None) -> tuple[set[str] | None, list[Diagnostic]]:
    if base:
        changed, diagnostic = run_git_names(["git", "diff", "--name-only", base])
        return changed, [diagnostic] if diagnostic else []

    commands = [
        ["git", "diff", "--name-only"],
        ["git", "diff", "--name-only", "--cached"],
        ["git", "ls-files", "--others", "--exclude-standard"],
    ]

    changed_files: set[str] = set()
    diagnostics: list[Diagnostic] = []
    for command in commands:
        changed, diagnostic = run_git_names(command)
        if diagnostic is not None:
            diagnostics.append(diagnostic)
            continue
        if changed is not None:
            changed_files.update(changed)

    if diagnostics:
        return None, diagnostics

    return changed_files, []


def validate_diff(
    data: dict[str, Any],
    changed_files: set[str] | None,
) -> list[Diagnostic]:
    if changed_files is None:
        return []

    files = data.get("files")
    if not isinstance(files, list):
        return []

    manifest_paths = {
        item.get("path")
        for item in files
        if isinstance(item, dict) and isinstance(item.get("path"), str)
    }

    diagnostics: list[Diagnostic] = []
    for path in sorted(changed_files - manifest_paths):
        diagnostics.append(
            Diagnostic(
                "diff.changed_file_not_documented",
                "warning",
                f"Changed file is not listed in the manifest: {path}",
                path,
            )
        )

    for path in sorted(manifest_paths - changed_files):
        diagnostics.append(
            Diagnostic(
                "diff.manifest_file_not_changed",
                "warning",
                f"Manifest file is not present in git diff: {path}",
                path,
            )
        )

    return diagnostics


def has_errors(diagnostics: list[Diagnostic]) -> bool:
    return any(diagnostic.level == "error" for diagnostic in diagnostics)


def has_warnings(diagnostics: list[Diagnostic]) -> bool:
    return any(diagnostic.level == "warning" for diagnostic in diagnostics)


def result_payload(diagnostics: list[Diagnostic]) -> dict[str, Any]:
    return {
        "ok": not has_errors(diagnostics),
        "diagnostics": [diagnostic.as_dict() for diagnostic in diagnostics],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate a change manifest.")
    parser.add_argument(
        "--manifest",
        default=".change-manifest/manifest.json",
        help="Path to the manifest JSON file.",
    )
    parser.add_argument(
        "--base",
        default=None,
        help="Optional Git base revision for diff validation.",
    )
    parser.add_argument(
        "--skip-diff",
        action="store_true",
        help="Skip Git diff validation.",
    )
    parser.add_argument(
        "--format",
        choices=("text", "json"),
        default="text",
        help="Output format.",
    )
    parser.add_argument(
        "--warnings-as-errors",
        action="store_true",
        help="Exit with failure when warnings are present.",
    )
    args = parser.parse_args()

    manifest_path = Path(args.manifest)
    data, diagnostics = read_manifest(manifest_path)
    if data is not None:
        diagnostics.extend(validate_manifest(data))
        if not args.skip_diff:
            changed_files, diff_diagnostics = get_changed_files(args.base)
            diagnostics.extend(diff_diagnostics)
            diagnostics.extend(validate_diff(data, changed_files))

    if args.format == "json":
        print(json.dumps(result_payload(diagnostics), indent=2, sort_keys=True))
    else:
        for diagnostic in diagnostics:
            print(diagnostic.render())

    if has_errors(diagnostics):
        return 1

    if args.warnings_as_errors and has_warnings(diagnostics):
        return 1

    if diagnostics:
        return 0

    if args.format == "text":
        print("OK change manifest is valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
