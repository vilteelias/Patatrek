import { useEffect, useRef, useState } from 'react';
import { Clock } from 'lucide-react';
import { cn } from '@/lib/utils';

/**
 * Cuenta regresiva de la retención de recursos (10 minutos).
 * Arranca desde los segundos que informa el servidor y avisa con onVencido al llegar a 0.
 */
export function ContadorRetencion({ segundos, onVencido, className }) {
  const [restantes, setRestantes] = useState(segundos);
  const avisado = useRef(false);

  useEffect(() => {
    setRestantes(segundos);
    avisado.current = false;
  }, [segundos]);

  useEffect(() => {
    if (restantes <= 0) {
      if (!avisado.current) {
        avisado.current = true;
        onVencido?.();
      }
      return undefined;
    }
    const t = setTimeout(() => setRestantes((s) => s - 1), 1000);
    return () => clearTimeout(t);
  }, [restantes, onVencido]);

  const min = Math.floor(Math.max(0, restantes) / 60);
  const seg = String(Math.max(0, restantes) % 60).padStart(2, '0');
  const urgente = restantes <= 60;

  return (
    <div
      className={cn(
        'flex items-center gap-3 rounded-xl px-4 py-3 text-sm',
        urgente ? 'bg-destructive/10 text-destructive' : 'bg-secondary text-secondary-foreground',
        className,
      )}
      role="timer"
      aria-live="off"
    >
      <Clock className="size-4 shrink-0" aria-hidden="true" />
      <p className="flex-1">
        Retenemos tus cupos, plazas y equipamiento mientras completás el pago. Tiempo restante:
      </p>
      <span className="font-mono text-base font-semibold tabular-nums">
        {min}:{seg}
      </span>
    </div>
  );
}
