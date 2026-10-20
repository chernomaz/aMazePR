# Full Loop

This repository now has the first complete change-manifest loop.

## Loop

1. A coding agent changes files.
2. The agent uses `skills/pr-manifest-skill/SKILL.md`.
3. The agent writes `.change-manifest/manifest.json`.
4. The verifier checks the manifest.
5. The renderer creates a walkthrough.
6. The PyCharm plugin displays the walkthrough.
7. CI runs tests, manifest verification, example verification, and walkthrough rendering.

## Commands

Run tests:

```bash
python -m unittest discover -s tests
```

Verify the repository manifest:

```bash
python tools/change_manifest_verify.py --manifest .change-manifest/manifest.json --warnings-as-errors
```

Emit machine-readable diagnostics:

```bash
python tools/change_manifest_verify.py --manifest .change-manifest/manifest.json --format json
```

Render the walkthrough:

```bash
python tools/change_manifest_walkthrough.py --manifest .change-manifest/manifest.json --output .change-manifest/walkthrough.md
```

Open the same repository in PyCharm with the plugin installed. The `aMazePR` tool window
loads `.change-manifest/manifest.json`.

In the plugin, choose **Working tree** for `HEAD` versus local edits. Choose **Pull request**
and enter a **Base branch** such as `origin/main` for merge base versus committed `HEAD`.
Fetch the target branch before use. Workflow objects with `line` and optional `end_line`
select that range in the diff; deleted files use the old side.

Build the PyCharm plugin:

```powershell
cd plugin/pycharm
$env:JAVA_HOME = (Resolve-Path '..\..\.tools\jdk-21').Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
..\..\.tools\gradle-9.8.1\bin\gradle.bat build
..\..\.tools\gradle-9.8.1\bin\gradle.bat buildPlugin
```

Verify example manifests:

```bash
python tools/change_manifest_verify.py --manifest docs/examples/feature.json --skip-diff
```

## Ready State

The loop is ready when:

- tests pass
- strict manifest verification passes
- JSON diagnostics report `ok: true`
- walkthrough rendering succeeds
- the PyCharm plugin can display the rendered walkthrough
- the PyCharm plugin build succeeds
- example manifests validate
- CI runs the same checks
