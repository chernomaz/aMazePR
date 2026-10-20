---
name: pr-manifest-skill
description: Write or refresh a structured change manifest after code changes, with an evidence-based review walkthrough for IDE and CI consumers.
---

# PR Manifest Skill

Use this skill when you are a coding agent that has changed code and must write a
structured change manifest for review tools.

The output is the input for manifest consumers, such as the PyCharm plugin, CI
verifiers, GitHub Apps, and other IDEs.

## Output Path

Write the manifest to:

```text
.change-manifest/manifest.json
```

## Required Protocol

Use protocol version:

```text
0.1
```

Follow the schema in:

```text
docs/manifest.schema.json
```

## Procedure

1. Inspect the final Git diff, staged changes, and relevant untracked files. If a PR
   base is supplied, use it to establish the change scope. Distinguish a new feature
   from a fix or refactor; do not invent a previous implementation for a new project.
2. Identify the intent of the change.
3. Identify the real scope of the change.
4. List important changed files.
5. Explain why each file changed.
6. Create a reviewer workflow that explains behavior and connects the relevant files.
7. Record verification commands and their real status.
8. Record risks and mitigations.
9. Record assumptions and limitations.
10. Write valid JSON, run the repository verifier, and regenerate the walkthrough if
    this repository provides a renderer. Record the results you actually observed.

## Strict Rules

- Do not invent files.
- Do not invent tests.
- Do not mark a command as `passed` unless you ran it successfully.
- Do not hide failed or skipped checks.
- Do not claim the plugin can verify intent.
- Do not use long prose where a short explanation is enough.
- Do not include secrets, credentials, or private environment data.
- Do not include unrelated changed files unless they affect review.
- Write the final state of the whole change. Replace stale descriptions instead of
  appending a history of implementation attempts.

## Explanation Quality

The agent writes the explanation in the manifest. Renderers format it; they cannot
recover missing design intent. Put essential detail in the existing `description`,
`reason`, and `review_notes` fields so current consumers can show it.

For each workflow step, explain the changed behavior, why it is needed, how the
referenced files cooperate, and what a reviewer should verify. Use the concrete
trigger and outcome when they clarify the change. Name the relevant function,
component, or protocol field rather than repeating the filenames. A short paragraph
can be enough for a small change; use more detail for a substantial decision or risk.

Group files by a behavior or review decision. Split a step when it combines unrelated
decisions. Supporting types can share a step with their caller. Keep all meaningful
files in the inventory even when they do not need a separate step.

Weak description:

> Review the diff opener and tests.

Useful description for a hypothetical multi-file diff fix:

> A step can reference several files, but the action selected only the first one.
> The view now passes the complete ordered list and selected index to the diff
> opener, which creates one navigable comparison group. A file button starts at
> that file while retaining the rest of the group. Verify that a three-file step
> exposes all three files in order and that selecting the last file starts there.

Do not copy this example as a claim about another change. Derive the explanation
from the code you inspected.

Use `files[].reason` for the file's responsibility and why it belongs in this change.
Use `review_notes` for specific invariants, compatibility decisions, failure cases,
or remaining questions. An empty notes array is appropriate for a simple support
file; filler such as "review carefully" adds no guidance.

Prefer a verified function or section range over `line: 1` on every file. For example,
point at the call that passes the full file list and the method that builds the diff
group. Read the final file to obtain those lines. Omit a line target when you cannot
verify the relevant side of the comparison; deleted-file targets refer to old content.

Record limits precisely. A passing build does not prove live UI behavior. Structure
validation does not prove the agent's explanation is correct. Label fictional sample
paths and sample test results as examples, not evidence from the current repository.

## Required JSON Shape

```json
{
  "protocol_version": "0.1",
  "producer": {
    "name": "codex",
    "version": "unknown"
  },
  "change": {
    "title": "",
    "intent": "",
    "scope": "",
    "non_goals": []
  },
  "workflow": [
    {
      "step": 1,
      "title": "",
      "description": "",
      "files": []
    }
  ],
  "files": [
    {
      "path": "",
      "role": "modified",
      "reason": "",
      "review_notes": []
    }
  ],
  "verification": {
    "commands": [
      {
        "command": "",
        "status": "not_run",
        "evidence": ""
      }
    ],
    "manual_checks": [],
    "known_gaps": []
  },
  "risks": [
    {
      "area": "",
      "risk": "",
      "mitigation": ""
    }
  ],
  "agent_notes": {
    "assumptions": [],
    "limitations": []
  }
}
```

## File Roles

Use one of these values:

- `added`
- `modified`
- `deleted`
- `moved`

## Verification Status

Use one of these values:

- `passed`
- `failed`
- `not_run`
- `unknown`

If the command did not run because the environment was unavailable, use `not_run` and
explain why in `evidence`.

## Review Workflow

The workflow must help a reviewer follow the change from input through processing
to its visible result, then assess the evidence and remaining risks.

Choose the order that fits the change. A common order is:

- Start with protocol or model changes.
- Then show implementation.
- Then show tests.
- Then show docs or examples.

Each file reference must be listed in the `files` inventory for this verifier.

Workflow files can be string paths:

```json
"src/main.py"
```

Or objects with line targets:

```json
{
  "path": "src/main.py",
  "line": 42,
  "end_line": 45,
  "label": "Validation branch"
}
```

Only include line targets when they are based on the final diff or direct file
inspection.

## Claims And Facts

Separate intent from facts.

Intent belongs in:

- `change.intent`
- `change.scope`
- `files[].reason`
- `risks[]`

Facts belong in:

- file paths
- file roles
- command status
- command evidence

Only include facts that you can support from the diff or from commands you ran.

## Final Check

Before you finish:

1. Confirm that the JSON parses.
2. Confirm that every workflow file is listed in `files`.
3. Confirm that every important changed file is listed in `files`.
4. Confirm that command statuses are truthful.
5. Confirm that risks are specific enough for review.
6. Read each step without its file list. It must still explain a concrete behavior
   and a useful review check. Rewrite descriptions that could fit an unrelated PR.
7. Check the causal path against the source: the named caller, transformation, and
   result must agree. Separate observed behavior from intended behavior and gaps.
8. Confirm line targets match the final files and the intended comparison side.
9. Read the rendered workflow, when available, and confirm the substantive explanation
   survived rendering. Keep the manifest as the source; do not enrich only generated Markdown.
