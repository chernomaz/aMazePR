from __future__ import annotations

import importlib.util
import json
import sys
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
TOOLS_ROOT = REPO_ROOT / "tools"
VERIFIER_PATH = TOOLS_ROOT / "change_manifest_verify.py"
WALKTHROUGH_PATH = TOOLS_ROOT / "change_manifest_walkthrough.py"


verifier_spec = importlib.util.spec_from_file_location(
    "change_manifest_verify",
    VERIFIER_PATH,
)
assert verifier_spec is not None
assert verifier_spec.loader is not None
verifier = importlib.util.module_from_spec(verifier_spec)
sys.modules["change_manifest_verify"] = verifier
verifier_spec.loader.exec_module(verifier)

walkthrough_spec = importlib.util.spec_from_file_location(
    "change_manifest_walkthrough",
    WALKTHROUGH_PATH,
)
assert walkthrough_spec is not None
assert walkthrough_spec.loader is not None
walkthrough = importlib.util.module_from_spec(walkthrough_spec)
sys.modules["change_manifest_walkthrough"] = walkthrough
walkthrough_spec.loader.exec_module(walkthrough)


class ChangeManifestWalkthroughTest(unittest.TestCase):
    def test_repository_manifest_renders_core_sections(self) -> None:
        manifest_path = REPO_ROOT / ".change-manifest" / "manifest.json"
        data = json.loads(manifest_path.read_text(encoding="utf-8"))

        markdown = walkthrough.render_walkthrough(data, [])

        self.assertTrue(markdown.startswith(f"# {data['change']['title']}\n"))
        self.assertIn("## Intent", markdown)
        self.assertIn("## Review Workflow", markdown)
        self.assertIn("## Files", markdown)
        self.assertIn("## Verification", markdown)
        self.assertIn("## Risks", markdown)

    def test_diagnostics_render_near_top(self) -> None:
        manifest_path = REPO_ROOT / ".change-manifest" / "manifest.json"
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
        diagnostic = verifier.Diagnostic(
            "workflow.file_not_listed",
            "error",
            "Workflow file is missing.",
            "$.workflow[0].files[0]",
        )

        markdown = walkthrough.render_walkthrough(data, [diagnostic])

        self.assertIn("## Diagnostics", markdown)
        self.assertIn("**ERROR workflow.file_not_listed**", markdown)
        self.assertLess(markdown.index("## Diagnostics"), markdown.index("## Intent"))

    def test_workflow_file_object_renders_line_target_and_label(self) -> None:
        data = {
            "change": {
                "title": "Line target test",
                "intent": "Check renderer support for file objects.",
                "scope": "Renderer test.",
                "non_goals": [],
            },
            "workflow": [
                {
                    "step": 1,
                    "title": "Review target",
                    "description": "Open the exact target.",
                    "files": [
                        {
                            "path": "src/example.py",
                            "line": 10,
                            "end_line": 12,
                            "label": "Validation branch",
                        }
                    ],
                }
            ],
            "files": [],
            "verification": {
                "commands": [],
                "manual_checks": [],
                "known_gaps": [],
            },
            "risks": [],
            "agent_notes": {
                "assumptions": [],
                "limitations": [],
            },
        }

        markdown = walkthrough.render_walkthrough(data, [])

        self.assertTrue(markdown.startswith("# Line target test\n"))
        self.assertIn("`src/example.py:10-12` - Validation branch", markdown)


if __name__ == "__main__":
    unittest.main()
