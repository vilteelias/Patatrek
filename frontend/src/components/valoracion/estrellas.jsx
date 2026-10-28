import { useState } from 'react';
import { Star } from 'lucide-react';
import { cn } from '@/lib/utils';

const TEXTOS = ['', 'Muy mala', 'Mala', 'Buena', 'Muy buena', 'Excelente'];

/**
 * Selector de 1 a 5 estrellas (CU-28). Es un grupo de radios: se puede usar con el mouse o con el teclado (flechas).
 */
export function SelectorEstrellas({ valor, onChange, invalido, describedBy }) {
  const [sobre, setSobre] = useState(0);
  const mostrado = sobre || valor || 0;

  const teclas = (e) => {
    if (e.key === 'ArrowRight' || e.key === 'ArrowUp') {
      e.preventDefault();
      onChange(Math.min(5, (valor || 0) + 1));
    } else if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') {
      e.preventDefault();
      onChange(Math.max(1, (valor || 1) - 1));
    }
  };

  return (
    <div className="flex flex-col items-center gap-1.5">
      <div
        role="radiogroup"
        aria-label="Tu calificación"
        aria-required="true"
        aria-invalid={invalido || undefined}
        aria-describedby={describedBy}
        className={cn('flex gap-1 rounded-xl p-1', invalido && 'ring-2 ring-destructive/50')}
        onMouseLeave={() => setSobre(0)}
        onKeyDown={teclas}
      >
        {[1, 2, 3, 4, 5].map((n) => {
          const llena = n <= mostrado;
          return (
            <button
              key={n}
              type="button"
              role="radio"
              aria-checked={valor === n}
              aria-label={`${n} ${n === 1 ? 'estrella' : 'estrellas'} (${TEXTOS[n]})`}
              tabIndex={valor ? (valor === n ? 0 : -1) : n === 1 ? 0 : -1}
              onClick={() => onChange(n)}
              onMouseEnter={() => setSobre(n)}
              className="rounded-md p-1 transition-transform outline-none hover:scale-110 focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              <Star
                className={cn('size-9', llena ? 'fill-warning text-warning' : 'text-input')}
                strokeWidth={1.5}
                aria-hidden="true"
              />
            </button>
          );
        })}
      </div>
      <p className="h-5 text-sm text-muted-foreground" aria-live="polite">
        {mostrado ? TEXTOS[mostrado] : 'Tocá una estrella para calificar'}
      </p>
    </div>
  );
}

/** Estrellas de solo lectura (con medias estrellas para los promedios). */
export function EstrellasFijas({ valor = 0, tamano = 'size-4', className }) {
  return (
    <span className={cn('inline-flex items-center gap-0.5', className)} aria-label={`${valor} de 5 estrellas`} role="img">
      {[1, 2, 3, 4, 5].map((n) => {
        const relleno = Math.max(0, Math.min(1, valor - (n - 1)));
        return (
          <span key={n} className={cn('relative inline-block', tamano)}>
            <Star className={cn('absolute inset-0 text-input', tamano)} strokeWidth={1.5} aria-hidden="true" />
            {relleno > 0 && (
              <span className="absolute inset-0 overflow-hidden" style={{ width: `${relleno * 100}%` }}>
                <Star className={cn('fill-warning text-warning', tamano)} strokeWidth={1.5} aria-hidden="true" />
              </span>
            )}
          </span>
        );
      })}
    </span>
  );
}
