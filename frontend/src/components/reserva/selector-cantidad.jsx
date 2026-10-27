import { Minus, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';

/** Botones − / + como en el prototipo. */
export function SelectorCantidad({ id, valor, min = 0, max, onChange, etiqueta }) {
  return (
    <div className="flex items-center gap-3" role="group" aria-label={etiqueta}>
      <Button
        type="button"
        variant="outline"
        size="icon-lg"
        className="size-10"
        onClick={() => onChange(valor - 1)}
        disabled={valor <= min}
        aria-label={`Quitar uno${etiqueta ? ` (${etiqueta})` : ''}`}
      >
        <Minus />
      </Button>
      <output id={id} className="w-8 text-center text-lg font-semibold tabular-nums" aria-live="polite">
        {valor}
      </output>
      <Button
        type="button"
        variant="outline"
        size="icon-lg"
        className="size-10"
        onClick={() => onChange(valor + 1)}
        disabled={max !== undefined && valor >= max}
        aria-label={`Agregar uno${etiqueta ? ` (${etiqueta})` : ''}`}
      >
        <Plus />
      </Button>
    </div>
  );
}
