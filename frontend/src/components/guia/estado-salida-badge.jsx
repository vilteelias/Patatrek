import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
const ESTADOS = {
  DISPONIBLE: { etiqueta: 'Disponible', clase: 'bg-accent text-accent-foreground' },
  COMPLETA: { etiqueta: 'Completa', clase: 'bg-info text-info-foreground' },
  EN_CURSO: { etiqueta: 'En curso', clase: 'bg-secondary text-secondary-foreground' },
  FINALIZADA: { etiqueta: 'Finalizada', clase: 'bg-lavender text-lavender-foreground' },
  CANCELADA: { etiqueta: 'Cancelada', clase: 'bg-destructive/10 text-destructive' },
};
export function EstadoSalidaBadge({ estado, className }) {
  const { etiqueta, clase } = ESTADOS[estado];
  return <Badge className={cn('h-6 shrink-0 px-3 text-xs font-semibold', clase, className)}>{etiqueta}</Badge>;
}
