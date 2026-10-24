"""A text normalization example for guided PR review."""

import re


def slugify(text: str) -> str:
    """Return a lowercase ASCII slug, joining separators with one hyphen."""
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")
