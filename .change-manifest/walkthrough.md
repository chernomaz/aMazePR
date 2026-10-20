# Bootstrap open change-manifest protocol and PyCharm viewer

## Intent

Let the coding agent that made a change explain its intent and review path once, in a portable JSON manifest. A verifier checks structural facts and file coverage; PyCharm and Markdown consumers present that explanation with links back to the actual code.

## Scope

Initial protocol 0.1, schema, reusable producer skill, four illustrative manifests, Python verifier and Markdown renderer, Python CI checks, and a local-build PyCharm viewer. The viewer reads JSON, groups every step file into a navigable diff chain, supports local and merge-base PR comparisons, and selects inclusive manifest line ranges. This manifest describes the complete uncommitted repository foundation, including the later diff-navigation fixes.

## Non-Goals

- No remote PR discovery, automatic fetch, or GitHub App implementation.
- No full JSON Schema engine or shared Python/Java validation library.
- No claim that validation proves intent, explanation quality, or runtime correctness.
- No marketplace release, portable IDE download setup, or automated live UI tests.

## Review Workflow

### 1. Establish what the manifest promises

This repository introduces the producer-to-consumer contract: the agent writes .change-manifest/manifest.json after changing code, and consumers use that record to guide review. The schema fixes the required sections and file-role vocabulary. Workflow references can be plain paths or objects with a path, one-based line, optional inclusive end_line, and label. This allows an IDE to open the relevant code without asking another agent to rediscover the review path.

Compare the documented contract with the schema. Confirm every workflow path can be resolved through the file inventory, and that unknown fields remain allowed. Review intent as an agent explanation, not as a verified fact. Protocol version 0.1 is an initial contract; the Python verifier currently accepts exactly 0.1 even though the prose discusses broader version compatibility.

Files:
- `docs/change-manifest-protocol.md:56` - Producer and consumer contract
- `docs/manifest.schema.json:7` - Required top-level sections
- `docs/manifest.schema.json:68` - Workflow path and inclusive range fields

### 2. Make the producing agent supply the missing explanation

The skill is the source of authoring guidance for Codex or Claude. A renderer cannot infer why a change was made from a filename list, so the updated guidance puts substantive explanations into workflow descriptions, file reasons, and review notes. Each step must describe changed behavior, motivation, the relationship between files, and a concrete review check. A short before/after example shows why "review these files" is insufficient.

Verify that the guidance demands evidence rather than word count: code targets come from inspected source, command results come from observed runs, and hypothetical samples are kept separate from repository facts. The final quality check asks whether a step still explains a useful behavior without its file list. The skill remains in this repository; this change does not install it globally.

Files:
- `skills/pr-manifest-skill/SKILL.md:65` - Quality rules and concrete example
- `skills/pr-manifest-skill/SKILL.md:241` - Final author review before delivery

### 3. Trace structural validation and Git coverage

The Python verifier loads JSON, validates required fields and supported values, rejects duplicate file paths and step numbers, and checks each workflow path against the inventory. Without --base it combines unstaged, staged, and non-ignored untracked paths, which matters here because most of this foundation is still untracked. validate_diff reports missing or extra inventory paths as warnings; --warnings-as-errors makes those warnings fail the command. JSON output exposes the same diagnostics for tool consumers.

Review the boundary between these checks and a full schema engine. The implementation does not enforce every JSON Schema constraint or the correctness of prose and target line ranges. Also inspect --base: it compares the supplied revision directly with the working tree; it does not use the plugin PR mode's merge-base-to-HEAD semantics. The tests cover listed and unlisted workflow references, duplicates, protocol rejection, diff coverage, and diagnostic serialization.

Files:
- `tools/change_manifest_verify.py:168-375` - Structural rules
- `tools/change_manifest_verify.py:412-438` - Collect staged, unstaged, and untracked changes
- `docs/verifier-rules.md:26` - Diagnostic severity and contract
- `docs/verifier-cli.md` - CLI options and JSON diagnostics
- `tests/test_change_manifest_verify.py:79` - Positive and negative rule checks

