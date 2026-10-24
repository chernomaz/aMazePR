# aMazePR

aMazePR is an open change-manifest protocol for coding agents.

A coding agent writes a structured manifest after it changes code. A viewer, verifier,
CI job, GitHub App, or IDE can then use the manifest to understand the change with less
repeat analysis.

The first intended viewer is a PyCharm plugin. The protocol is not PyCharm-specific.

## What Is In This Repository

- `docs/change-manifest-protocol.md`: protocol design.
- `docs/full-loop.md`: end-to-end loop commands.
- `docs/manifest.schema.json`: schema for protocol version `0.1`.
- `docs/pycharm-plugin.md`: first PyCharm plugin viewer notes.
- `docs/verifier-rules.md`: verifier diagnostics and rules.
- `docs/verifier-cli.md`: CLI usage and machine-readable output.
- `docs/walkthrough-rendering.md`: walkthrough rendering behavior.
- `docs/examples/`: example manifests for common PR shapes.
- `skills/pr-manifest-skill/SKILL.md`: instructions for coding agents that produce manifests.
- `tools/change_manifest_verify.py`: portable first verifier.
- `tools/change_manifest_walkthrough.py`: Markdown walkthrough renderer.
- `plugin/pycharm/`: first PyCharm plugin scaffold.
- `.change-manifest/manifest.json`: manifest for this repository change.

## Validate A Manifest

Use the verifier:

```bash
python tools/change_manifest_verify.py --manifest .change-manifest/manifest.json
```

If the repository has no meaningful Git diff for the manifest, skip diff checks:

```bash
python tools/change_manifest_verify.py --manifest .change-manifest/manifest.json --skip-diff
```

For IDEs, CI systems, and GitHub Apps, use JSON output:

```bash
python tools/change_manifest_verify.py --format json
```

## Render A Walkthrough

```bash
python tools/change_manifest_walkthrough.py --manifest .change-manifest/manifest.json
```

Render to a file:

```bash
python tools/change_manifest_walkthrough.py --output .change-manifest/walkthrough.md
```

## PyCharm Plugin

The first plugin scaffold is in:

```text
plugin/pycharm
```

It opens an `aMazePR` tool window and displays `.change-manifest/manifest.json` as a
guided walkthrough. File actions select manifest line ranges in the diff. Choose
**Working tree** for local changes or **Pull request** and a base branch for committed
branch changes.

For prerequisites, fresh-clone setup, build commands, ZIP output, and installation,
see the [plugin build instructions](plugin/pycharm/README.md#build).

With JDK 21, Gradle, Git, and a local PyCharm installation available, run from the
repository root:

```powershell
gradle -p plugin/pycharm check buildPlugin
```

The ZIP is written to
`plugin/pycharm/build/distributions/amazepr-pycharm-plugin-0.1.2.zip`.

## Run Tests

```bash
python -m unittest discover -s tests
```

## Protocol Status

Current protocol version:

```text
0.1
```

This is an early protocol draft. Consumers should expect compatible additions while the
format matures.
