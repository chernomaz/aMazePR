# PyCharm Plugin

The first PyCharm plugin scaffold lives in:

```text
plugin/pycharm
```

It adds an `aMazePR` tool window with a guided walkthrough panel.

## Current Behavior

The **Walkthrough** button loads the manifest JSON and shows it as step cards:

```text
.change-manifest/manifest.json
```

The **Manifest** button loads the raw manifest JSON:

```text
.change-manifest/manifest.json
```

This gives the protocol its first IDE viewer path while the native plugin verifier is
still being built.

The first structured view includes:

- numbered step navigation
- a walkthrough card
- manifest diagnostics
- `WHAT CHANGED`
- `WHY`
- `WORKFLOW EFFECT`
- file reference buttons
- previous and next actions

The explanation card scrolls vertically and wraps text to the panel width. This keeps
long descriptions and file lists reachable while the step navigation stays visible.

**Open step diff** opens all of the step's file references in one read-only diff group.
Every target appears in a persistent left sidebar. Click a target or use **Previous file**
and **Next file** to move through the group. The selected file's diff appears on the right.
An individual walkthrough file button opens the full group at the selected file.
Files load on demand; an error for one file does not remove the other comparisons.

**Working tree** compares `HEAD`
with local files. **Pull request** compares `git merge-base <base> HEAD` with committed
`HEAD`, excluding local edits. Enter the target reference in **Base branch**, for example
`origin/main`. Fetch it before use; the plugin uses local Git data and does not contact GitHub.
The project remembers the selected mode and the last base used.

A workflow reference with `line` and optional `end_line` selects the inclusive range in
the side-by-side diff. Folded target lines are revealed. Targets outside the file are
clamped to valid lines. Deleted files focus the old side; other files focus the new side.
Git-detected renames compare the old path with the new path. No source editor takes
focus after opening the diff. Git commands run in the background and report failures.

Line targets refer to the new side of the selected comparison, or the old side for a
deleted file. In PR mode the manifest must use committed line numbers.

## Build

The plugin uses the IntelliJ Platform Gradle Plugin. JetBrains documents this Gradle
plugin as the supported way to build, run, test, and publish IntelliJ Platform plugins.

This repository has been verified with local tools under `.tools` and the installed
PyCharm Community Edition 2024.3.

Default local IDE path:

```text
C:/Program Files/JetBrains/PyCharm Community Edition 2024.3
```

Build:

```powershell
$env:JAVA_HOME = (Resolve-Path '..\..\.tools\jdk-21').Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
..\..\.tools\gradle-9.8.1\bin\gradle.bat build
..\..\.tools\gradle-9.8.1\bin\gradle.bat buildPlugin
```

Installable artifact:

```text
plugin/pycharm/build/distributions/amazepr-pycharm-plugin-0.1.2.zip
```

## Next Steps

- Bring native manifest verification to parity with the Python verifier.
- Verify selection, folding, and focus in a live PyCharm session and add IDE UI tests.