### 4. Follow the explanation into generated Markdown

The renderer calls the verifier, then copies the manifest's intent, scope, workflow descriptions, file reasons, verification evidence, and risks into Markdown sections. render_workflow preserves each file label and line range. It does not call an AI model or invent a deeper explanation; a thin manifest produces a thin walkthrough. Richer descriptions therefore belong in JSON and flow into both this generated file and the plugin.

Inspect render_workflow and render_workflow_file_reference to confirm the review narrative and inclusive ranges survive formatting. Diagnostics appear near the top, and errors make the CLI return a failure. The tests cover section output, diagnostic placement, and labeled line ranges. Regenerate the walkthrough after editing the manifest rather than maintaining a separate explanation in Markdown.

Files:
- `tools/change_manifest_walkthrough.py:54-98` - Render the authored workflow
- `tools/change_manifest_walkthrough.py:221-238` - Validate before rendering
- `docs/walkthrough-rendering.md` - Rendering and failure behavior
- `tests/test_change_manifest_walkthrough.py:37` - Sections, diagnostics, and line reference coverage

### 5. Separate illustrative manifests from this real change

The four sample files demonstrate different review decisions: an invalid workflow reference for a bug fix, parser extraction for a refactor, panel rendering for a feature, and role-value compatibility for a migration. Their Kotlin paths and Gradle test evidence are illustrative; those implementations are not present in this repository. --skip-diff allows their structure to be checked without treating fictional paths as this Git change.

This PR's manifest is the real producer output, and walkthrough.md is its generated representation. Compare the two to confirm the same explanations, file coverage, and verification limits appear. Sample validity does not establish that sample tests ran or that every explanatory statement is consistent: the bug-fix sample, for example, describes a warning mitigation while its workflow-reference rule is an error. Treat the examples as format illustrations requiring editorial review.

Files:
- `docs/examples/bug-fix.json` - Bug-fix illustration
- `docs/examples/refactor.json` - Refactor illustration
- `docs/examples/feature.json` - Feature illustration
- `docs/examples/risky-migration.json` - Compatibility illustration
- `.change-manifest/manifest.json` - Actual repository change record
- `.change-manifest/walkthrough.md` - Generated representation of this record

### 6. Load JSON and turn it into walkthrough data

The active tool-window view calls loadManifestOnly and passes the JSON to ManifestJsonParser. The parser extracts the change title and intent, workflow steps, file references, and a limited set of diagnostics. ParsedWalkthrough and WalkthroughStep carry that data to the view; WalkthroughContent represents loaded, missing, and read-error states. The loader and Markdown parser also retain older Markdown paths, but the current Walkthrough action reads JSON directly.

Trace a workflow reference from JSON into WorkflowFileReference and then into a step. Check missing content and duplicate or unlisted-path diagnostics. Do not treat an empty plugin diagnostic list as full protocol validation: the Java parser does not implement all Python rules, such as the supported-version check. The old Markdown parser also does not fully handle labeled range references emitted by the current renderer.

