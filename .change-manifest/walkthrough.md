# Test PR: three demo functions

## Intent

Provide a small real PR for trying the aMazePR guided diff viewer.

## Scope

Three independent Python demo functions and their three-step review walkthrough.

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

## Verification

Commands:
- `passed` `python -m unittest discover -s tests`: All 15 existing tests passed.
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
