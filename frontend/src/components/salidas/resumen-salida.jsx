import { EstadoSalidaBadge } from '@/components/guia/estado-salida-badge';
import { formatearRangoFechas } from '@/components/guia/formato';
import { hora } from '@/components/reserva/formato';

export const ESTADOS_SALIDA = [
  { valor: 'DISPONIBLE', etiqueta: 'Disponible' },
  { valor: 'COMPLETA', etiqueta: 'Completa' },
  { valor: 'EN_CURSO', etiqueta: 'En curso' },
  { valor: 'FINALIZADA', etiqueta: 'Finalizada' },
  { valor: 'CANCELADA', etiqueta: 'Cancelada' },
];

/** "14 al 16 de noviembre, 2026 · 08:00 a 17:30 h" */
export function cronograma(s) {
  return `${formatearRangoFechas(s.fechaInicio, s.fechaFinalizacion)} · ${hora(s.horaInicio)} a ${hora(s.horaFinalizacion)} h`;
}

function Dato({ etiqueta, children }) {
  return (
    <div className="flex min-w-0 flex-col gap-0.5">
      <span className="text-xs text-muted-foreground">{etiqueta}</span>
      <span className="text-sm font-semibold text-foreground">{children}</span>
    </div>
  );
}

/** Tarjeta con los datos principales de la salida (encabezado de CU-11, CU-12 y CU-14). */
export function ResumenSalida({ salida, extra }) {
  return (
    <section className="grid gap-4 rounded-2xl border border-border bg-card px-5 py-4 sm:grid-cols-2 lg:grid-cols-4">
      <Dato etiqueta="Excursión">{salida.excursion}</Dato>
      <Dato etiqueta="Fecha">{cronograma(salida)}</Dato>
      <Dato etiqueta="Guía responsable">{salida.guiaResponsable?.nombre ?? '—'}</Dato>
      <div className="flex flex-col items-start gap-1">
        <span className="text-xs text-muted-foreground">Estado actual</span>
        <EstadoSalidaBadge estado={salida.estadoSalida} className="h-5 px-2.5" />
      </div>
      {extra}
    </section>
  );
}

/** Migas: Salidas / Excursión — fecha / Pantalla */
export function Migas({ salida, pantalla }) {
  return (
    <p className="text-sm text-muted-foreground">
      Salidas{salida && ` / ${salida.excursion} — ${formatearRangoFechas(salida.fechaInicio, salida.fechaInicio)}`}
      {pantalla && ` / ${pantalla}`}
    </p>
  );
}