Files:
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughLoader.java:40` - Active JSON-only loading path
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestJsonParser.java:101` - Parse file targets and labels
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ParsedWalkthrough.java` - Parsed result for the view
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/WalkthroughStep.java` - Step data and ordered references
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/WalkthroughContent.java` - Loaded, missing, and error content states
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/WalkthroughMarkdownParser.java:56` - Legacy Markdown parsing path
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestPaths.java` - Shared manifest and walkthrough paths

### 7. Keep every step file available in the diff viewer

A workflow step can contain several file targets. The earlier Open diff action selected only its first file, so the other references were absent from that comparison. The view now passes the complete ordered list and a selected index to ManifestDiffOpener. Open step diff starts at index zero; clicking a particular file retains the full group and starts at that file. createChain makes one lazy DiffRequestProducer per reference, including separate targets in the same file.

Follow the view's file actions into createChain. Confirm the chain length equals the step reference count, file order is preserved, and a click on the last reference selects the last comparison. StepDiffDialog embeds the native diff processor alongside StepDiffPanel, which permanently lists every target. Clicking the list or using Previous file and Next file calls setCurrentRequest with the selected index. Processor navigation synchronizes the list without recursively loading again. Each producer carries its own range hints and reports its own loading error. The sidebar exposes the group size and selected position, independent of which native toolbar actions are visible. It shows one selected file diff on the right and keeps all other targets directly selectable on the left.

Long explanations previously exceeded the fixed card height and were clipped. The card now sits in a vertical scroll pane; its Scrollable implementation tracks viewport width so text wraps and the card can grow in height. The navigation controls remain outside the scrolling area. Verify that a long step can be scrolled through its final paragraph and file actions at a narrow window width, and that switching steps returns the card to the top.

Inspection of the locally installed 0.1.0 JAR found no diff-chain implementation, explaining why that copy still opened only one file. The packaged version is now 0.1.1. Install the new ZIP and restart PyCharm; reloading the manifest cannot update plugin classes.

Files:
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughView.java:256-301` - File buttons and full-step action
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughView.java:350-376` - Pass all references and selected index
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestDiffOpener.java:36` - Create one producer per step target
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/WorkflowFileReference.java` - Keep ranges and labels with each target
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughView.java:167-204` - Scrollable card and fixed navigation
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/StepDiffPanel.java:20` - StepDiffPanel
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/StepDiffDialog.java:12` - StepDiffDialog

### 8. Choose the correct content for local and PR review

GitDiffComparison resolves HEAD to a commit before reading a file. Working tree mode uses that commit on the left and the local file on the right, so it includes local modifications and untracked files. Pull request mode resolves the entered base reference, finds its merge base with HEAD, and compares that ancestor with committed HEAD. If the target branch advanced independently, those target-only changes are excluded; local edits are excluded as well.

Review the rename pairing and the distinction between an empty file and a missing side. Added files get an empty old side, deleted files get an empty new side, and Git-detected renames retain old-path content. Missing bases, unrelated histories, unsupported file kinds, read failures, and timeouts produce errors. The plugin uses local references and does not fetch them. Lazy producers resolve commits separately, so keep the checkout stable while moving through a group; a group-wide snapshot is not pinned.

