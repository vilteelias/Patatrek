import { cn } from '@/lib/utils';

/**
 * Chips de pasos del asistente (prototipo pág. 9-11).
 * pasos: [{ clave, titulo }], actual: clave, habilitados: Set de claves a las que se puede volver.
 */
export function PasosReserva({ pasos, actual, habilitados, onIr }) {
  const indiceActual = pasos.findIndex((p) => p.clave === actual);
  return (
    <ol className="flex flex-wrap gap-2" aria-label="Pasos de la reserva">
      {pasos.map((p, i) => {
        const esActual = p.clave === actual;
        const hecho = i < indiceActual;
        const puedeIr = !esActual && habilitados.has(p.clave);
        return (
          <li key={p.clave}>
            <button
              type="button"
              onClick={() => puedeIr && onIr(p.clave)}
              aria-disabled={!puedeIr || undefined}
              tabIndex={puedeIr ? 0 : -1}
              aria-current={esActual ? 'step' : undefined}
              className={cn(
                'rounded-md px-2.5 py-1 text-xs font-medium transition-colors',
                esActual && 'bg-primary text-primary-foreground',
                !esActual && hecho && 'bg-accent text-accent-foreground',
                !esActual && !hecho && 'bg-muted text-muted-foreground/70',
                puedeIr && 'cursor-pointer hover:bg-accent/70',
                !puedeIr && !esActual && 'cursor-default',
              )}
            >
              {i + 1} · {p.titulo}
            </button>
          </li>
        );
      })}
    </ol>
  );
}
