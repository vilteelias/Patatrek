import { Users } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { EstadoSalidaBadge } from './estado-salida-badge';
import { formatearHorario, formatearRangoFechas } from './formato';
export function SalidaCard({ salida, onVerParticipantes }) {
  const inactiva = salida.estadoSalida === 'FINALIZADA' || salida.estadoSalida === 'CANCELADA';
  const rol = salida.rolGuia === 'RESPONSABLE' ? 'Guía responsable' : 'Guía ayudante';
  const datos = [
    { etiqueta: 'Participantes', valor: `${salida.cuposOcupados} / ${salida.cupoMaximo}` },
    { etiqueta: 'Guía responsable', valor: salida.guiaResponsable },
    {
      etiqueta: 'Guías ayudantes',
      valor: salida.guiasAyudantes.length ? salida.guiasAyudantes.join(', ') : '—',
    },
    { etiqueta: 'Refugio/s', valor: salida.refugios.length ? salida.refugios.join(', ') : '—' },
  ];
  return (
    <Card
      className={cn('gap-0 rounded-xl px-5 py-5 shadow-none ring-border sm:px-8 sm:py-7', inactiva && 'bg-card/60')}
    >
      <article aria-labelledby={`salida-${salida.idSalida}`} className={cn(inactiva && 'opacity-75')}>
        <div className="flex flex-col-reverse items-start gap-2 sm:flex-row sm:justify-between sm:gap-4">
          <div className="min-w-0">
            <h2 id={`salida-${salida.idSalida}`} className="text-base font-semibold text-foreground sm:text-lg">
              {salida.nombreExcursion}
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {formatearRangoFechas(salida.fechaInicio, salida.fechaFinalizacion)}
              <span aria-hidden="true"> · </span>
              {formatearHorario(salida.horaInicio, salida.horaFinalizacion)}
              <span aria-hidden="true"> · </span>
              Rol: {rol}
            </p>
          </div>
          <EstadoSalidaBadge estado={salida.estadoSalida} />
        </div>

        <hr className="my-4 border-border" />

        <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:flex sm:flex-wrap sm:gap-x-10">
          {datos.map((dato) => (
            <div key={dato.etiqueta} className="min-w-0">
              <dt className="text-xs text-muted-foreground">{dato.etiqueta}</dt>
              <dd className="mt-0.5 text-sm font-semibold break-words text-foreground">{dato.valor}</dd>
            </div>
          ))}
        </dl>

        <div className="mt-5 flex justify-end">
          <Button
            variant={inactiva ? 'outline' : 'default'}
            onClick={() => onVerParticipantes(salida.idSalida)}
            className="h-11 w-full px-6 text-sm font-semibold sm:w-auto"
          >
            <Users aria-hidden="true" />
            Ver participantes
          </Button>
        </div>
      </article>
    </Card>
  );
}
export function SalidaCardSkeleton() {
  return (
    <Card className="gap-0 rounded-xl px-5 py-5 shadow-none ring-border sm:px-8 sm:py-7" aria-hidden="true">
      <div className="flex justify-between gap-4">
        <div className="flex flex-1 flex-col gap-2">
          <div className="h-5 w-2/3 animate-pulse rounded-md bg-muted" />
          <div className="h-4 w-1/2 animate-pulse rounded-md bg-muted" />
        </div>
        <div className="h-6 w-20 animate-pulse rounded-full bg-muted" />
      </div>
      <hr className="my-4 border-border" />
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="flex flex-col gap-1.5">
            <div className="h-3 w-16 animate-pulse rounded-md bg-muted" />
            <div className="h-4 w-24 animate-pulse rounded-md bg-muted" />
          </div>
        ))}
      </div>
      <div className="mt-5 flex justify-end">
        <div className="h-11 w-full animate-pulse rounded-lg bg-muted sm:w-44" />
      </div>
    </Card>
  );
}
