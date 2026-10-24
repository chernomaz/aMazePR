# Test PR: three demo functions

## Intent

Provide a small real PR for trying the aMazePR guided diff viewer.

## Scope

Three independent Python demo functions, a guided review walkthrough, and a renderer regression test that supports changing manifest titles.

## Non-Goals

- No changes to plugin or verifier behavior.

## Review Workflow

### 1. Normalize a greeting

greet trims surrounding whitespace and substitutes World for blank input before formatting the greeting. Check that a named and a blank greeting have predictable results.

Files:
- `demo/greeting.py:4-6` - Normalize a greeting

### 2. Clamp an integer

clamp returns the nearest inclusive bound for an out-of-range integer and leaves in-range values unchanged. Reversed bounds raise ValueError so callers cannot silently use an invalid interval. Check boundary, equal-bound, and error cases.

Files:
- `demo/numbers.py:4-8` - Clamp an integer

### 3. Create an ASCII slug

slugify lowercases input, replaces consecutive non-ASCII-alphanumeric characters with one hyphen, and removes edge hyphens. Punctuation-only input produces an empty string; Unicode transliteration is outside this demo. Check repeated separators and empty results.

Files:
- `demo/text.py:6-8` - Create an ASCII slug

### 4. Verify rendering after the manifest changes

The repository manifest now describes the demo PR, so the rendering test must compare the heading with the loaded title. A separate synthetic manifest retains a fixed expected title to verify rendering independently. Confirm both tests pass when the repository manifest changes.

Files:
- `tests/test_change_manifest_walkthrough.py:38-50` - Current manifest title assertion

## Files

### `demo/greeting.py`

Role: `added`

greet trims surrounding whitespace and substitutes World for blank input before formatting the greeting. Check that a named and a blank greeting have predictable results.

### `demo/numbers.py`

Role: `added`

clamp returns the nearest inclusive bound for an out-of-range integer and leaves in-range values unchanged. Reversed bounds raise ValueError so callers cannot silently use an invalid interval. Check boundary, equal-bound, and error cases.

### `demo/text.py`

Role: `added`

slugify lowercases input, replaces consecutive non-ASCII-alphanumeric characters with one hyphen, and removes edge hyphens. Punctuation-only input produces an empty string; Unicode transliteration is outside this demo. Check repeated separators and empty results.

### `.change-manifest/manifest.json`

Role: `modified`

Describe this demo PR and point each review step to its function.

### `.change-manifest/walkthrough.md`

Role: `modified`

Render the demo manifest as a readable Markdown walkthrough.

### `tests/test_change_manifest_walkthrough.py`

Role: `modified`

Assert that rendering uses the current manifest title, replacing a hardcoded bootstrap title that caused this PR to fail CI.

Review notes:
- The synthetic Line target test still checks a fixed expected heading, independently of the repository manifest.

## Verification

Commands:
- `passed` `python -m unittest discover -s tests`: All 15 tests pass after refreshing the demo manifest and replacing the obsolete title expectation.
- `passed` `python - (inline demo smoke checks)`: Named and blank greetings; clamping below, inside, above and at bounds; equal and reversed bounds; slug punctuation and repeated separators all passed.

Known gaps:
- The live PyCharm UI has not been exercised for this PR.

## Risks

### Demo usage

Risk: These small examples could be mistaken for production utilities; slugify drops non-ASCII characters.

Mitigation: Keep the functions in demo/ and document their exact behavior.

## Assumptions

- The requested PR is a demonstration for guided diff review.

## Limitations

- Existing uncommitted README changes are excluded from this PR.
