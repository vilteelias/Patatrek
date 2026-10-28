import { useEffect, useState } from 'react';
import { Loader2, MessageSquareText } from 'lucide-react';
import { EstrellasFijas } from '@/components/valoracion/estrellas';
import { fechaHora, formatearRangoFechas } from '@/components/reserva/formato';
import { detalleValoraciones, resumenValoraciones } from '@/api/valoraciones';
import { cn } from '@/lib/utils';

const promedioTexto = (p) => (p == null ? '—' : p.toLocaleString('es-AR', { minimumFractionDigits: 1 }));

/**
 * CU-29 Consultar valoraciones de excursiones (RF-26): el administrador elige una excursión del listado
 * y ve el promedio general, el total de valoraciones y cada calificación con su comentario.
 */
export default function AdminValoracionesPagina() {
  const [excursiones, setExcursiones] = useState(null);
  const [error, setError] = useState('');
  const [seleccionada, setSeleccionada] = useState(null);
  const [detalle, setDetalle] = useState(null);
  const [cargandoDetalle, setCargandoDetalle] = useState(false);

  useEffect(() => {
    resumenValoraciones()
      .then((lista) => {
        setExcursiones(lista);
        // Se abre la primera que tenga valoraciones
        const primera = lista.find((e) => e.total > 0) ?? lista[0];
        if (primera) setSeleccionada(primera.idExcursion);
      })
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!seleccionada) return;
    let vigente = true;
    setCargandoDetalle(true);
    detalleValoraciones(seleccionada)
      .then((d) => vigente && setDetalle(d))
      .catch((e) => vigente && setError(e.message))
      .finally(() => vigente && setCargandoDetalle(false));
    return () => {
      vigente = false;
    };
  }, [seleccionada]);

  return (
    <div className="flex flex-col gap-5">
      <div>
        <p className="text-sm text-muted-foreground">Calidad del servicio</p>
        <h1 className="font-serif text-3xl font-semibold text-foreground">Valoraciones</h1>
        <p className="text-muted-foreground">Elegí una excursión para ver su promedio y los comentarios de los participantes.</p>
      </div>

      {error && (
        <p className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">
          {error}
        </p>
      )}

      {!excursiones ? (
        <p className="flex items-center gap-2 py-8 text-muted-foreground">
          <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando…
        </p>
      ) : (
        <div className="grid gap-5 lg:grid-cols-[320px_1fr]">
          {/* Listado de excursiones */}
          <nav aria-label="Excursiones" className="flex flex-col gap-2">
            {excursiones.map((e) => {
              const activa = e.idExcursion === seleccionada;
              return (
                <button
                  key={e.idExcursion}
                  type="button"
                  onClick={() => setSeleccionada(e.idExcursion)}
                  aria-current={activa ? 'true' : undefined}
                  className={cn(
                    'flex flex-col gap-1 rounded-xl border px-4 py-3 text-left transition-colors',
                    activa ? 'border-primary bg-accent ring-2 ring-primary/20' : 'border-border bg-card hover:bg-muted/60',
                  )}
                >
                  <span className="flex items-start justify-between gap-2">
                    <span className="font-semibold text-foreground">{e.excursion}</span>
                    {!e.activa && (
                      <span className="shrink-0 rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">
                        Inactiva
                      </span>
                    )}
                  </span>
                  <span className="flex items-center gap-2 text-sm text-muted-foreground">
                    <EstrellasFijas valor={e.promedio ?? 0} />
                    <span className="font-semibold text-foreground">{promedioTexto(e.promedio)}</span>
                    <span>
                      · {e.total === 1 ? '1 valoración' : `${e.total} valoraciones`}
                    </span>
                  </span>
                </button>
              );
            })}
          </nav>

          {/* Detalle */}
          <section aria-live="polite" className="rounded-2xl border border-border bg-card px-5 py-6 sm:px-7">
            {cargandoDetalle && !detalle ? (
              <p className="flex items-center gap-2 text-muted-foreground">
                <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando…
              </p>
            ) : detalle ? (
              <div className={cn('flex flex-col gap-6', cargandoDetalle && 'opacity-60')}>
                <h2 className="font-serif text-2xl font-semibold text-foreground">{detalle.excursion}</h2>

                <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
                  <div className="flex flex-col items-center gap-1 sm:w-44">
                    <span className="font-serif text-5xl font-semibold text-foreground">
                      {promedioTexto(detalle.promedio)}
                    </span>
                    <EstrellasFijas valor={detalle.promedio ?? 0} tamano="size-5" />
                    <span className="text-sm text-muted-foreground">
                      {detalle.total === 1 ? '1 valoración' : `${detalle.total} valoraciones`}
                    </span>
                  </div>
                  <ul className="flex flex-1 flex-col gap-1.5" aria-label="Cantidad por estrellas">
                    {[5, 4, 3, 2, 1].map((n) => {
                      const cant = detalle.cantidadPorEstrellas[n - 1];
                      const pct = detalle.total ? Math.round((cant / detalle.total) * 100) : 0;
                      return (
                        <li key={n} className="flex items-center gap-3 text-sm">
                          <span className="w-20 shrink-0 whitespace-nowrap text-muted-foreground">
                            {n} {n === 1 ? 'estrella' : 'estrellas'}
                          </span>
                          <span className="h-2 flex-1 overflow-hidden rounded-full bg-muted">
                            <span className="block h-full rounded-full bg-warning" style={{ width: `${pct}%` }} />
                          </span>
                          <span className="w-6 text-right text-muted-foreground tabular-nums">{cant}</span>
                        </li>
                      );
                    })}
                  </ul>
                </div>

                <div>
                  <h3 className="mb-2 font-semibold text-foreground">Calificaciones y comentarios</h3>
                  {detalle.valoraciones.length === 0 ? (
                    <p className="rounded-xl border border-dashed border-input px-4 py-8 text-center text-sm text-muted-foreground">
                      Esta excursión todavía no recibió valoraciones.
                    </p>
                  ) : (
                    <ul className="flex flex-col divide-y divide-border">
                      {detalle.valoraciones.map((v) => (
                        <li key={v.idValoracion} className="flex flex-col gap-1.5 py-3">
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <span className="flex items-center gap-2">
                              <EstrellasFijas valor={v.calificacion} />
                              <span className="text-sm font-semibold text-foreground">{v.participante}</span>
                            </span>
                            <span className="text-xs text-muted-foreground">
                              Salida #{v.idSalida} del {formatearRangoFechas(v.fechaSalida, v.fechaSalida)} ·{' '}
                              {fechaHora(v.fecha)}
                            </span>
                          </div>
                          {v.comentario ? (
                            <p className="text-sm text-foreground">{v.comentario}</p>
                          ) : (
                            <p className="flex items-center gap-1.5 text-sm text-muted-foreground italic">
                              <MessageSquareText className="size-3.5" aria-hidden="true" /> Sin comentario
                            </p>
                          )}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            ) : (
              <p className="text-muted-foreground">Elegí una excursión.</p>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
