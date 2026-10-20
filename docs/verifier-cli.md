# Verifier CLI

The first verifier is a portable Python script:

```text
tools/change_manifest_verify.py
```

It uses only the Python standard library.

## Basic Use

```bash
python tools/change_manifest_verify.py --manifest .change-manifest/manifest.json
```

The verifier checks manifest structure and, by default, compares manifest files with
the current Git change set. The change set includes:

- unstaged files
- staged files
- untracked files

Use `--skip-diff` when a manifest is checked outside a Git change.

```bash
python tools/change_manifest_verify.py --manifest docs/examples/feature.json --skip-diff
```

## JSON Output

Use JSON output for tools such as IDE plugins, GitHub Apps, and CI processors.

```bash
python tools/change_manifest_verify.py --format json
```

Output shape:

```json
{
  "ok": true,
  "diagnostics": []
}
```

Diagnostics have this shape:

```json
{
  "code": "workflow.file_not_listed",
  "level": "error",
  "message": "Workflow references file not listed in files: src/missing.txt",
  "path": "$.workflow[0].files[0]"
}
```

## Exit Codes

The verifier exits with `1` when any error exists.

Warnings do not fail by default. Use `--warnings-as-errors` for stricter CI gates.

```bash
python tools/change_manifest_verify.py --warnings-as-errors
```
