# aMazePR PyCharm Plugin

This is the first PyCharm plugin scaffold for the change-manifest protocol.

The plugin adds an `aMazePR` tool window with a guided PR walkthrough. The structured
viewer reads the manifest from:

```text
.change-manifest/manifest.json
```

The generated Markdown walkthrough is still supported by the CLI and docs:

```text
.change-manifest/walkthrough.md
```

Use **Walkthrough** for the guided step cards. Use **Manifest** to inspect the raw
manifest JSON.

The step card has a vertical scrollbar for long explanations and file lists. Text
wraps to the panel width, and the Previous/Open step diff/Next controls stay visible.

Click **Open step diff** to open every file reference in the current step as one diff
group. The diff window lists every target in a persistent left sidebar. Click a target
or use **Previous file** / **Next file** to review the group in manifest order.
Clicking an individual walkthrough file button opens the same group at that file.
The sidebar shows the current file number and total. Files load on demand;
a file error appears in its own comparison and leaves the other files available.

Choose a mode in the **Compare** field:

- **Working tree** compares `HEAD` with local files, including uncommitted changes.
- **Pull request** compares the merge base of the **Base branch** and `HEAD` with
  committed `HEAD`. This shows changes made on the current branch and excludes local edits.

Set **Base branch** to a local Git reference such as `origin/main`. Fetch that branch
first if necessary. The plugin remembers the mode and the base used for each project.
It does not fetch branches or look up PRs on GitHub. Invalid bases and unrelated
histories show an error instead of falling back to an empty comparison.

When a workflow reference has `line` and optional `end_line`, the side-by-side diff
reveals and selects that inclusive range. Out-of-range targets are clamped to the
file. Deleted files use the left side. Other files use the right side. The diff keeps
focus; opening a target does not open a separate source editor. Comparisons support
added, empty, deleted, and Git-detected renamed files. Both sides are read-only.
In PR mode, line targets must refer to committed `HEAD` (or the merge base for deleted files).

The plugin parser shows diagnostics for missing fields, duplicate file entries, and
workflow file references that are not listed in the manifest file list.

The Python verifier and renderer remain the reference implementation while the plugin
viewer matures.

## Build

The plugin builds against a local PyCharm installation.

Default local IDE path:

```text
C:/Program Files/JetBrains/PyCharm Community Edition 2024.3
```

Build with the local Gradle and JDK installed in the repository workspace:

```powershell
$env:JAVA_HOME = (Resolve-Path '..\..\.tools\jdk-21').Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
..\..\.tools\gradle-9.8.1\bin\gradle.bat build
..\..\.tools\gradle-9.8.1\bin\gradle.bat buildPlugin
```

The installable plugin artifact is produced at:

```text
plugin/pycharm/build/distributions/amazepr-pycharm-plugin-0.1.1.zip
```

To use another local IDE path:

```powershell
..\..\.tools\gradle-9.8.1\bin\gradle.bat build -PlocalIdePath="C:/Path/To/PyCharm"
```

## Next Plugin Steps

- Bring native manifest validation to parity with the Python verifier.
- Add IDE UI tests for line selection and focus.

## Regression Checks

Run `gradle check` with the local tools above. It includes `diffNavigationTest`, which
uses temporary Git repositories to check PR merge-base semantics, local edits,
file lifecycle changes, large output, invalid bases, and line range bounds.
