# Verifier Rules

This document defines the first verifier rules for the change manifest protocol.

## Rule Format

Each rule should return a diagnostic.

Diagnostic fields:

- `code`
- `level`
- `message`
- `path`
- `details`

The current CLI emits `code`, `level`, `message`, and `path`. A future version may add
`details`.

`level` must be one of:

- `error`
- `warning`
- `info`

## Rules

### `manifest.missing`

Level: `error`

The default manifest file does not exist.

Default path:

```text
.change-manifest/manifest.json
```

### `manifest.invalid_json`

Level: `error`

The manifest is not valid JSON.

### `manifest.unsupported_protocol`

Level: `error`

The manifest uses an unsupported protocol version.

### `manifest.missing_required_field`

Level: `error`

A required field is missing.

### `manifest.duplicate_file`

Level: `error`

The `files` array contains the same path more than once.

### `workflow.file_not_listed`

Level: `error`

A workflow step references a file that is not listed in `files`.

Workflow file references may be string paths or objects with a `path` field and optional
line target.

### `workflow.invalid_file_reference`

Level: `error`

A workflow file reference is neither a string path nor an object with a non-empty `path`
field.

### `diff.changed_file_not_documented`

Level: `warning`

A changed file is not listed in the manifest.

This is a warning in protocol version `0.1` because some generated or incidental files
may not need review notes. A future policy can make this stricter.

### `diff.manifest_file_not_changed`

Level: `warning`

A file listed in the manifest is not present in the Git diff.

This can be valid when a step references a repository-level file, but the producer
should explain the reason.

### `verification.passed_without_evidence`

Level: `warning`

A command is marked `passed` but has no evidence text.

### `verification.empty_command`

Level: `error`

A verification command is empty.

### `diff.unavailable`

Level: `warning`

The verifier could not read the Git diff. This can happen outside a Git repository or
when Git is not installed.

## Rendering Behavior

The viewer should render manifests with warnings. It should show the warning near the
related section.

The viewer should not render a trusted walkthrough when the manifest has errors. It can
still show a recovery panel with diagnostics.
