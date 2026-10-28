import { useCallback, useEffect, useState } from 'react';
import { Check, CheckCircle2, Loader2, RefreshCcw, ShieldCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { fechaHora, formatearRangoFechas, pesos } from '@/components/reserva/formato';
import { ejecutarDevolucion, listarDevoluciones } from '@/api/devoluciones';
import { cn } from '@/lib/utils';

/** "2026-09-10T11:04:00" → "10 sep 2026" */
function fechaCorta(valor) {
  if (!valor) return '';
  const d = new Date(String(valor).slice(0, 10) + 'T12:00:00');
  return d.toLocaleDateString('es-AR', { day: 'numeric', month: 'short', year: 'numeric' }).replace(/ de /g, ' ').replace('.', '');
}

function Indicador({ etiqueta, valor }) {
  return (
    <div className="rounded-2xl border border-border bg-card px-5 py-4">
      <p className="text-xs text-muted-foreground">{etiqueta}</p>
      <p className="mt-1 text-2xl font-semibold text-foreground tabular-nums">{valor}</p>
    </div>
  );
}

/**
 * CU-27 Registrar devolución de reserva (RF-24, prototipo pág. 31).
 * El administrador ve las reservas "Pendiente de devolución" (salida cancelada por contingencia o
 * reprogramación rechazada) y ejecuta el reembolso total de lo abonado a través de la pasarela simulada.
 */
export default function AdminDevolucionesPagina() {
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState(null); // { mensaje, nro }
  const [seleccionada, setSeleccionada] = useState(null);

  const cargar = useCallback(
    () =>
      listarDevoluciones()
        .then((d) => {
          setDatos(d);
          setError('');
        })
        .catch((e) => setError(e.message)),
    [],
  );

  useEffect(() => {
    cargar();
  }, [cargar]);

  const confirmar = async () => {
    const r = await ejecutarDevolucion(seleccionada.idReserva);
    setSeleccionada(null);
    const cliente = r.devolucion.cliente.split(', ').reverse().join(' ');
    setAviso({
      mensaje: `Se reintegraron ${pesos(r.importeReintegrado)} a ${cliente} (reserva #${r.idReserva}). Le enviamos el comprobante por correo.`,
      nro: r.nroTransaccionReintegro,
    });
    await cargar();
  };

  const filas = datos ? [...datos.pendientes, ...datos.realizadas] : [];

  return (
    <div className="flex flex-col gap-5">
      <div>
        <p className="text-sm text-muted-foreground">Devoluciones</p>
        <h1 className="font-serif text-3xl font-semibold text-foreground">Reservas pendientes de devolución</h1>
        <p className="max-w-2xl text-muted-foreground">
          Reservas afectadas por la cancelación de una salida o el rechazo de una reprogramación. Ejecutá la devolución
          total a través de la pasarela de pagos para cerrarlas.
        </p>
      </div>

      {aviso && (
        <div
          className="flex items-start gap-3 rounded-xl border border-primary/30 bg-accent px-4 py-3 text-sm text-accent-foreground"
          role="status"
        >
          <CheckCircle2 className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <p className="flex-1">
            <strong>Reintegro finalizado.</strong> {aviso.mensaje}{' '}
            <span className="whitespace-nowrap">N.º de reintegro: {aviso.nro}</span>
          </p>
          <button type="button" className="text-xs underline" onClick={() => setAviso(null)}>
            Cerrar
          </button>
        </div>
      )}

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
          <div className="grid gap-3 sm:grid-cols-3">
            <Indicador etiqueta="Reservas pendientes" valor={datos.pendientes.length} />
            <Indicador etiqueta="Monto total a reintegrar" valor={pesos(datos.montoTotalPendiente)} />
            <Indicador etiqueta="Devoluciones realizadas" valor={datos.realizadas.length} />
          </div>

          {datos.pendientes.length === 0 && (
            <p className="rounded-2xl border border-dashed border-input px-6 py-8 text-center text-muted-foreground">
              No hay reembolsos pendientes por tramitar.
            </p>
          )}

          {filas.length > 0 && (
            <div className="overflow-x-auto rounded-2xl border border-border bg-card">
              <table className="w-full min-w-[900px] text-left text-sm">
                <thead className="border-b border-border bg-muted/60 text-xs uppercase tracking-wide text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 font-medium">Reserva</th>
                    <th className="px-4 py-3 font-medium">Cliente</th>
                    <th className="px-4 py-3 font-medium">Excursión / salida</th>
                    <th className="px-4 py-3 font-medium">Motivo</th>
                    <th className="px-4 py-3 text-right font-medium">Monto</th>
                    <th className="px-4 py-3 font-medium">Fecha</th>
                    <th className="px-4 py-3 text-right font-medium">Acción</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {filas.map((d) => {
                    const pendiente = d.estadoReserva === 'PENDIENTE_DEVOLUCION';
                    return (
                      <tr key={d.idReserva} className={cn(!pendiente && 'bg-muted/30')}>
                        <td className="px-4 py-3 font-semibold text-foreground">#{d.idReserva}</td>
                        <td className="px-4 py-3">
                          <p className="whitespace-nowrap text-foreground">{d.cliente}</p>
                          <p className="text-xs text-muted-foreground">DNI {d.dniCliente}</p>
                        </td>
                        <td className="px-4 py-3">
                          <p className="font-medium text-foreground">{d.excursion}</p>
                          <p className="text-xs text-muted-foreground">
                            {formatearRangoFechas(d.fechaInicio, d.fechaFinalizacion)} ·{' '}
                            {d.cantidadParticipantes === 1 ? '1 plaza' : `${d.cantidadParticipantes} plazas`}
                          </p>
                        </td>
                        <td className="max-w-[260px] px-4 py-3 text-xs text-muted-foreground">{d.motivo}</td>
                        <td className="px-4 py-3 text-right font-semibold text-foreground tabular-nums">
                          {pesos(d.importeAbonado)}
                        </td>
                        <td className="px-4 py-3 text-xs whitespace-nowrap text-muted-foreground">
                          {pendiente ? (
                            <>Pagó el {fechaCorta(d.fechaPago)}</>
                          ) : (
                            <>Devuelta el {fechaCorta(d.fechaReintegro)}</>
                          )}
                        </td>
                        <td className="px-4 py-3 text-right">
                          {pendiente ? (
                            <Button className="h-9 px-3" onClick={() => setSeleccionada(d)}>
                              Ejecutar devolución
                            </Button>
                          ) : (
                            <span
                              className="inline-flex items-center gap-1 rounded-lg bg-accent px-3 py-1.5 text-xs font-semibold text-accent-foreground"
                              title={`N.º de reintegro ${d.nroTransaccionReintegro}`}
                            >
                              <Check className="size-3.5" aria-hidden="true" /> Devuelta
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      <ConfirmarDevolucionDialog
        devolucion={seleccionada}
        onConfirmar={confirmar}
        onCancelar={() => setSeleccionada(null)}
      />
    </div>
  );
}

function Fila({ etiqueta, children }) {
  return (
    <div className="flex justify-between gap-4 py-1.5 text-sm">
      <dt className="text-muted-foreground">{etiqueta}</dt>
      <dd className="text-right font-medium text-foreground">{children}</dd>
    </div>
  );
}

/** Pasos 3 a 6: el administrador confirma y la pasarela procesa el reembolso. */
function ConfirmarDevolucionDialog({ devolucion, onConfirmar, onCancelar }) {
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (devolucion) {
      setProcesando(false);
      setError('');
    }
  }, [devolucion]);

  const confirmar = async () => {
    setProcesando(true);
    setError('');
    try {
      // Una pausa breve para que se vea que la pasarela está procesando
      await new Promise((r) => setTimeout(r, 900));
      await onConfirmar();
    } catch (e) {
      setError(e.message);
      setProcesando(false);
    }
  };

  const d = devolucion;
  return (
    <Dialog open={!!d} onOpenChange={(abrir) => !abrir && !procesando && onCancelar()}>
      <DialogContent showCloseButton={false} className="rounded-xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="font-serif text-xl font-semibold">Ejecutar devolución</DialogTitle>
          <DialogDescription>Reserva #{d?.idReserva}. Se reintegra el total abonado.</DialogDescription>
        </DialogHeader>

        {d && (
          <dl className="divide-y divide-border rounded-lg border border-border px-3">
            <Fila etiqueta="Cliente titular">{d.cliente}</Fila>
            <Fila etiqueta="Excursión">{d.excursion}</Fila>
            <Fila etiqueta="Salida">{formatearRangoFechas(d.fechaInicio, d.fechaFinalizacion)}</Fila>
            <Fila etiqueta="Pago original">
              {d.nroTransaccionOriginal ?? '—'}
              {d.fechaPago && <span className="block text-xs text-muted-foreground">{fechaHora(d.fechaPago)}</span>}
            </Fila>
            <Fila etiqueta="Monto a reintegrar">
              <span className="text-base font-semibold">{pesos(d.importeAbonado)}</span>
            </Fila>
          </dl>
        )}

        <p className="flex gap-2 rounded-lg bg-muted/60 px-3 py-2.5 text-xs text-muted-foreground">
          <ShieldCheck className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          La pasarela de pagos devuelve el dinero al mismo medio con el que pagó el cliente. PataTrek no conoce ni guarda
          los datos de ese medio. Al terminar, el cliente recibe el comprobante por correo.
        </p>

        {error && (
          <p className="text-sm text-destructive" role="alert">
            {error}
          </p>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onCancelar} disabled={procesando}>
            Volver
          </Button>
          <Button onClick={confirmar} disabled={procesando}>
            {procesando ? (
              <>
                <Loader2 className="animate-spin" aria-hidden="true" /> Procesando con la pasarela…
              </>
            ) : (
              <>
                <RefreshCcw aria-hidden="true" /> Ejecutar devolución
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
