from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
VERIFIER_PATH = REPO_ROOT / "tools" / "change_manifest_verify.py"


spec = importlib.util.spec_from_file_location("change_manifest_verify", VERIFIER_PATH)
assert spec is not None
assert spec.loader is not None
verifier = importlib.util.module_from_spec(spec)
sys.modules["change_manifest_verify"] = verifier
spec.loader.exec_module(verifier)


def minimal_manifest() -> dict:
    return {
        "protocol_version": "0.1",
        "producer": {
            "name": "codex",
            "version": "unknown",
        },
        "change": {
            "title": "Test manifest",
            "intent": "Validate the verifier.",
            "scope": "Unit test fixture.",
            "non_goals": [],
        },
        "workflow": [
            {
                "step": 1,
                "title": "Review file",
                "description": "Review the changed file.",
                "files": [
                    "src/example.txt",
                ],
            }
        ],
        "files": [
            {
                "path": "src/example.txt",
                "role": "modified",
                "reason": "Provides a stable test fixture.",
                "review_notes": [],
            }
        ],
        "verification": {
            "commands": [
                {
                    "command": "python -m unittest",
                    "status": "passed",
                    "evidence": "Unit tests passed.",
                }
            ],
            "manual_checks": [],
            "known_gaps": [],
        },
        "risks": [
            {
                "area": "Test",
                "risk": "The fixture may not cover all rules.",
                "mitigation": "Add focused tests for each rule.",
            }
        ],
        "agent_notes": {
            "assumptions": [],
            "limitations": [],
        },
    }


class ChangeManifestVerifierTest(unittest.TestCase):
    def test_valid_manifest_has_no_diagnostics(self) -> None:
        diagnostics = verifier.validate_manifest(minimal_manifest())

        self.assertEqual([], diagnostics)

    def test_workflow_file_must_be_listed(self) -> None:
        manifest = minimal_manifest()
        manifest["workflow"][0]["files"] = ["src/missing.txt"]

        diagnostics = verifier.validate_manifest(manifest)

        self.assertIn(
            "workflow.file_not_listed",
            [diagnostic.code for diagnostic in diagnostics],
        )

    def test_workflow_file_object_may_reference_listed_file(self) -> None:
        manifest = minimal_manifest()
        manifest["workflow"][0]["files"] = [
            {
                "path": "src/example.txt",
                "line": 3,
                "end_line": 5,
                "label": "Example target",
            }
        ]

        diagnostics = verifier.validate_manifest(manifest)

        self.assertEqual([], diagnostics)

    def test_workflow_file_object_requires_path(self) -> None:
        manifest = minimal_manifest()
        manifest["workflow"][0]["files"] = [
            {
                "line": 3,
            }
        ]

        diagnostics = verifier.validate_manifest(manifest)

        self.assertIn(
            "workflow.invalid_file_reference",
            [diagnostic.code for diagnostic in diagnostics],
        )

    def test_duplicate_file_is_error(self) -> None:
        manifest = minimal_manifest()
        manifest["files"].append(dict(manifest["files"][0]))

        diagnostics = verifier.validate_manifest(manifest)

        self.assertIn(
            "manifest.duplicate_file",
            [diagnostic.code for diagnostic in diagnostics],
        )

    def test_unsupported_protocol_is_error(self) -> None:
        manifest = minimal_manifest()
        manifest["protocol_version"] = "2.0"

        diagnostics = verifier.validate_manifest(manifest)

        self.assertIn(
            "manifest.unsupported_protocol",
            [diagnostic.code for diagnostic in diagnostics],
        )

    def test_read_manifest_reports_invalid_json(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            manifest_path = Path(directory) / "manifest.json"
            manifest_path.write_text("{not-json", encoding="utf-8")

            data, diagnostics = verifier.read_manifest(manifest_path)

        self.assertIsNone(data)
        self.assertEqual(["manifest.invalid_json"], [item.code for item in diagnostics])

    def test_repository_manifest_is_valid_without_diff(self) -> None:
        manifest_path = REPO_ROOT / ".change-manifest" / "manifest.json"
        data = json.loads(manifest_path.read_text(encoding="utf-8"))

        diagnostics = verifier.validate_manifest(data)

        self.assertEqual([], diagnostics)

    def test_diff_validation_reports_undocumented_changed_file(self) -> None:
        manifest = minimal_manifest()

        diagnostics = verifier.validate_diff(manifest, {"src/example.txt", "src/extra.txt"})

        self.assertIn(
            "diff.changed_file_not_documented",
            [diagnostic.code for diagnostic in diagnostics],
        )

    def test_diff_validation_reports_manifest_file_not_changed(self) -> None:
        manifest = minimal_manifest()

        diagnostics = verifier.validate_diff(manifest, set())

        self.assertIn(
            "diff.manifest_file_not_changed",
            [diagnostic.code for diagnostic in diagnostics],
        )

    def test_diagnostic_json_payload_marks_errors_not_ok(self) -> None:
        diagnostic = verifier.Diagnostic(
            "manifest.missing",
            "error",
            "Manifest file does not exist.",
            ".change-manifest/manifest.json",
        )

        payload = verifier.result_payload([diagnostic])

        self.assertFalse(payload["ok"])
        self.assertEqual("manifest.missing", payload["diagnostics"][0]["code"])

    def test_diagnostic_json_payload_marks_warnings_ok(self) -> None:
        diagnostic = verifier.Diagnostic(
            "diff.changed_file_not_documented",
            "warning",
            "Changed file is not listed.",
            "src/extra.txt",
        )

        payload = verifier.result_payload([diagnostic])

        self.assertTrue(payload["ok"])
        self.assertEqual("warning", payload["diagnostics"][0]["level"])


if __name__ == "__main__":
    unittest.main()
