import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { CheckCircle2, CreditCard, XCircle } from 'lucide-react';
import { buttonVariants } from '@/components/ui/button';
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
import { obtenerReserva } from '@/api/reservas';
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

/**
 * Resultado del pago y consulta de la reserva (versión inicial de CU-19).
 * Si se llega desde el pago, location.state.resultado trae el mensaje de CU-26.
 */
export default function ReservaDetallePagina() {
  const { id } = useParams();
  const { state } = useLocation();
  const resultado = state?.resultado;
  const [reserva, setReserva] = useState(resultado?.reserva ?? null);
  const [error, setError] = useState('');

  useEffect(() => {
    obtenerReserva(id)
      .then(setReserva)
      .catch((e) => setError(e.message));
  }, [id]);

  if (error) return <Aviso titulo="No pudimos cargar la reserva" mensaje={error} volverA="/mis-reservas" textoVolver="Mis reservas" />;
  if (!reserva) return <Cargando texto="Cargando reserva…" />;

  const aprobado = resultado?.estadoPago === 'APROBADO';
  const s = reserva.salida;

  return (
    <div className="mx-auto flex max-w-[720px] flex-col gap-5">
      <Volver to="/mis-reservas">Mis reservas</Volver>

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
          {['PENDIENTE_PAGO', 'SENADA', 'CONFIRMADA'].includes(reserva.estadoReserva) && (
            <Fila etiqueta="Saldo pendiente" fuerte>
              {pesos(reserva.saldoPendiente)}
            </Fila>
          )}
        </dl>

        {reserva.modosPagoDisponibles.length > 0 && (
          <Link
            to={`/reservas/${reserva.idReserva}/pago`}
            className={cn(buttonVariants(), 'mt-5 h-11 w-full px-6 font-semibold sm:w-auto')}
          >
            <CreditCard aria-hidden="true" />
            {reserva.estadoReserva === 'SENADA' ? 'Pagar el saldo restante' : 'Continuar con el pago'}
          </Link>
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
    </div>
  );
}
