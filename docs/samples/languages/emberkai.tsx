// Check TSX colors: tags, components, attributes, and embedded expressions.
declare global {
  namespace JSX {
    interface Element {}
    interface IntrinsicElements {
      [tag: string]: Record<string, unknown>;
    }
  }
}

type SwatchProps = { name: string; hex: number; selected?: boolean };

export function SwatchCard({ name, hex, selected = false }: SwatchProps) {
  const color = `#${hex.toString(16).padStart(6, "0")}`;
  return (
    <figure className={selected ? "swatch selected" : "swatch"} title={name}>
      <div style={{ background: color }} aria-hidden="true" />
      <figcaption>
        {name} &middot; <code>{color}</code>
      </figcaption>
    </figure>
  );
}

export const Legend = () => <SwatchCard name="ember" hex={0xfd9353} selected />;
