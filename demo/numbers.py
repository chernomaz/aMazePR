"""A numeric boundary example for guided PR review."""


def clamp(value: int, minimum: int, maximum: int) -> int:
    """Keep an integer inside inclusive bounds; reject reversed bounds."""
    if minimum > maximum:
        raise ValueError("minimum must not exceed maximum")
    return max(minimum, min(value, maximum))
