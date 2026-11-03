import { useEffect, useMemo, useState } from 'react';
import { Clock, Loader2, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { EstadoReservaBadge } from '@/components/reserva/estado-reserva-badge';
import { CancelacionAutomaticaDialog } from '@/components/reserva/cancelacion-automatica-dialog';
import {
  ALOJAMIENTO,
  CLASE_SELECT,
  ESTADO_PAGO,
  ESTADOS_RESERVA,
  MODO_PAGO,
  fechaHora,
  formatearRangoFechas,
  pesos,
} from '@/components/reserva/formato';
import { listarReservasAdmin, obtenerReservaAdmin } from '@/api/reservas';
import { cn } from '@/lib/utils';

const SIN_FILTROS = { excursion: '', idSalida: '', desde: '', hasta: '', dni: '', estado: '' };

const COLOR_PAGO = {
  APROBADO: 'bg-accent text-accent-foreground',
  PENDIENTE: 'bg-secondary text-secondary-foreground',
  RECHAZADO: 'bg-destructive/10 text-destructive',
  DEVUELTO: 'bg-lavender text-lavender-foreground',
};

function EstadoPagoBadge({ estado }) {
  return (
    <span className={cn('inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold', COLOR_PAGO[estado])}>
      {ESTADO_PAGO[estado] ?? estado}
    </span>
  );
}

/** Espera a que el usuario deje de escribir para no consultar en cada tecla. */
function useDemorado(valor, ms = 350) {
  const [demorado, setDemorado] = useState(valor);
  useEffect(() => {
    const t = setTimeout(() => setDemorado(valor), ms);
    return () => clearTimeout(t);
  }, [valor, ms]);
  return demorado;
}

/** CU-20 Listar reservas generales (Administrador) — RF-17. */
export default function AdminReservasPagina() {
  const [filtros, setFiltros] = useState(SIN_FILTROS);
  const demorados = useDemorado(filtros);
  const [reservas, setReservas] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [detalle, setDetalle] = useState(null); // { id, reserva? , error? }
  const [cancelacionAbierta, setCancelacionAbierta] = useState(false);
  const [recarga, setRecarga] = useState(0);

  const errorFechas =
    demorados.desde && demorados.hasta && demorados.hasta < demorados.desde
      ? 'La fecha "hasta" no puede ser anterior a la fecha "desde".'
      : '';

  useEffect(() => {
    if (errorFechas) return undefined;
    let vigente = true;
    setCargando(true);
    listarReservasAdmin(demorados)
      .then((r) => {
        if (!vigente) return;
        setReservas(r);
        setError('');
      })
      .catch((e) => vigente && setError(e.message))
      .finally(() => vigente && setCargando(false));
    return () => {
      vigente = false;
    };
  }, [demorados, errorFechas, recarga]);

  const abrirDetalle = (id) => {
    setDetalle({ id });
    obtenerReservaAdmin(id)
      .then((reserva) => setDetalle((d) => (d?.id === id ? { id, reserva } : d)))
      .catch((e) => setDetalle((d) => (d?.id === id ? { id, error: e.message } : d)));
  };

  const cambiar = (campo) => (e) => setFiltros((f) => ({ ...f, [campo]: e.target.value }));
  const hayFiltros = Object.values(filtros).some((v) => v !== '');

  const totales = useMemo(() => {
    if (!reservas) return null;
    const vigentes = reservas.filter((r) => ['SENADA', 'CONFIRMADA'].includes(r.estadoReserva));
    return {
      cantidad: reservas.length,
      participantes: vigentes.reduce((a, r) => a + r.cantidadParticipantes, 0),
    };
  }, [reservas]);

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm text-muted-foreground">Gestión comercial</p>
          <h1 className="font-serif text-3xl font-semibold text-foreground">Reservas</h1>
          <p className="text-muted-foreground">Catálogo general de todas las reservas del sistema.</p>
        </div>
        <Button variant="outline" className="h-10 shrink-0 px-4" onClick={() => setCancelacionAbierta(true)}>
          <Clock aria-hidden="true" /> Cancelación automática
        </Button>
      </div>

      <section aria-label="Filtros" className="rounded-2xl border border-border bg-card px-5 py-4">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
          <div className="flex flex-col gap-1.5 xl:col-span-2">
            <Label htmlFor="fa-excursion">Excursión</Label>
            <div className="relative">
              <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
              <Input
                id="fa-excursion"
                value={filtros.excursion}
                onChange={cambiar('excursion')}
                placeholder="Nombre de la excursión"
                className="h-10 bg-card pl-8"
              />
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="fa-salida">ID de salida</Label>
            <Input
              id="fa-salida"
              type="number"
              min="1"
              value={filtros.idSalida}
              onChange={cambiar('idSalida')}
              className="h-10 bg-card"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="fa-dni">DNI del cliente</Label>
            <Input
              id="fa-dni"
              inputMode="numeric"
              value={filtros.dni}
              onChange={cambiar('dni')}
              placeholder="Sin puntos"
              className="h-10 bg-card"
            />
          </div>
          <div className="flex flex-col gap-1.5 xl:col-span-2">
            <Label htmlFor="fa-estado">Estado de reserva</Label>
            <select id="fa-estado" value={filtros.estado} onChange={cambiar('estado')} className={CLASE_SELECT}>
              <option value="">Todos los estados</option>
              {ESTADOS_RESERVA.map((e) => (
                <option key={e.valor} value={e.valor}>
                  {e.etiqueta}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="fa-desde">Salida desde</Label>
            <Input id="fa-desde" type="date" value={filtros.desde} onChange={cambiar('desde')} className="h-10 bg-card" />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="fa-hasta">Salida hasta</Label>
            <Input
              id="fa-hasta"
              type="date"
              value={filtros.hasta}
              onChange={cambiar('hasta')}
              aria-invalid={errorFechas ? true : undefined}
              className="h-10 bg-card"
            />
          </div>
          <div className="flex items-end">
            <Button
              type="button"
              variant="outline"
              className="h-10 w-full px-4"
              onClick={() => setFiltros(SIN_FILTROS)}
              disabled={!hayFiltros}
            >
              Limpiar filtros
            </Button>
          </div>
        </div>
        {errorFechas && (
          <p className="mt-2 text-xs text-destructive" role="alert">
            {errorFechas}
          </p>
        )}
      </section>

      {error && (
        <p className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">
          {error}
        </p>
      )}

      {totales && (
        <p className="flex items-center gap-2 text-sm text-muted-foreground" aria-live="polite">
          {cargando && <Loader2 className="size-3.5 animate-spin" aria-hidden="true" />}
          {totales.cantidad === 1 ? '1 reserva' : `${totales.cantidad} reservas`} · {totales.participantes}{' '}
          participantes en reservas vigentes
        </p>
      )}

      {reservas === null ? (
        <p className="py-8 text-muted-foreground">Cargando reservas…</p>
      ) : reservas.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-input px-6 py-12 text-center text-muted-foreground">
          {hayFiltros ? 'No hay reservas para los filtros seleccionados.' : 'Todavía no hay reservas registradas.'}
        </p>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-border bg-card">
          <table className="w-full min-w-[860px] text-left text-sm">
            <thead className="border-b border-border bg-muted/60 text-xs text-muted-foreground">
              <tr>
                <th className="px-4 py-3 font-medium">Reserva</th>
                <th className="px-4 py-3 font-medium">Excursión y salida</th>
                <th className="px-4 py-3 font-medium">Cliente titular</th>
                <th className="px-4 py-3 text-center font-medium">Part.</th>
                <th className="px-4 py-3 font-medium">Estado</th>
                <th className="px-4 py-3 font-medium">Pago</th>
                <th className="px-4 py-3 text-right font-medium">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {reservas.map((r) => (
                <tr
                  key={r.idReserva}
                  className="cursor-pointer transition-colors hover:bg-muted/50 focus-visible:bg-muted/50 focus-visible:outline-none"
                  tabIndex={0}
                  onClick={() => abrirDetalle(r.idReserva)}
                  onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), abrirDetalle(r.idReserva))}
                  aria-label={`Ver detalle de la reserva ${r.idReserva}`}
                >
                  <td className="px-4 py-3">
                    <p className="font-semibold text-foreground">#{r.idReserva}</p>
                    <p className="text-xs text-muted-foreground">{fechaHora(r.fechaHora)}</p>
                  </td>
                  <td className="px-4 py-3">
                    <p className="font-medium text-foreground">{r.excursion}</p>
                    <p className="text-xs text-muted-foreground">
                      Salida #{r.idSalida} · {formatearRangoFechas(r.fechaInicio, r.fechaFinalizacion)}
                    </p>
                  </td>
                  <td className="px-4 py-3">
                    <p className="text-foreground">{r.cliente}</p>
                    <p className="text-xs text-muted-foreground">DNI {r.dniCliente}</p>
                  </td>
                  <td className="px-4 py-3 text-center">{r.cantidadParticipantes}</td>
                  <td className="px-4 py-3">
                    <EstadoReservaBadge estado={r.estadoReserva} className="h-5 px-2.5" />
                  </td>
                  <td className="px-4 py-3">
                    <EstadoPagoBadge estado={r.estadoPago} />
                  </td>
                  <td className="px-4 py-3 text-right font-semibold">{pesos(r.montoTotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <DetalleReservaDialog detalle={detalle} onCerrar={() => setDetalle(null)} />
      <CancelacionAutomaticaDialog
        abierto={cancelacionAbierta}
        onTerminar={(huboCambios) => {
          setCancelacionAbierta(false);
          if (huboCambios) setRecarga((n) => n + 1);
        }}
      />
    </div>
  );
}

function DetalleReservaDialog({ detalle, onCerrar }) {
  const r = detalle?.reserva;
  return (
    <Dialog open={!!detalle} onOpenChange={(abrir) => !abrir && onCerrar()}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto rounded-xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="font-serif text-xl font-semibold">Reserva #{detalle?.id}</DialogTitle>
          <DialogDescription>{r ? r.salida.excursion : 'Detalle de la reserva'}</DialogDescription>
        </DialogHeader>

        {!r && !detalle?.error && (
          <p className="flex items-center gap-2 py-6 text-muted-foreground">
            <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando…
          </p>
        )}
        {detalle?.error && <p className="text-sm text-destructive">{detalle.error}</p>}

        {r && (
          <div className="flex flex-col gap-4 text-sm">
            <div className="flex flex-wrap items-center gap-2">
              <EstadoReservaBadge estado={r.estadoReserva} />
              <span className="text-muted-foreground">Registrada el {fechaHora(r.fechaHora)}</span>
            </div>
            {r.motivoCancelacion && (
              <p className="rounded-lg bg-destructive/10 px-3 py-2 text-destructive">{r.motivoCancelacion}</p>
            )}
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2">
              <dt className="text-muted-foreground">Salida</dt>
              <dd>{formatearRangoFechas(r.salida.fechaInicio, r.salida.fechaFinalizacion)}</dd>
              {r.modalidadAlojamiento && (
                <>
                  <dt className="text-muted-foreground">Alojamiento</dt>
                  <dd>
                    {ALOJAMIENTO[r.modalidadAlojamiento]}
                    {r.refugios.length > 0 && ` (${r.refugios.map((x) => x.nombre).join(', ')})`}
                  </dd>
                </>
              )}
              <dt className="text-muted-foreground">Monto total</dt>
              <dd className="font-semibold">{pesos(r.montoTotal)}</dd>
              <dt className="text-muted-foreground">Abonado</dt>
              <dd>{pesos(r.importeAbonado)}</dd>
              <dt className="text-muted-foreground">Saldo pendiente</dt>
              <dd>{pesos(r.saldoPendiente)}</dd>
            </dl>

            <div>
              <p className="mb-1 font-semibold">Participantes ({r.participantes.length})</p>
              <ul className="divide-y divide-border rounded-lg border border-border">
                {r.participantes.map((p) => (
                  <li key={p.dni} className="flex flex-wrap justify-between gap-2 px-3 py-2">
                    <span>
                      {p.apellido}, {p.nombre}
                    </span>
                    <span className="text-muted-foreground">DNI {p.dni}</span>
                  </li>
                ))}
              </ul>
            </div>

            {r.equipamiento.length > 0 && (
              <div>
                <p className="mb-1 font-semibold">Equipamiento en préstamo</p>
                <ul className="text-muted-foreground">
                  {r.equipamiento.map((e) => (
                    <li key={e.nombre}>
                      {e.nombre} × {e.cantidad}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <div>
              <p className="mb-1 font-semibold">Pagos</p>
              {r.pagos.length === 0 ? (
                <p className="text-muted-foreground">Sin pagos registrados.</p>
              ) : (
                <ul className="divide-y divide-border rounded-lg border border-border">
                  {r.pagos.map((p) => (
                    <li key={p.idPago} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
                      <span>
                        {MODO_PAGO[p.modoPago]} · {pesos(p.importe)}
                        <span className="block text-xs text-muted-foreground">
                          {fechaHora(p.fechaPago)} · {p.nroTransaccion}
                        </span>
                      </span>
                      <EstadoPagoBadge estado={p.estadoPago} />
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