Files:
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/GitDiffComparison.java:13-61` - Resolve snapshots and pair renamed paths
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/GitDiffComparison.java:66` - Distinguish absent content and Git failures
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughView.java:73-144` - Persist comparison mode and base

### 9. Reveal and select the intended code range

The opener adds aMazePR-specific target data to a read-only side-by-side diff request. ManifestDiffRangeExtension handles only requests with that key. After diff calculation, it converts the one-based inclusive range to valid editor lines, expands folds that intersect the range, selects its text, and scrolls the caret into view. Deleted files target the old side; other files target the new side. Opening the target no longer navigates a separate source editor and takes focus away from the diff.

Check the extension registration, side choice, and end-line conversion together. DiffLineRange clamps targets past the file end and prevents a reversed range from selecting backwards. These bounds checks do not detect a stale manifest that points to valid but unrelated lines. The extension runs its initial selection once per viewer; live folding, selection visibility, and focus still need a PyCharm check.

Files:
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestDiffRangeExtension.java:19` - Apply the range after rediff
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/DiffLineRange.java:6` - Inclusive range conversion and clamping
- `plugin/pycharm/src/main/resources/META-INF/plugin.xml:12` - Register tool window and range extension

### 10. Check what the automated plugin tests establish

The standalone diffNavigationTest task runs dependency-free test logic with the IDE compile classpath. It reads this actual manifest and builds native diff chains for every non-empty step, asserting full reference counts, order, and selected indices. It also checks that repeated targets in one file remain separate. Temporary Git repositories exercise an advanced target branch, exclusion of local edits from PR mode, local changes, additions, deletions, renames, empty files, large output, invalid references, and unrelated history.

Read the assertions alongside the implementation rather than relying on a build label. The source set is separate from framework tests so Gradle check runs the standalone checks without a false test-discovery failure. These tests do not instantiate the live tool window, verify toolbar appearance, or prove visual selection and focus. The local IDE and toolchain requirements remain in the build and plugin README.

Sidebar tests construct the real StepDiffPanel on the Swing event dispatch thread for every manifest step. They assert full list membership, initial selection, clicks and Previous/Next callbacks, disabled boundary buttons, and no callback recursion during processor sync. They do not instantiate the live IDE diff processor or prove installed-plugin behavior.

Files:
- `plugin/pycharm/src/diffTest/java/dev/amazepr/plugin/DiffNavigationTest.java:105-132` - Assert complete native chains for every manifest step
- `plugin/pycharm/src/diffTest/java/dev/amazepr/plugin/DiffNavigationTest.java:16-99` - Git scenarios and range checks
- `plugin/pycharm/build.gradle.kts:32` - Wire standalone checks into Gradle check
- `plugin/pycharm/README.md` - Usage, build prerequisites, and current gaps

### 11. Reproduce the loop and identify delivery limits

The README and full-loop guide connect authoring, strict verification, Markdown generation, and IDE review. The GitHub Actions workflow runs Python tests, verifies manifest structure with --skip-diff, renders the walkthrough, and checks the examples. It does not enforce PR diff coverage there and does not build or run the Java plugin. The local Gradle setup uses the installed PyCharm 2024.3 and workspace JDK/Gradle; the tool-window factory attaches the viewer to the IDE.

Check the commands against the actual build paths and the recorded evidence below. The implementation plan contains older scaffold descriptions and is not an accurate completion checklist. Local tools and IDE caches are ignored. No commit or PR publication is part of this manifest refresh; install the packaged plugin separately to assess the remaining UI checks.

Files:
- `README.md` - Project entry point and commands
- `docs/full-loop.md` - Author-to-consumer loop
- `.github/workflows/verify.yml` - Actual Python-only CI checks
- `docs/pycharm-plugin.md` - Plugin modes and current limits
- `docs/implementation-plan.md` - Roadmap with older scaffold descriptions
- `plugin/pycharm/settings.gradle.kts` - Standalone plugin project
- `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughToolWindowFactory.java` - Create the IDE view
- `.gitignore` - Keep local tools and caches out of source

## Files

### `.gitignore`

Role: `modified`

Excludes local build tools, IDE state, and generated caches so the initial source change contains reviewable inputs.

### `README.md`

Role: `added`

Connects protocol authoring, verification, rendering, and plugin use so a reviewer can reproduce the local loop.

### `docs/change-manifest-protocol.md`

Role: `added`

Defines the common JSON contract and separates agent intent from facts that tools can verify.

Review notes:
- Compare version guidance with the exact 0.1 acceptance implemented by the verifier.

### `docs/manifest.schema.json`

Role: `added`

Defines required sections, file roles, and optional line-range references for interoperable producers.

Review notes:
- The Python verifier implements a subset of these constraints; schema validity and narrative quality are separate checks.

### `docs/verifier-rules.md`

Role: `added`

Names diagnostics and their severity so CLI and IDE consumers can interpret failures consistently.

### `docs/verifier-cli.md`

Role: `added`

Documents command options and structured diagnostics for local and CI use.

Review notes:
- The CLI --base comparison includes the working tree; it differs from plugin PR mode.

### `docs/walkthrough-rendering.md`

Role: `added`

Explains how authored JSON becomes Markdown and how diagnostics affect renderer output.

### `docs/full-loop.md`

Role: `added`

Gives commands to reproduce the producer, verifier, renderer, and IDE consumer sequence.

### `docs/implementation-plan.md`

Role: `added`

Records the original phases for protocol and consumer work.

Review notes:
- Some scaffold and status notes predate the current implementation; use source and verification evidence for current behavior.

### `docs/pycharm-plugin.md`

Role: `added`

Documents multi-file navigation, local versus PR comparisons, range targets, and the remaining UI checks.

### `docs/examples/bug-fix.json`

Role: `added`

Illustrates a bug-fix review workflow and the protocol shape without requiring the hypothetical implementation in this repository.

Review notes:
- Sample paths and claimed test outcomes are illustrative, not evidence for this PR.

### `docs/examples/refactor.json`

Role: `added`

Illustrates a refactor review workflow and the protocol shape without requiring the hypothetical implementation in this repository.

Review notes:
- Sample paths and claimed test outcomes are illustrative, not evidence for this PR.

### `docs/examples/feature.json`

Role: `added`

Illustrates a feature review workflow and the protocol shape without requiring the hypothetical implementation in this repository.

Review notes:
- Sample paths and claimed test outcomes are illustrative, not evidence for this PR.

### `docs/examples/risky-migration.json`

Role: `added`

Illustrates a risky-migration review workflow and the protocol shape without requiring the hypothetical implementation in this repository.

Review notes:
- Sample paths and claimed test outcomes are illustrative, not evidence for this PR.

### `skills/pr-manifest-skill/SKILL.md`

Role: `added`

Teaches agents to produce an evidence-based review explanation using the fields current consumers already read.

Review notes:
- Check behavior, motivation, relationships between files, and reviewer checks in every workflow description.
- Keep output quality guidance distinct from claims that automated validation proves prose.

### `tools/change_manifest_verify.py`

Role: `added`

Checks protocol structure and workflow membership, compares the inventory with Git paths, and returns text or JSON diagnostics.

Review notes:
- Without a base, staged, unstaged, and untracked files are combined.
- It is a hand-written rule set, not a full schema engine or an assessment of explanation quality.

### `tools/change_manifest_walkthrough.py`

Role: `added`

Formats the manifest narrative and diagnostics as Markdown without adding inferred explanations.

Review notes:
- Compare generated step descriptions, labels, and line ranges with their JSON source.

### `tests/test_change_manifest_verify.py`

Role: `added`

Checks valid inputs and diagnostic behavior for malformed JSON, protocol versions, duplicate paths, workflow references, and diff coverage.

### `tests/test_change_manifest_walkthrough.py`

Role: `added`

Checks that the renderer preserves main sections, leading diagnostics, and labeled line targets.

Review notes:
- These assertions establish formatting behavior, not the quality of the authored review narrative.

### `.github/workflows/verify.yml`

Role: `added`

Runs Python tests, structural manifest checks, Markdown generation, and illustrative-example verification on pushes and PRs.

Review notes:
- --skip-diff skips file coverage in CI; Java plugin checks are local only.

### `.change-manifest/manifest.json`

Role: `added`

Applies the producer skill to this complete repository foundation with a concrete review path and explicit validation limits.

Review notes:
- Verify source targets and factual claims against the referenced code; successful structural verification is insufficient.

### `.change-manifest/walkthrough.md`

Role: `added`

Publishes the renderer output for this manifest so reviewers can read the same narrative outside PyCharm.

Review notes:
- Regenerate from JSON; do not maintain a separate explanation in this generated file.

### `plugin/pycharm/README.md`

Role: `added`

Explains installation artifact paths, whole-step file navigation, comparison modes, and local regression commands.

### `plugin/pycharm/settings.gradle.kts`

Role: `added`

Names the standalone plugin build so it can run independently of the Python tools.

### `plugin/pycharm/build.gradle.kts`

Role: `added`

Builds against the local IDE and makes the standalone Git, chain, and range checks part of Gradle check.

Review notes:
- The local IDE path and workspace toolchain still limit portability; CI does not execute this build.
- Plugin version is 0.1.1 so the installed package can be distinguished from older 0.1.0 builds.

### `plugin/pycharm/src/main/resources/META-INF/plugin.xml`

Role: `added`

Registers both the tool-window factory and the diff-range extension with the IntelliJ Platform.

Review notes:
- The existing plugin ID produces a product-name warning in structure verification.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestPaths.java`

