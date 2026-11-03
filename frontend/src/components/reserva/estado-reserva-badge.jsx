import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

const ESTADOS = {
  PENDIENTE_PAGO: { etiqueta: 'Pendiente de pago', clase: 'bg-secondary text-secondary-foreground' },
  SENADA: { etiqueta: 'Señada', clase: 'bg-info text-info-foreground' },
  CONFIRMADA: { etiqueta: 'Confirmada', clase: 'bg-accent text-accent-foreground' },
  CANCELADA: { etiqueta: 'Cancelada', clase: 'bg-destructive/10 text-destructive' },
  PENDIENTE_DEVOLUCION: { etiqueta: 'Pendiente de devolución', clase: 'bg-lavender text-lavender-foreground' },
  DEVUELTA: { etiqueta: 'Devuelta', clase: 'bg-lavender text-lavender-foreground' },
};

export function EstadoReservaBadge({ estado, className }) {
  const { etiqueta, clase } = ESTADOS[estado] ?? { etiqueta: estado, clase: '' };
  return <Badge className={cn('h-6 shrink-0 px-3 text-xs font-semibold', clase, className)}>{etiqueta}</Badge>;
}
