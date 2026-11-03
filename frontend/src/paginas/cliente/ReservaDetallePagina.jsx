import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { CalendarClock, CheckCircle2, CreditCard, FileText, Info, Loader2, XCircle } from 'lucide-react';
import { Button, buttonVariants } from '@/components/ui/button';
import { CancelarReservaDialog } from '@/components/reserva/cancelar-reserva-dialog';
import { EstadoReservaBadge } from '@/components/reserva/estado-reserva-badge';
import { ContadorRetencion } from '@/components/reserva/contador-retencion';
import {
  ALOJAMIENTO,
  ESTADO_PAGO,
  MODO_PAGO,
  fechaHora,
  formatearRangoFechas,
  hora,
  pesos,
} from '@/components/reserva/formato';
import { cancelarReserva, obtenerReserva, responderReprogramacion } from '@/api/reservas';
import { cn } from '@/lib/utils';
import { Aviso, Cargando, Volver } from './Estados';

function Fila({ etiqueta, children, fuerte }) {
  return (
      <div className="flex justify-between gap-4 py-1.5">
        <dt className="text-muted-foreground">{etiqueta}</dt>
        <dd className={cn('text-right', fuerte ? 'font-semibold text-foreground' : 'text-foreground')}>{children}</dd>
      </div>
  );
}

const ESTADO_EQUIPO = { RETENIDO: 'Retenido', CONSOLIDADO: 'Reservado', LIBERADO: 'Liberado' };

// Faltan más de 72 horas para la salida (CU-24 y CU-26: con 72 hs o menos no se paga ni se cargan certificados)
const faltanMasDe72Horas = (salida) => {
  if (!salida.fechaInicio || !salida.horaInicio) return true;
  const [anio, mes, dia] = salida.fechaInicio.split('-').map(Number);
  const [h, m] = salida.horaInicio.split(':').map(Number);
  const inicio = new Date(anio, mes - 1, dia, h || 0, m || 0);
  return inicio.getTime() - Date.now() > 72 * 60 * 60 * 1000;
};

/** CU-14: la salida se reprogramó y el titular tiene que aceptar la nueva fecha o rechazarla (devolución total). */
function AvisoReprogramacion({ reserva, onRespondida }) {
  const [rechazando, setRechazando] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const r = reserva.reprogramacion;
  const s = reserva.salida;

  const responder = async (acepta) => {
    setEnviando(true);
    setError('');
    try {
      onRespondida(await responderReprogramacion(reserva.idReserva, acepta), acepta);
    } catch (e) {
      setError(e.message);
      setEnviando(false);
    }
  };

  return (
    <section
      aria-labelledby="t-reprogramacion"
      className="flex flex-col gap-3 rounded-2xl border border-amber-300 bg-amber-50 px-5 py-5 text-amber-950 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-100 sm:px-6"
    >
      <h2 id="t-reprogramacion" className="flex items-center gap-2 font-semibold">
        <CalendarClock className="size-5 shrink-0" aria-hidden="true" /> Tu salida fue reprogramada
      </h2>
      <dl className="grid gap-2 text-sm sm:grid-cols-2">
        {r.fechaAnterior && (
          <div>
            <dt className="text-xs opacity-80">Fecha anterior</dt>
            <dd className="font-medium line-through decoration-1">{fechaHora(r.fechaAnterior)} h</dd>
          </div>
        )}
        <div>
          <dt className="text-xs opacity-80">Nueva fecha</dt>
          <dd className="font-semibold">
            {formatearRangoFechas(s.fechaInicio, s.fechaFinalizacion)} · {hora(s.horaInicio)} h
          </dd>
        </div>
        {r.motivo && (
          <div className="sm:col-span-2">
            <dt className="text-xs opacity-80">Motivo</dt>
            <dd>{r.motivo}</dd>
          </div>
        )}
        {r.nuevoRecorrido && (
          <div className="sm:col-span-2">
            <dt className="text-xs opacity-80">Recorrido</dt>
            <dd>{r.nuevoRecorrido}</dd>
          </div>
        )}
      </dl>
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      {rechazando ? (
        <div role="alertdialog" aria-labelledby="t-rechazo" className="flex flex-col gap-3 rounded-xl bg-card px-4 py-3 text-foreground">
          <p id="t-rechazo" className="text-sm">
            Si rechazás la nueva fecha, la reserva se cancela y te devolvemos el total abonado
            {Number(reserva.importeAbonado) > 0 ? ` (${pesos(reserva.importeAbonado)})` : ''}. ¿Querés continuar?
          </p>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => setRechazando(false)} disabled={enviando}>Volver</Button>
            <Button type="button" variant="destructive" onClick={() => responder(false)} disabled={enviando}>
              {enviando && <Loader2 className="animate-spin" aria-hidden="true" />} Sí, rechazar
            </Button>
          </div>
        </div>
      ) : (
        <div className="flex flex-col gap-2 sm:flex-row">
          <Button type="button" onClick={() => responder(true)} disabled={enviando}>
            {enviando && <Loader2 className="animate-spin" aria-hidden="true" />} Acepto la nueva fecha
          </Button>
          <Button type="button" variant="outline" onClick={() => setRechazando(true)} disabled={enviando}>
            Rechazar y pedir devolución
          </Button>
        </div>
      )}
    </section>
  );
}