Role: `added`

Keeps the default manifest and generated Markdown paths consistent across loader calls.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughLoader.java`

Role: `added`

Reads JSON or legacy Markdown and represents missing and read-error states for the view.

Review notes:
- The active walkthrough uses loadManifestOnly; retained Markdown fallback is not the active path.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestJsonParser.java`

Role: `added`

Maps JSON into walkthrough models and reports missing fields, duplicates, and workflow paths absent from the inventory.

Review notes:
- Do not equate its limited diagnostic set with the Python verifier or complete schema validation.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughView.java`

Role: `added`

Renders authored step descriptions, remembers comparison settings, and passes the full step list plus selected target to the diff opener. Keeps long explanation and file content in a width-tracking vertical scroll pane while leaving navigation controls fixed.

Review notes:
- An individual file action must preserve all other files in its step.
- WHY currently repeats the overall change intent; WORKFLOW EFFECT reports target count rather than a dedicated per-step explanation.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestDiffOpener.java`

Role: `added`

Creates the ordered lazy diff chain and opens StepDiffDialog, where every step target is explicitly listed beside the selected diff.

Review notes:
- Check selected index, reference order, repeated targets, and per-producer errors.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestWalkthroughToolWindowFactory.java`

Role: `added`

Attaches the walkthrough component to the registered aMazePR tool window.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/ParsedWalkthrough.java`

