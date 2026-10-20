# Implementation Plan

## Phase 1: Protocol Package

Status: started and buildable.

Deliverables:

- `docs/change-manifest-protocol.md`
- `docs/manifest.schema.json`
- `docs/examples/*.json`
- `skills/pr-manifest-skill/SKILL.md`
- `.change-manifest/manifest.json`
- `tools/change_manifest_verify.py`
- `tools/change_manifest_walkthrough.py`
- `.github/workflows/verify.yml`

Acceptance criteria:

- The protocol has a default manifest path.
- The schema defines required fields.
- Examples cover common PR shapes.
- The agent skill tells producers how to create truthful manifests.
- The repository contains its own manifest as a dogfood example.
- A first verifier can validate the manifest structure.
- A first renderer can turn the manifest into a review walkthrough.
- CI runs verifier tests and manifest checks.

## Phase 2: Core Manifest Library

Build a small core library that is independent of PyCharm UI code.

Responsibilities:

- Load manifest JSON.
- Parse it into typed objects.
- Validate required fields.
- Report structured diagnostics.
- Compare manifest paths with the Git diff.

Suggested modules:

```text
manifest/model
manifest/parser
manifest/schema
manifest/verify
```

The PyCharm plugin and future CLI verifier should both use this core layer.

## Phase 3: Diff Verification

The verifier should compare the manifest with the current change set.

Checks:

- Manifest file exists.
- Manifest JSON parses.
- Protocol version is supported.
- All required sections exist.
- Workflow file references are present in `files`.
- Manifest file paths are unique.
- Changed files are listed in the manifest, unless ignored by policy.
- Manifest files exist in the diff or have a valid reason.

Diagnostic levels:

- `error`: blocks trusted rendering.
- `warning`: allows rendering with visible caution.
- `info`: gives useful context.

## Phase 4: PyCharm Viewer

Status: started.

Render the manifest as a walkthrough.

Sections:

- Change intent
- Review workflow
- Files
- Verification
- Risks
- Diagnostics

The viewer must keep links to the real diff. The manifest explains the review path; it
does not replace the diff.

The first CLI renderer is:

```text
python tools/change_manifest_walkthrough.py --manifest .change-manifest/manifest.json
```

It is a reference for the PyCharm viewer.

The first plugin scaffold is:

```text
plugin/pycharm
```

It loads `.change-manifest/walkthrough.md` into an `aMazePR` tool window.

The plugin builds against the local PyCharm Community Edition 2024.3 installation.

## Phase 5: CLI Verifier

Add a command that CI can run.

Example:

```text
change-manifest verify --manifest .change-manifest/manifest.json --base main
```

The CLI should use the same core library as the plugin.

The initial portable script is:

```text
python tools/change_manifest_verify.py --manifest .change-manifest/manifest.json
```

For tool integrations, the script can emit JSON:

```text
python tools/change_manifest_verify.py --format json
```

## Phase 6: Interoperability

Prepare the protocol for other producers and consumers.

Tasks:

- Document compatibility rules.
- Add producer examples for Codex and Claude.
- Add CI examples.
- Add GitHub App notes.
- Add a changelog for protocol versions.
