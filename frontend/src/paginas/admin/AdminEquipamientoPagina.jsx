import { useCallback, useEffect, useState } from 'react';
import { Info, Loader2, RotateCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { EstadoReservaBadge } from '@/components/reserva/estado-reserva-badge';
import { CLASE_SELECT, formatearRangoFechas } from '@/components/reserva/formato';
import { consultarStockEquipamiento, listarSalidasParaEquipamiento } from '@/api/equipamiento-stock';
import { cn } from '@/lib/utils';

const SEGUNDOS_ACTUALIZACION = 10;

const ESTADO_ASIGNACION = {
  RETENIDO: { etiqueta: 'Retenido', clase: 'bg-warning/15 text-warning' },
  CONSOLIDADO: { etiqueta: 'Prestado', clase: 'bg-info text-info-foreground' },
};

/** Barra con las tres partes del stock: prestado, retenido y disponible. */
function BarraStock({ item }) {
  const total = Math.max(item.stockTotal, 1);
  const ancho = (n) => `${(n / total) * 100}%`;
  return (
    <div
      className="flex h-2.5 w-full overflow-hidden rounded-full bg-muted"
      role="img"
      aria-label={`${item.prestado} prestados, ${item.retenido} retenidos y ${item.disponible} disponibles de ${item.stockTotal}`}
    >
      <span className="h-full bg-info-foreground/70" style={{ width: ancho(item.prestado) }} />
      <span className="h-full bg-warning" style={{ width: ancho(item.retenido) }} />
      <span className="h-full bg-primary" style={{ width: ancho(item.disponible) }} />
    </div>
  );
}

function Referencia({ clase, children }) {
  return (
    <span className="flex items-center gap-1.5">
      <span className={cn('size-2.5 rounded-full', clase)} aria-hidden="true" />
      {children}
    </span>
  );
}

/**
 * Consulta de disponibilidad del equipamiento (ERS, "Gestión de equipamiento").
 * Solo lectura: el alta, la baja y la reposición del stock quedan fuera del alcance del sistema.
 * Se actualiza sola cada 10 segundos para ver en vivo cómo una reserva retiene y libera el equipamiento.
 */
export default function AdminEquipamientoPagina() {
  const [salidas, setSalidas] = useState([]);
  const [idSalida, setIdSalida] = useState('');
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);
  const [actualizado, setActualizado] = useState(null);

  useEffect(() => {
    listarSalidasParaEquipamiento()
      .then(setSalidas)
      .catch(() => setSalidas([]));
  }, []);

  const cargar = useCallback(() => {
    setCargando(true);
    return consultarStockEquipamiento(idSalida)
      .then((d) => {
        setDatos(d);
        setError('');
        setActualizado(new Date());
      })
      .catch((e) => setError(e.message))
      .finally(() => setCargando(false));
  }, [idSalida]);

  useEffect(() => {
    cargar();
    const t = setInterval(cargar, SEGUNDOS_ACTUALIZACION * 1000);
    return () => clearInterval(t);
  }, [cargar]);

  return (
    <div className="flex flex-col gap-5">
      <div>
        <p className="text-sm text-muted-foreground">Equipamiento</p>
        <h1 className="font-serif text-3xl font-semibold text-foreground">Disponibilidad del equipamiento</h1>
        <p className="max-w-2xl text-muted-foreground">
          Stock del catálogo precargado: cuánto está retenido por reservas que esperan el pago, cuánto está prestado y
          cuánto queda disponible.
        </p>
      </div>

      <section aria-label="Filtro" className="rounded-2xl border border-border bg-card px-5 py-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex min-w-0 flex-1 flex-col gap-1.5">
            <Label htmlFor="eq-salida">Ver disponibilidad para</Label>
            <select
              id="eq-salida"
              value={idSalida}
              onChange={(e) => setIdSalida(e.target.value)}
              className={cn(CLASE_SELECT, 'min-w-0 truncate')}
            >
              <option value="">Hoy</option>
              {salidas.map((s) => (
                <option key={s.idSalida} value={s.idSalida}>
                  {s.excursion} — {formatearRangoFechas(s.fechaInicio, s.fechaFinalizacion)} (salida #{s.idSalida})
                </option>
              ))}
            </select>
          </div>
          <Button type="button" variant="outline" className="h-10 px-4" onClick={cargar} disabled={cargando}>
            <RotateCw className={cn(cargando && 'animate-spin')} aria-hidden="true" /> Actualizar
          </Button>
        </div>
        <p className="mt-2 text-xs text-muted-foreground" aria-live="polite">
          {actualizado &&
            `Actualizado a las ${actualizado.toLocaleTimeString('es-AR')}. Se actualiza solo cada ${SEGUNDOS_ACTUALIZACION} segundos.`}
        </p>
      </section>

      {error && (
        <p className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">
          {error}
        </p>
      )}

      {!datos ? (
        !error && (
          <p className="flex items-center gap-2 py-8 text-muted-foreground">
            <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando…
          </p>
        )
      ) : (
        <>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="font-semibold text-foreground">
              {datos.salida
                ? `${datos.salida.excursion} · ${formatearRangoFechas(datos.salida.fechaInicio, datos.salida.fechaFinalizacion)}`
                : 'Disponibilidad de hoy'}
            </h2>
            <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
              <Referencia clase="bg-info-foreground/70">Prestado</Referencia>
              <Referencia clase="bg-warning">Retenido</Referencia>
              <Referencia clase="bg-primary">Disponible</Referencia>
            </div>
          </div>

          <div className="relative overflow-x-auto rounded-2xl border border-border bg-card">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b border-border bg-muted/60 text-xs uppercase tracking-wide text-muted-foreground">
                <tr>
                  <th className="px-4 py-3 font-medium">Ítem</th>
                  <th className="px-4 py-3 text-right font-medium">Stock total</th>
                  <th className="px-4 py-3 text-right font-medium">Retenido</th>
                  <th className="px-4 py-3 text-right font-medium">Prestado</th>
                  <th className="px-4 py-3 text-right font-medium">Disponible</th>
                  <th className="w-[28%] px-4 py-3 font-medium">
                    <span className="sr-only">Gráfico</span>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {datos.items.map((i) => (
                  <tr key={i.idEquipamiento}>
                    <td className="px-4 py-3">
                      <p className="font-semibold text-foreground">{i.nombre}</p>
                      {i.descripcion && <p className="text-xs text-muted-foreground">{i.descripcion}</p>}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">{i.stockTotal}</td>
                    <td className={cn('px-4 py-3 text-right tabular-nums', i.retenido > 0 && 'font-semibold text-warning')}>
                      {i.retenido}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">{i.prestado}</td>
                    <td
                      className={cn(
                        'px-4 py-3 text-right font-semibold tabular-nums',
                        i.disponible === 0 ? 'text-destructive' : 'text-foreground',
                      )}
                    >
                      {i.disponible}
                    </td>
                    <td className="px-4 py-3">
                      <BarraStock item={i} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <p className="flex gap-2 text-xs text-muted-foreground">
            <Info className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
            <span>
              <strong>Retenido:</strong> lo apartan las reservas pendientes de pago durante 10 minutos; si el pago se
              rechaza o vence el plazo, vuelve a estar disponible. <strong>Prestado:</strong> lo tienen asignado reservas
              ya pagadas en salidas de {datos.salida ? 'esas fechas' : 'hoy'}. El alta y la reposición del stock no se
              gestionan en el sistema.
            </span>
          </p>

          {datos.salida && (
            <section aria-labelledby="titulo-asignaciones" className="flex flex-col gap-2">
              <h2 id="titulo-asignaciones" className="font-semibold text-foreground">
                Equipamiento pedido en esta salida
              </h2>
              {datos.asignaciones.length === 0 ? (
                <p className="rounded-2xl border border-dashed border-input px-6 py-8 text-center text-sm text-muted-foreground">
                  Ninguna reserva de esta salida pidió equipamiento.
                </p>
              ) : (
                <div className="relative overflow-x-auto rounded-2xl border border-border bg-card">
                  <table className="w-full min-w-[640px] text-left text-sm">
                    <thead className="border-b border-border bg-muted/60 text-xs uppercase tracking-wide text-muted-foreground">
                      <tr>
                        <th className="px-4 py-3 font-medium">Reserva</th>
                        <th className="px-4 py-3 font-medium">Cliente</th>
                        <th className="px-4 py-3 font-medium">Estado de la reserva</th>
                        <th className="px-4 py-3 font-medium">Ítem</th>
                        <th className="px-4 py-3 text-right font-medium">Cantidad</th>
                        <th className="px-4 py-3 font-medium">Equipamiento</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {datos.asignaciones.map((a) => {
                        const estado = ESTADO_ASIGNACION[a.estadoAsignacion] ?? { etiqueta: a.estadoAsignacion, clase: '' };
                        return (
                          <tr key={`${a.idReserva}-${a.equipamiento}`}>
                            <td className="px-4 py-3 font-semibold text-foreground">#{a.idReserva}</td>
                            <td className="px-4 py-3 whitespace-nowrap">{a.cliente}</td>
                            <td className="px-4 py-3">
                              <EstadoReservaBadge estado={a.estadoReserva} className="h-5 px-2.5" />
                            </td>
                            <td className="px-4 py-3">{a.equipamiento}</td>
                            <td className="px-4 py-3 text-right tabular-nums">{a.cantidad}</td>
                            <td className="px-4 py-3">
                              <span className={cn('inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold', estado.clase)}>
                                {estado.etiqueta}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}
        </>
      )}
    </div>
  );
}