/**
 * CU-19 Consultar y gestionar reserva: resultado del pago, detalle, pagar el saldo y cancelar (CU-23).
 * CU-24 Cargar certificado médico: solo se ofrece si la excursión lo exige; se deshabilita junto con el pago
 * cuando faltan 72 horas o menos para la salida.
 */
export default function ReservaDetallePagina() {
  const { id } = useParams();
  const { state } = useLocation();
  const resultado = state?.resultado;
  const [reserva, setReserva] = useState(resultado?.reserva ?? null);
  const [error, setError] = useState('');
  const [cancelando, setCancelando] = useState(false);
  const [aviso, setAviso] = useState('');
  const [avisoReprogramacion, setAvisoReprogramacion] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    obtenerReserva(id)
        .then(setReserva)
        .catch((e) => setError(e.message));
  }, [id]);

  if (error) return <Aviso titulo="No pudimos cargar la reserva" mensaje={error} volverA="/mis-reservas" textoVolver="Mis reservas" />;
  if (!reserva) return <Cargando texto="Cargando reserva…" />;

  const confirmarCancelacion = async (motivo) => {
    await cancelarReserva(reserva.idReserva, motivo);
    navigate('/mis-reservas', {
      state: { aviso: `Cancelaste la reserva #${reserva.idReserva}. Te enviamos el detalle por correo.` },
    });
  };
  const mantenerReserva = () => {
    setCancelando(false);
    setAviso('No se realizaron cambios en la reserva.');
  };

  const aprobado = resultado?.estadoPago === 'APROBADO';
  const s = reserva.salida;

  const dentroDelPlazo = faltanMasDe72Horas(s);
  const tieneSaldoPendiente =
      ['PENDIENTE_PAGO', 'SENADA'].includes(reserva.estadoReserva) && Number(reserva.saldoPendiente) > 0;
  // El certificado solo se ofrece si la excursión lo exige y la reserva ya está vigente
  const ofreceCertificado =
      s.requiereCertificado === true && ['SENADA', 'CONFIRMADA'].includes(reserva.estadoReserva);
  // Hay acciones que se ofrecerían, pero el plazo ya cerró
  const plazoCerrado = !dentroDelPlazo && (tieneSaldoPendiente || ofreceCertificado);

  return (
      <div className="mx-auto flex max-w-[720px] flex-col gap-5">
        <Volver to="/mis-reservas">Mis reservas</Volver>

        {avisoReprogramacion && (
            <p role="status" className="rounded-xl border border-primary/30 bg-accent px-4 py-3 text-sm text-foreground">{avisoReprogramacion}</p>
        )}

        {reserva.reprogramacion && (
            <AvisoReprogramacion
                reserva={reserva}
                onRespondida={(r, acepta) => {
                  setReserva(r);
                  setAvisoReprogramacion(
                      acepta
                          ? 'Aceptaste la nueva fecha. Tu reserva sigue vigente.'
                          : 'Rechazaste la nueva fecha: la reserva pasó a Pendiente de devolución y te avisaremos por correo cuando se procese.',
                  );
                }}
            />
        )}

        {resultado && (
            <div
                className={cn(
                    'flex flex-col items-center gap-2 rounded-2xl border px-6 py-7 text-center',
                    aprobado ? 'border-primary/30 bg-accent' : 'border-destructive/30 bg-destructive/5',
                )}
                role="status"
            >
              {aprobado ? (
                  <CheckCircle2 className="size-10 text-primary" aria-hidden="true" />
              ) : (
                  <XCircle className="size-10 text-destructive" aria-hidden="true" />
              )}
              <h1 className="font-serif text-2xl font-semibold text-foreground">
                {aprobado ? 'Pago aprobado' : resultado.estadoPago === 'RECHAZADO' ? 'Pago rechazado' : 'No se realizó el pago'}
              </h1>
              <p className="max-w-md text-sm text-muted-foreground">{resultado.mensaje}</p>
              {resultado.nroTransaccion && (
                  <p className="text-xs text-muted-foreground">N.º de transacción: {resultado.nroTransaccion}</p>
              )}
            </div>
        )}

        <section className="rounded-2xl border border-border bg-card px-5 py-6 sm:px-8">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm text-muted-foreground">Mi reserva #{reserva.idReserva}</p>
              <h2 className="font-serif text-2xl font-semibold text-foreground">{s.excursion}</h2>
            </div>
            <EstadoReservaBadge estado={reserva.estadoReserva} />
          </div>

          {reserva.estadoReserva === 'PENDIENTE_PAGO' && (
              <div className="mb-4 flex flex-col gap-3">
                <ContadorRetencion
                    segundos={reserva.segundosParaVencer}
                    onVencido={() => obtenerReserva(id).then(setReserva).catch(() => {})}
                />
              </div>
          )}

          {reserva.motivoCancelacion && (
              <p className="mb-4 rounded-xl bg-destructive/10 px-4 py-3 text-sm text-destructive">
                {reserva.motivoCancelacion}
              </p>
          )}

          <dl className="divide-y divide-border text-sm">
            <Fila etiqueta="Salida">
              {formatearRangoFechas(s.fechaInicio, s.fechaFinalizacion)} · {hora(s.horaInicio)} h
            </Fila>
            <Fila etiqueta="Participantes">{reserva.cantidadParticipantes}</Fila>
            {reserva.modalidadAlojamiento && (
                <Fila etiqueta="Alojamiento">
                  {ALOJAMIENTO[reserva.modalidadAlojamiento]}
                  {reserva.refugios.length > 0 && ` (${reserva.refugios.map((r) => r.nombre).join(', ')})`}
                </Fila>
            )}
            <Fila etiqueta="Monto total" fuerte>
              {pesos(reserva.montoTotal)}
            </Fila>
            {Number(reserva.importeAbonado) > 0 && <Fila etiqueta="Importe abonado">{pesos(reserva.importeAbonado)}</Fila>}
            {['PENDIENTE_PAGO', 'SENADA', 'CONFIRMADA'].includes(reserva.estadoReserva) && (
                <Fila etiqueta="Saldo pendiente" fuerte>
                  {pesos(reserva.saldoPendiente)}
                </Fila>
            )}
          </dl>

          {/* Acciones: pagar, cargar certificado y cancelar */}
          {(tieneSaldoPendiente || ofreceCertificado || reserva.cancelable) && (
              <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex flex-wrap gap-3">
                  {/* Pago: habilitado solo con más de 72 hs para la salida */}
                  {tieneSaldoPendiente &&
                      (dentroDelPlazo ? (
                          <Link
                              to={`/reservas/${reserva.idReserva}/pago`}
                              className={cn(buttonVariants(), 'h-11 px-6 font-semibold')}
                          >
                            <CreditCard aria-hidden="true" className="mr-2 size-4" />
                            {reserva.estadoReserva === 'SENADA' ? 'Pagar el saldo restante' : 'Continuar con el pago'}
                          </Link>
                      ) : (
                          <Button disabled variant="secondary" className="h-11 px-6 font-semibold">
                            <CreditCard aria-hidden="true" className="mr-2 size-4" />
                            {reserva.estadoReserva === 'SENADA' ? 'Pagar el saldo restante' : 'Continuar con el pago'}
                          </Button>
                      ))}

                  {/* Certificado médico: aparece solo si la excursión lo exige; mismo plazo que el pago */}
                  {ofreceCertificado &&
                      (dentroDelPlazo ? (
                          <Link
                              to={`/reservas/${reserva.idReserva}/certificados`}
                              className={cn(
                                  buttonVariants({ variant: 'outline' }),
                                  'h-11 border-primary/40 text-primary hover:bg-primary/10',
                              )}
                          >
                            <FileText aria-hidden="true" className="mr-2 size-4" />
                            Cargar certificado médico
                          </Link>
                      ) : (
                          <Button disabled variant="outline" className="h-11 border-primary/40 text-muted-foreground">
                            <FileText aria-hidden="true" className="mr-2 size-4" />
                            Cargar certificado médico
                          </Button>
                      ))}
                </div>

                {reserva.cancelable && (
                    <Button
                        type="button"
                        variant="outline"
                        className="h-11 border-destructive/40 px-5 text-destructive hover:bg-destructive/10 hover:text-destructive"
                        onClick={() => {
                          setAviso('');
                          setCancelando(true);
                        }}
                    >
                      Cancelar reserva
                    </Button>
                )}
              </div>
          )}

          {plazoCerrado && (
              <p className="mt-4 flex items-start gap-2 text-sm text-muted-foreground" role="status">
                <Info className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                Con 72 horas o menos para la salida ya no se puede pagar ni cargar certificados médicos.
              </p>
          )}
          {aviso && (
              <p className="mt-4 flex items-center gap-2 text-sm text-muted-foreground" role="status">
                <Info className="size-4 shrink-0" aria-hidden="true" /> {aviso}
              </p>
          )}
          {reserva.motivoNoPuedePagar && reserva.estadoReserva === 'SENADA' && (
              <p className="mt-4 text-sm text-muted-foreground">{reserva.motivoNoPuedePagar}</p>
          )}
        </section>

        <section className="rounded-2xl border border-border bg-card px-5 py-6 sm:px-8">
          <h3 className="mb-3 font-semibold text-foreground">Participantes</h3>
          <ul className="divide-y divide-border text-sm">
            {reserva.participantes.map((p) => (
                <li key={p.dni} className="flex flex-wrap justify-between gap-2 py-2">
              <span className="font-medium text-foreground">
                {p.apellido}, {p.nombre}
              </span>
                  <span className="text-muted-foreground">
                DNI {p.dni} · {p.correoElectronico}
              </span>
                </li>
            ))}
          </ul>

          {reserva.equipamiento.length > 0 && (
              <>
                <h3 className="mt-6 mb-3 font-semibold text-foreground">Equipamiento en préstamo</h3>
                <ul className="divide-y divide-border text-sm">
                  {reserva.equipamiento.map((e) => (
                      <li key={e.nombre} className="flex justify-between gap-2 py-2">
                  <span>
                    {e.nombre} × {e.cantidad}
                  </span>
                        <span className="text-muted-foreground">{ESTADO_EQUIPO[e.estado]}</span>
                      </li>
                  ))}
                </ul>
              </>
          )}

          {reserva.pagos.length > 0 && (
              <>
                <h3 className="mt-6 mb-3 font-semibold text-foreground">Pagos</h3>
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[480px] text-left text-sm">
                    <thead className="text-xs text-muted-foreground">
                    <tr>
                      <th className="py-2 font-medium">Fecha</th>
                      <th className="py-2 font-medium">Modo</th>
                      <th className="py-2 font-medium">Importe</th>
                      <th className="py-2 font-medium">Estado</th>
                      <th className="py-2 font-medium">Transacción</th>
                    </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                    {reserva.pagos.map((p) => (
                        <tr key={p.idPago}>
                          <td className="py-2">{fechaHora(p.fechaPago)}</td>
                          <td className="py-2">{MODO_PAGO[p.modoPago]}</td>
                          <td className="py-2">{pesos(p.importe)}</td>
                          <td className={cn('py-2', p.estadoPago === 'RECHAZADO' && 'text-destructive')}>
                            {ESTADO_PAGO[p.estadoPago]}
                          </td>
                          <td className="py-2 font-mono text-xs">{p.nroTransaccion}</td>
                        </tr>
                    ))}
                    </tbody>
                  </table>
                </div>
              </>
          )}
        </section>

        <Link to="/" className={cn(buttonVariants({ variant: 'outline' }), 'h-11 w-fit px-5')}>
          Ver más excursiones
        </Link>
        <CancelarReservaDialog
            abierto={cancelando}
            importeAbonado={reserva.importeAbonado}
            onConfirmar={confirmarCancelacion}
            onMantener={mantenerReserva}
        />
      </div>
  );
}