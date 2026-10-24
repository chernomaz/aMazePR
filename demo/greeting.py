"""A small greeting example for guided PR review."""


def greet(name: str) -> str:
    """Greet a trimmed name, falling back to World for blank input."""
    return f"Hello, {name.strip() or 'World'}!"
