// Check TypeScript colors: primitive and declared types, type parameters, enum members,
// classes, decorators, functions, and template literals.
enum Tone {
  Warm = "warm",
  Cool = "cool",
}

interface Swatch {
  readonly name: string;
  hex: number;
  tone?: Tone;
}

type Palette<T extends Swatch = Swatch> = Map<string, T>;

function logged(method: Function, context: ClassMethodDecoratorContext): void {
  console.debug(`${String(context.name)} wraps ${method.name}`);
}

export class Theme {
  private static readonly count = 6;

  constructor(private readonly palette: Palette) {}

  @logged
  describe(name: string, upper = false): string {
    const swatch = this.palette.get(name);
    if (!swatch) return `missing ${name}`;
    const label = upper ? swatch.name.toUpperCase() : swatch.name;
    return `${label} #${swatch.hex.toString(16).padStart(6, "0")} ${swatch.tone ?? Tone.Warm}`;
  }
}

export function first<T>(items: readonly T[]): T | undefined {
  return items.length > 0 ? items[0] : undefined;
}
