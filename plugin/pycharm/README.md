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

Run the following commands from the **repository root**, which contains `README.md`,
`docs`, and `plugin`.

### Prerequisites

- JDK 21.
- Gradle installed separately. This project was built with Gradle 9.8.1.
- A local PyCharm installation. The tested target is PyCharm Community Edition 2024.3.
- Git available on `PATH`, because the regression checks create temporary repositories.
- Internet access on the first build to download Gradle plugin and Maven dependencies.

There is no Gradle wrapper or standalone build script in this repository. The build
configuration is `plugin/pycharm/build.gradle.kts`. The `.tools` directory is ignored
by Git and is not supplied by a fresh clone.

The default IDE installation path in the build configuration is:

```text
C:/Program Files/JetBrains/PyCharm Community Edition 2024.3
```

### Build From A Fresh Clone

Install the prerequisites, then open PowerShell:

```powershell
git clone https://github.com/chernomaz/aMazePR.git
cd aMazePR
```

Set `JAVA_HOME` to your JDK 21 installation. Replace the example path below with your
actual JDK directory. The Gradle executable must also be available on `PATH`.

```powershell
$env:JAVA_HOME = 'C:\Path\To\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
gradle --version
gradle -p plugin/pycharm check buildPlugin
```

`check` runs the standalone regression checks, including file sidebar navigation,
Git comparisons, and line ranges. `buildPlugin` creates the installable ZIP.

If PyCharm is installed somewhere else, pass its installation directory:

```powershell
gradle -p plugin/pycharm check buildPlugin '-PlocalIdePath=C:/Path/To/PyCharm'
```

### Build In The Existing Workspace

On the original development machine, the JDK and Gradle already exist under `.tools`.
From the repository root, run:

```powershell
cd D:\ai\amazePR\repo
$env:JAVA_HOME = (Resolve-Path '.tools\jdk-21').Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\.tools\gradle-9.8.1\bin\gradle.bat -p plugin/pycharm check buildPlugin
```

### Output And Installation

The output, relative to the repository root, is:

```text
plugin/pycharm/build/distributions/amazepr-pycharm-plugin-0.1.2.zip
```

The versioned plugin JAR is also available at:

```text
plugin/pycharm/build/libs/amazepr-pycharm-plugin-0.1.2.jar
```

Use the ZIP for **Install Plugin from Disk**, since it contains the packaged plugin.

For a clean rebuild, add `clean` before the other tasks:

```powershell
gradle -p plugin/pycharm clean check buildPlugin
```

In PyCharm, open **Settings > Plugins**, open the gear menu, and select
**Install Plugin from Disk**. Choose the ZIP and restart PyCharm. For this version,
the installed plugin should show **0.1.2**. Reloading a manifest does not update plugin code.

### Common Build Problems

- **`gradle` is not recognized:** add the installed Gradle `bin` directory to `PATH`,
  or invoke its `gradle.bat` by full path.
- **JDK 21 is not found:** check `JAVA_HOME` and `java -version` in the same terminal.
- **The local IDE directory is missing:** supply `-PlocalIdePath` as shown above.
- **The old plugin still runs:** install the newly built ZIP and restart the IDE;
  check the installed plugin version.

## Next Plugin Steps

- Bring native manifest validation to parity with the Python verifier.
- Add IDE UI tests for line selection and focus.

## Regression Checks

Run `gradle check` with the local tools above. It includes `diffNavigationTest`, which
uses temporary Git repositories to check PR merge-base semantics, local edits,
file lifecycle changes, large output, invalid bases, and line range bounds.
