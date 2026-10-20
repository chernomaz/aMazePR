# Change Manifest Protocol

## Purpose

The change manifest protocol is an open format for code changes made by coding agents.
An agent writes a manifest after it changes code. A tool can then verify the manifest
against the Git diff and render it as a review walkthrough.

The first viewer and verifier is the PyCharm plugin. The protocol is not specific to
PyCharm. Codex, Claude, CI systems, GitHub Apps, and other IDEs can produce or consume
the same format.

## Design Goals

- Keep the format stable and easy to parse.
- Make the review intent clear.
- Make the review workflow explicit.
- Separate human intent from verifiable facts.
- Let tools compare the manifest with the real Git diff.
- Keep the format useful for many producers and consumers.

## Manifest Location

The default manifest path is:

```text
.change-manifest/manifest.json
```

Tools may support another path, but this path is the default for interoperability.

## Protocol Version

The first protocol version is:

```text
0.1
```

Consumers must reject manifests with unsupported major versions. During the `0.x`
phase, consumers should show a warning when the exact version is not known.

## Required Sections

The manifest has these required top-level fields:

- `protocol_version`
- `producer`
- `change`
- `workflow`
- `files`
- `verification`
- `risks`
- `agent_notes`

## Claims And Facts

The manifest contains two kinds of information.

A claim explains intent or meaning. Example:

```text
The change makes manifest validation stricter.
```

A fact can be checked against the repository or command output. Example:

```text
docs/manifest.schema.json adds the required protocol_version field.
```

Consumers should verify facts when possible. Consumers should show claims as agent
explanations, not as proven data.

## Field Reference

### `producer`

Identifies the tool that produced the manifest.

```json
{
  "name": "codex",
  "version": "unknown"
}
```

`name` is required. `version` is optional.

### `change`

Describes the change as a whole.

```json
{
  "title": "Add change manifest protocol draft",
  "intent": "Define a structured manifest that coding agents can write after code changes.",
  "scope": "Documentation, schema, examples, and the first agent skill.",
  "non_goals": [
    "No PyCharm UI implementation in this change."
  ]
}
```

### `workflow`

Gives the reviewer an ordered path through the PR.

Each step must have:

- `step`
- `title`
- `description`
- `files`

The `files` array should reference paths from the `files` section, unless the step is a
repository-level or process-level step.

Each item in `files` can be a string path:

```json
"docs/manifest.schema.json"
```

Or an object with a path and optional line target:

```json
{
  "path": "docs/manifest.schema.json",
  "line": 42,
  "end_line": 48,
  "label": "Schema required fields"
}
```

Consumers should use line targets to focus the IDE diff when possible. Producers should
only include line targets when they come from the final diff or from direct file
inspection.

### `files`

Lists files that matter to the change.

Each item must have:

- `path`
- `role`
- `reason`
- `review_notes`

Allowed `role` values:

- `added`
- `modified`
- `deleted`
- `moved`

### `verification`

Describes checks that were run or should be run.

```json
{
  "commands": [
    {
      "command": "npm test",
      "status": "not_run",
      "evidence": "Command runner unavailable in this environment."
    }
  ],
  "manual_checks": [],
  "known_gaps": []
}
```

Allowed command `status` values:

- `passed`
- `failed`
- `not_run`
- `unknown`

Agents must not mark a command as `passed` unless they ran it successfully.

### `risks`

Lists review risks and mitigations.

Each risk must have:

- `area`
- `risk`
- `mitigation`

### `agent_notes`

Records assumptions and limitations.

```json
{
  "assumptions": [],
  "limitations": []
}
```

## Consumer Behavior

A consumer should:

1. Load `.change-manifest/manifest.json`.
2. Validate it against `docs/manifest.schema.json`.
3. Compare the manifest file list with the Git diff.
4. Verify workflow file references.
5. Render the walkthrough.
6. Show warnings for missing, stale, or unverified data.

## Producer Behavior

A producer should:

1. Inspect the final diff.
2. Write the manifest after code changes are complete.
3. Include all meaningful changed files.
4. Explain why each file changed.
5. Add a review workflow in a useful order.
6. State test commands and their real status.
7. State risks and known gaps.
8. Avoid invented facts.

## Compatibility

Consumers should ignore unknown fields by default. This lets producers add extra
metadata without breaking older viewers.

Required fields must remain stable within a protocol version.