Role: `added`

Carries the parsed title, intent, diagnostics, and ordered steps from parser to view.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/WalkthroughContent.java`

Role: `added`

Carries loaded, missing, and error results so loading failures can reach the view.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/WalkthroughMarkdownParser.java`

Role: `added`

Retains the older Markdown-to-step parsing path alongside the active JSON consumer.

Review notes:
- Its file parsing is not fully compatible with the renderer's labeled range syntax; do not rely on it for parity.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/WalkthroughStep.java`

Role: `added`

Keeps step number, explanation, and file targets together when rendering and opening a diff group.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/WorkflowFileReference.java`

Role: `added`

Carries a path, optional inclusive range, and readable label through parser, view, and diff opening.

Review notes:
- Two references to the same path can still represent different code targets.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/GitDiffComparison.java`

Role: `added`

Loads local or merge-base snapshots, pairs detected renames, distinguishes absent content, and surfaces Git/read failures.

Review notes:
- PR content is committed HEAD; local edits appear only in Working tree mode.
- Each lazy file load resolves commits again; the whole group is not frozen to a single checkout snapshot.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/DiffLineRange.java`

Role: `added`

Converts one-based inclusive targets to bounded editor indices for empty, short, or changed files.

Review notes:
- Clamping prevents invalid indices but cannot establish that a target still refers to the intended code.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/ManifestDiffRangeExtension.java`

Role: `added`

Reveals folded target text and selects its range after diff calculation for aMazePR-tagged requests.

Review notes:
- Deleted-file targets use the old side; other targets use the new side.
- Live visual selection and focus remain unverified.

### `plugin/pycharm/src/diffTest/java/dev/amazepr/plugin/DiffNavigationTest.java`

Role: `added`

Checks native chain membership against the actual manifest and tests Git snapshots and line bounds in temporary repositories. Exercises the real Swing sidebar on the event dispatch thread, including every manifest step, clicked target, button boundaries, and callback suppression during selection sync.

