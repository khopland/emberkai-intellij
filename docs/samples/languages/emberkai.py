"""Check Python colors in PyCharm, or IntelliJ with the Python plugin: decorators, self,
type hints and type parameters, f-strings, built-ins, and keyword arguments."""
from dataclasses import dataclass, field
from enum import Enum


class Tone(Enum):
    WARM = "warm"
    COOL = "cool"


@dataclass(frozen=True)
class Swatch:
    """A palette color.

    :param name: shown in the legend
    """

    name: str
    hex: int
    tags: list[str] = field(default_factory=list)

    def describe(self, *, upper: bool = False) -> str:
        label = self.name.upper() if upper else self.name
        return f"{label:<8} #{self.hex:06x} {self.tags!r}\n"


def first[T](items: list[T], default: T | None = None) -> T | None:
    # Built-ins and special names
    return items[0] if len(items) > 0 else default


if __name__ == "__main__":
    ember = Swatch("ember", 0xFD9353, tags=["accent"])
    print(ember.describe(upper=True), first([Tone.WARM, Tone.COOL]), sep="")
