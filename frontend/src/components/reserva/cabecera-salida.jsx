import { formatearRangoFechas, pesos } from './formato';

/** Tarjeta superior del asistente: excursión, fechas, precio y cupos (prototipo pág. 9). */
export function CabeceraSalida({ excursion, fechaInicio, fechaFinalizacion, precio, cupos }) {
  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-border bg-card px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
      <div className="min-w-0">
        <p className="font-semibold text-foreground">{excursion}</p>
        <p className="text-sm text-muted-foreground">
          Salida: {formatearRangoFechas(fechaInicio, fechaFinalizacion)} · {pesos(precio)} por persona
        </p>
      </div>
      {cupos !== undefined && (
        <span className="w-fit shrink-0 rounded-full bg-accent px-3 py-1 text-xs font-semibold text-accent-foreground">
          {cupos === 1 ? '1 cupo disponible' : `${cupos} cupos disponibles`}
        </span>
      )}
    </div>
  );
}
