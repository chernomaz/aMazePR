# Walkthrough Rendering

The walkthrough renderer is the first concrete consumer of the change manifest.

It proves the basic loop:

1. An agent writes `.change-manifest/manifest.json`.
2. The verifier checks structure and changed files.
3. The renderer turns the manifest into a review walkthrough.

The PyCharm plugin can use the same sections:

- diagnostics
- intent
- scope
- non-goals
- review workflow
- files
- verification
- risks
- assumptions
- limitations

## CLI

Render to standard output:

```bash
python tools/change_manifest_walkthrough.py --manifest .change-manifest/manifest.json
```

Render to a file:

```bash
python tools/change_manifest_walkthrough.py --output .change-manifest/walkthrough.md
```

Use `--skip-diff` when rendering an example manifest or a manifest outside a Git change.

```bash
python tools/change_manifest_walkthrough.py --manifest docs/examples/feature.json --skip-diff
```

## Error Behavior

The renderer shows diagnostics near the top of the walkthrough.

If the manifest has errors, the command exits with `1`. This lets CI or an IDE know
that the walkthrough is not trusted.
