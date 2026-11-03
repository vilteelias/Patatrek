import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AlertCircle } from 'lucide-react';
import { Alert, AlertTitle } from '@/components/ui/alert';
import { FormularioPago } from '@/components/reserva/formulario-pago';
import { ContadorRetencion } from '@/components/reserva/contador-retencion';
import { formatearRangoFechas, pesos } from '@/components/reserva/formato';
import { obtenerReserva, pagarReserva, simularVencimiento } from '@/api/reservas';
import { Aviso, Cargando } from './Estados';

/**
 * CU-26 Realizar pago invocado desde la consulta de la reserva (prototipo pág. 12):
 * pago restante de una reserva señada, o retomar el pago de una reserva pendiente.
 */
export default function PagoPagina() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [reserva, setReserva] = useState(null);
  const [error, setError] = useState('');
  const [errorPago, setErrorPago] = useState('');

  const cargar = () =>
    obtenerReserva(id)
      .then(setReserva)
      .catch((e) => setError(e.message));

  useEffect(() => {
    cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  if (error) return <Aviso titulo="No pudimos cargar la reserva" mensaje={error} volverA="/mis-reservas" textoVolver="Mis reservas" />;
  if (!reserva) return <Cargando texto="Cargando reserva…" />;

  const pagar = (modo) => {
    setErrorPago('');
    return pagarReserva(reserva.idReserva, modo);
  };
  const terminarPago = (resultado) => navigate(`/reservas/${reserva.idReserva}`, { state: { resultado } });

  const s = reserva.salida;
  const puedePagar = reserva.modosPagoDisponibles.length > 0;

  return (
    <div className="mx-auto flex max-w-[620px] flex-col gap-5">
      <nav className="text-sm text-muted-foreground" aria-label="Ruta">
        <Link to={`/reservas/${reserva.idReserva}`} className="hover:text-foreground">
          Mi reserva #{reserva.idReserva}
        </Link>{' '}
        / Realizar pago
      </nav>
      <h1 className="font-serif text-3xl font-semibold text-foreground">Realizar pago</h1>

      <dl className="rounded-2xl border border-border bg-card px-5 py-4 text-sm sm:px-6">
        <div className="flex justify-between gap-4 py-1">
          <dt className="text-muted-foreground">Excursión</dt>
          <dd className="text-right font-semibold">{s.excursion}</dd>
        </div>
        <div className="flex justify-between gap-4 py-1">
          <dt className="text-muted-foreground">Salida</dt>
          <dd className="text-right font-semibold">{formatearRangoFechas(s.fechaInicio, s.fechaFinalizacion)}</dd>
        </div>
        <div className="flex justify-between gap-4 py-1">
          <dt className="text-muted-foreground">Participantes</dt>
          <dd className="font-semibold">{reserva.cantidadParticipantes}</dd>
        </div>
        <div className="mt-2 flex justify-between gap-4 border-t border-border pt-3">
          <dt className="font-semibold text-foreground">Importe total de la reserva</dt>
          <dd className="font-semibold">{pesos(reserva.montoTotal)}</dd>
        </div>
        {reserva.estadoReserva === 'SENADA' && (
          <div className="flex justify-between gap-4 py-1">
            <dt className="text-muted-foreground">Saldo pendiente</dt>
            <dd className="font-semibold">{pesos(reserva.saldoPendiente)}</dd>
          </div>
        )}
      </dl>

      {errorPago && (
        <Alert variant="destructive" className="rounded-xl">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>{errorPago}</AlertTitle>
        </Alert>
      )}

      {puedePagar ? (
        <section className="rounded-2xl border border-border bg-card px-5 py-6 sm:px-8">
          {reserva.estadoReserva === 'PENDIENTE_PAGO' && (
            <ContadorRetencion segundos={reserva.segundosParaVencer} onVencido={cargar} className="mb-5" />
          )}
          <FormularioPago
            reserva={reserva}
            onConfirmar={pagar}
            onTerminar={terminarPago}
            onCancelado={cargar}
            onAtras={() => navigate(`/reservas/${reserva.idReserva}`)}
            textoAtras="Cancelar"
            onSimularVencimiento={
              reserva.estadoReserva === 'PENDIENTE_PAGO'
                ? () => simularVencimiento(reserva.idReserva).then(setReserva).catch((e) => setErrorPago(e.message))
                : undefined
            }
          />
        </section>
      ) : (
        <Alert className="rounded-xl">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>{reserva.motivoNoPuedePagar ?? 'Esta reserva no admite pagos.'}</AlertTitle>
        </Alert>
      )}
    </div>
  );
}
