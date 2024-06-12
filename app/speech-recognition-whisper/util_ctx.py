from imports import *

Speaker = str
"""Represents the speaker's name."""

Location = str
"""Represents the location where the speaker is located."""

@dataclass
class Ctx:
    """Represents the speaking context. Mutable."""
    speaker: Speaker
    location: Location

CTX = Ctx('User', '')
"""Represents the global speaking context. Mutable."""