Review notes:
- The check count depends on manifest references; these are standalone checks, not live IDE UI tests.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/StepDiffPanel.java`

Role: `added`

Shows all step targets in a persistent sidebar and routes list clicks and Previous/Next file buttons to diff producer indices.

Review notes:
- Programmatic selection sync must not trigger another load. The file list remains present even when a producer shows an error.

### `plugin/pycharm/src/main/java/dev/amazepr/plugin/StepDiffDialog.java`

Role: `added`

Owns the native diff processor beside the explicit file list, synchronizes selection, and disposes the processor when the window closes.

Review notes:
- The sidebar and native toolbar must agree on the selected producer; the initial clicked index must be retained.

## Verification

Commands:
- `passed` `python -m unittest discover -s tests`: Ran 15 Python tests successfully after the scrolling change and manifest update.
- `passed` `python tools/change_manifest_verify.py --manifest .change-manifest/manifest.json --format json --warnings-as-errors`: Strict verification returned ok true with no diagnostics, including coverage of staged, unstaged, and untracked repository changes.
- `passed` `python tools/change_manifest_walkthrough.py --manifest .change-manifest/manifest.json --output .change-manifest/walkthrough.md`: Regenerated Markdown successfully. A direct comparison confirmed all 11 step descriptions survive rendering; all 41 inventory files appear in the review workflow and every source target is in bounds.
- `passed` `.tools/gradle-9.8.1/bin/gradle.bat -p plugin/pycharm check buildPlugin`: Passed 216 standalone checks, including actual Swing sidebar selection and Previous/Next actions for every manifest step. Packaged version 0.1.1; ZIP inspection confirmed both sidebar and dialog classes. This does not exercise live IDE processor integration.

Known gaps:
- The verifier implements protocol rules directly; it does not enforce the entire schema, validate line relevance, or judge explanation quality.
- The Java parser has fewer checks than the Python verifier. The view's verified label refers only to that parser result.
- Live PyCharm file selection, toolbar navigation, folding, and focus have not been manually rechecked for the updated plugin.
- The view shows the richer description, but WHY still repeats overall intent and WORKFLOW EFFECT shows a file-target count.
- CI uses --skip-diff and runs no plugin build or Java checks.
- Illustrative manifests include hypothetical paths and sample test results; their validity is structural, and some prose needs editorial correction.
- The legacy Markdown parser and older implementation-plan status notes lag behind the current JSON consumer.
- Structure verification previously warned that the existing plugin ID contains pycharm. The build succeeded; the ID was retained for installed-plugin compatibility.
- The scrollable card compiles with the local IDE, but live PyCharm scrolling with long text at narrow widths still requires a manual check.
- The persistent sidebar is tested as a Swing component; native diff-processor integration and installation of 0.1.1 still need a live PyCharm check.

## Risks

### Protocol and validator agreement

Risk: A producer can satisfy the schema while a consumer applies different checks or version rules.

Mitigation: Review schema, rule implementation, and diagnostics together; record the native-parser parity gap rather than claiming universal validation.

### Review completeness

Risk: A first-file-only action can conceal other targets in the same step, and a file-level error can interrupt a review.

Mitigation: Use an ordered native chain for all references, retain the clicked index, and check chain membership against the actual manifest.

### PR comparison context

Risk: A stale local target branch or changing checkout can make separate file comparisons use an unintended baseline.

Mitigation: Fetch the base before review, inspect commit labels, and keep the checkout stable while navigating lazy file comparisons. Group-wide pinning remains a gap.

### Code target accuracy

Risk: A valid but stale line range can select unrelated text; folding and focus behavior depend on IDE APIs.

Mitigation: Derive targets from the inspected final file, clamp editor bounds, and perform the remaining live UI checks.

### Evidence scope

Risk: Sample test evidence, local build results, and parser diagnostics can be mistaken for proof of the whole workflow.

Mitigation: Keep illustrative samples, observed command results, and unverified UI behavior explicit in steps and verification gaps.

## Assumptions

- This manifest covers the entire initial uncommitted foundation, not only the latest skill edit. Most source files remain untracked.
- The current code targets describe working-tree files. PR mode requires a manifest whose targets match committed HEAD, or old-side content for deletions.
- Local PyCharm 2024.3 and workspace JDK/Gradle are build inputs; they are not distributed source.

## Limitations

- The producer skill is repository-local and has not been installed globally.
- No remote PR was created or published during this refresh.
- This rewrite improves authored explanations within protocol 0.1; it does not change the schema, renderer, or plugin layout.
- The skill-creator quick validator accepted the updated skill. That check validates skill structure, not the quality of future agent output.
