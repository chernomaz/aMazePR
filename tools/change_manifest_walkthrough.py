#!/usr/bin/env python3
"""Render a change manifest as a review walkthrough."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any

import change_manifest_verify as verifier


def bullet_list(items: list[Any]) -> list[str]:
    return [f"- {item}" for item in items if isinstance(item, str) and item.strip()]


def render_diagnostics(diagnostics: list[verifier.Diagnostic]) -> list[str]:
    if not diagnostics:
        return []

    lines = ["## Diagnostics", ""]
    for diagnostic in diagnostics:
        location = f" `{diagnostic.path}`" if diagnostic.path else ""
        lines.append(
            f"- **{diagnostic.level.upper()} {diagnostic.code}**{location}: "
            f"{diagnostic.message}"
        )
    lines.append("")
    return lines


def render_change(change: dict[str, Any]) -> list[str]:
    lines = [
        "## Intent",
        "",
        str(change.get("intent", "")).strip(),
        "",
        "## Scope",
        "",
        str(change.get("scope", "")).strip(),
        "",
    ]

    non_goals = change.get("non_goals", [])
    if isinstance(non_goals, list) and non_goals:
        lines.extend(["## Non-Goals", ""])
        lines.extend(bullet_list(non_goals))
        lines.append("")

    return lines


def render_workflow(workflow: list[Any]) -> list[str]:
    lines = ["## Review Workflow", ""]
    for item in workflow:
        if not isinstance(item, dict):
            continue
        lines.extend(
            [
                f"### {item.get('step')}. {item.get('title')}",
                "",
                str(item.get("description", "")).strip(),
                "",
            ]
        )
        files = item.get("files", [])
        if isinstance(files, list) and files:
            lines.append("Files:")
            lines.extend(render_workflow_file_reference(file) for file in files)
            lines.append("")
    return lines


def render_workflow_file_reference(file: Any) -> str:
    if isinstance(file, str):
        return f"- `{file}`"

    if not isinstance(file, dict):
        return "- `<invalid file reference>`"

    path = str(file.get("path", "")).strip()
    label = str(file.get("label", "")).strip()
    line = file.get("line")
    end_line = file.get("end_line")

    target = path
    if isinstance(line, int) and isinstance(end_line, int) and line != end_line:
        target = f"{path}:{line}-{end_line}"
    elif isinstance(line, int):
        target = f"{path}:{line}"

    if label:
        return f"- `{target}` - {label}"

    return f"- `{target}`"


def render_files(files: list[Any]) -> list[str]:
    lines = ["## Files", ""]
    for item in files:
        if not isinstance(item, dict):
            continue
        path = str(item.get("path", "")).strip()
        role = str(item.get("role", "")).strip()
        reason = str(item.get("reason", "")).strip()
        lines.extend([f"### `{path}`", "", f"Role: `{role}`", "", reason, ""])

        review_notes = item.get("review_notes", [])
        if isinstance(review_notes, list) and review_notes:
            lines.append("Review notes:")
            lines.extend(bullet_list(review_notes))
            lines.append("")
    return lines


def render_verification(verification: dict[str, Any]) -> list[str]:
    lines = ["## Verification", ""]

    commands = verification.get("commands", [])
    if isinstance(commands, list) and commands:
        lines.append("Commands:")
        for item in commands:
            if not isinstance(item, dict):
                continue
            command = str(item.get("command", "")).strip()
            status = str(item.get("status", "")).strip()
            evidence = str(item.get("evidence", "")).strip()
            line = f"- `{status}` `{command}`"
            if evidence:
                line = f"{line}: {evidence}"
            lines.append(line)
        lines.append("")

    manual_checks = verification.get("manual_checks", [])
    if isinstance(manual_checks, list) and manual_checks:
        lines.append("Manual checks:")
        lines.extend(bullet_list(manual_checks))
        lines.append("")

    known_gaps = verification.get("known_gaps", [])
    if isinstance(known_gaps, list) and known_gaps:
        lines.append("Known gaps:")
        lines.extend(bullet_list(known_gaps))
        lines.append("")

    return lines


def render_risks(risks: list[Any]) -> list[str]:
    if not risks:
        return []

    lines = ["## Risks", ""]
    for item in risks:
        if not isinstance(item, dict):
            continue
        area = str(item.get("area", "")).strip()
        risk = str(item.get("risk", "")).strip()
        mitigation = str(item.get("mitigation", "")).strip()
        lines.extend([f"### {area}", "", f"Risk: {risk}", "", f"Mitigation: {mitigation}", ""])
    return lines


def render_agent_notes(agent_notes: dict[str, Any]) -> list[str]:
    lines: list[str] = []
    assumptions = agent_notes.get("assumptions", [])
    limitations = agent_notes.get("limitations", [])

    if isinstance(assumptions, list) and assumptions:
        lines.extend(["## Assumptions", ""])
        lines.extend(bullet_list(assumptions))
        lines.append("")

    if isinstance(limitations, list) and limitations:
        lines.extend(["## Limitations", ""])
        lines.extend(bullet_list(limitations))
        lines.append("")

    return lines


def render_walkthrough(
    data: dict[str, Any],
    diagnostics: list[verifier.Diagnostic],
) -> str:
    change = data.get("change", {})
    title = "Untitled Change"
    if isinstance(change, dict):
        title = str(change.get("title", title)).strip() or title

    lines = [f"# {title}", ""]
    lines.extend(render_diagnostics(diagnostics))

    if isinstance(change, dict):
        lines.extend(render_change(change))

    workflow = data.get("workflow", [])
    if isinstance(workflow, list):
        lines.extend(render_workflow(workflow))

    files = data.get("files", [])
    if isinstance(files, list):
        lines.extend(render_files(files))

    verification = data.get("verification", {})
    if isinstance(verification, dict):
        lines.extend(render_verification(verification))

    risks = data.get("risks", [])
    if isinstance(risks, list):
        lines.extend(render_risks(risks))

    agent_notes = data.get("agent_notes", {})
    if isinstance(agent_notes, dict):
        lines.extend(render_agent_notes(agent_notes))

    return "\n".join(lines).rstrip() + "\n"


def build_walkthrough(
    manifest_path: Path,
    base: str | None,
    skip_diff: bool,
) -> tuple[str | None, list[verifier.Diagnostic]]:
    data, diagnostics = verifier.read_manifest(manifest_path)
    if data is None:
        return None, diagnostics

    diagnostics.extend(verifier.validate_manifest(data))
    if not skip_diff:
        changed_files, diff_diagnostics = verifier.get_changed_files(base)
        diagnostics.extend(diff_diagnostics)
        diagnostics.extend(verifier.validate_diff(data, changed_files))

    return render_walkthrough(data, diagnostics), diagnostics


def main() -> int:
    parser = argparse.ArgumentParser(description="Render a change manifest walkthrough.")
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
        "--output",
        default=None,
        help="Optional path for the rendered Markdown walkthrough.",
    )
    args = parser.parse_args()

    markdown, diagnostics = build_walkthrough(
        Path(args.manifest),
        args.base,
        args.skip_diff,
    )

    if markdown is None:
        for diagnostic in diagnostics:
            print(diagnostic.render(), file=sys.stderr)
        return 1

    if args.output:
        Path(args.output).write_text(markdown, encoding="utf-8")
    else:
        print(markdown, end="")

    return 1 if verifier.has_errors(diagnostics) else 0


if __name__ == "__main__":
    sys.exit(main())
