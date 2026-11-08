import { useCallback, useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { AlertCircle, CheckCircle2, CreditCard, FileText, Route, ShieldCheck, XCircle } from 'lucide-react';
import { AccesoLayout } from '@/components/layouts/acceso-layout';
import { Button, buttonVariants } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { EstadoReservaBadge } from '@/components/reserva/estado-reserva-badge';
import { FormularioPago } from '@/components/reserva/formulario-pago';
import { formatearRangoFechas, hora, pesos } from '@/components/reserva/formato';
import { obtenerReservaParticipante, pagarSaldoComoParticipante } from '@/api/participante';
import { cn } from '@/lib/utils';

function Fila({ etiqueta, children }) {
  return (
    <div className="flex justify-between gap-4 py-1.5 text-sm">
      <dt className="text-muted-foreground">{etiqueta}</dt>
      <dd className="text-right font-medium text-foreground">{children}</dd>
    </div>
  );
}

/**
 * CU-19 Consultar y gestionar reserva, vista del Participante (paso 3): estado, fecha, saldo pendiente y
 * participantes. Si faltan más de 3 días, habilita cargar su certificado (CU-24, 3a) y pagar el saldo (CU-26, 3b).
 */
export default function ParticipanteReservaPagina() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const dni = params.get('dni') ?? '';
  const [reserva, setReserva] = useState(null);
  const [error, setError] = useState('');
  const [pagando, setPagando] = useState(false);
  const [resultado, setResultado] = useState(null);

  const cargar = useCallback(
    () =>
      obtenerReservaParticipante(id, dni)
        .then((r) => {
          setReserva(r);
          setError('');
        })
        .catch((e) => setError(e.message)),
    [id, dni],
  );

  useEffect(() => {
    if (dni) cargar();
  }, [cargar, dni]);

  const derecha = (
    <span className="flex items-center gap-2 text-xs text-muted-foreground">
      <ShieldCheck className="size-4 shrink-0" aria-hidden="true" />
      <span className="truncate">
        Acceso por N.º de reserva + DNI
        {reserva && ` · ${reserva.participante.apellido}, ${reserva.participante.nombre}`}
      </span>
    </span>
  );

  if (!dni || (error && !reserva)) {
    return (
      <AccesoLayout derecha={derecha}>
        <div className="flex flex-col items-start gap-4">
          <h1 className="font-serif text-3xl font-semibold text-foreground">No pudimos abrir la reserva</h1>
          <p className="text-muted-foreground">{error || 'Falta tu DNI para consultar la reserva.'}</p>
          <Link to="/participante" className={cn(buttonVariants(), 'h-10 px-5')}>
            Ingresar el N.º de reserva y mi DNI
          </Link>
        </div>
      </AccesoLayout>
    );
  }

  if (!reserva) {
    return (
      <AccesoLayout derecha={derecha}>
        <div className="flex flex-col gap-3" aria-busy="true">
          <span className="sr-only">Cargando la reserva…</span>
          <Skeleton className="h-10 w-2/3" />
          <Skeleton className="h-40 w-full" />
        </div>
      </AccesoLayout>
    );
  }

  const s = reserva.salida;
  const vigente = ['SENADA', 'CONFIRMADA'].includes(reserva.estadoReserva);
  const puedePagar = reserva.modosPagoDisponibles.includes('SALDO');
  const aprobado = resultado?.estadoPago === 'APROBADO';

  return (
    <AccesoLayout derecha={derecha}>
      <section aria-labelledby="titulo-reserva" className="flex flex-col gap-5">
        <header className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-sm text-muted-foreground">Reserva #{reserva.idReserva} · Titular: {reserva.titular}</p>
            <h1 id="titulo-reserva" className="mt-1 font-serif text-3xl font-semibold text-foreground">
              {s.excursion}
            </h1>
          </div>
          <EstadoReservaBadge estado={reserva.estadoReserva} />
        </header>

        {resultado && (
          <div
            role="status"
            className={cn(
              'flex items-start gap-3 rounded-xl border px-4 py-3 text-sm',
              aprobado ? 'border-primary/30 bg-accent text-foreground' : 'border-destructive/30 bg-destructive/10 text-destructive',
            )}
          >
            {aprobado ? (
              <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
            ) : (
              <XCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            )}
            <div>
              <p className="font-semibold">{aprobado ? 'Pago aprobado' : 'Pago rechazado'}</p>
              <p>{resultado.mensaje}</p>
              {resultado.nroTransaccion && <p className="text-xs opacity-80">N.º de transacción: {resultado.nroTransaccion}</p>}
            </div>
          </div>
        )}

        <dl className="rounded-2xl border border-border bg-card px-5 py-4 sm:px-6">
          <Fila etiqueta="Salida">
            {formatearRangoFechas(s.fechaInicio, s.fechaFinalizacion)} · {hora(s.horaInicio)} h
          </Fila>
          {s.recorrido && (
            <Fila etiqueta="Recorrido">
              <span className="inline-flex items-start gap-1.5">
                <Route className="mt-0.5 size-3.5 shrink-0 text-primary" aria-hidden="true" />
                {s.recorrido}
              </span>
            </Fila>
          )}
          <Fila etiqueta="Participantes">{reserva.participantes.join(' · ')}</Fila>
          <div className="mt-2 border-t border-border pt-2">
            <Fila etiqueta="Importe total">{pesos(reserva.montoTotal)}</Fila>
            <Fila etiqueta="Saldo pendiente">{pesos(reserva.saldoPendiente)}</Fila>
          </div>
          {reserva.motivoCancelacion && <Fila etiqueta="Motivo de cancelación">{reserva.motivoCancelacion}</Fila>}
        </dl>

        {vigente && s.requiereCertificado && (
          <div className="flex flex-col gap-3 rounded-2xl border border-border bg-card px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
            <div className="flex items-start gap-3">
              <FileText className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" />
              <div>
                <h2 className="font-semibold text-foreground">Certificado médico</h2>
                <p className="text-sm text-muted-foreground">Esta excursión lo exige. Podés cargar el tuyo.</p>
              </div>
            </div>
            <Link
              to={`/reservas/${reserva.idReserva}/mi-certificado?dni=${reserva.participante.dni}`}
              className={cn(buttonVariants({ variant: 'outline' }), 'h-10 px-4')}
            >
              Ver mi certificado
            </Link>
          </div>
        )}

        {puedePagar && (
          <div className="flex flex-col gap-4 rounded-2xl border border-border bg-card px-5 py-5 sm:px-6">
            <div className="flex items-start gap-3">
              <CreditCard className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" />
              <div>
                <h2 className="font-semibold text-foreground">Saldo pendiente</h2>
                <p className="text-sm text-muted-foreground">
                  La reserva está señada. Podés pagar el saldo restante ({pesos(reserva.saldoPendiente)}) hasta 72 horas
                  antes de la salida; si no se paga, la reserva se cancela sin devolución de la seña.
                </p>
              </div>
            </div>
            {pagando ? (
              <FormularioPago
                reserva={{ ...reserva, idReserva: reserva.idReserva }}
                onConfirmar={() => pagarSaldoComoParticipante(reserva.idReserva, dni)}
                onTerminar={(r) => {
                  setResultado(r);
                  if (r?.reserva) setReserva(r.reserva);
                  setPagando(false);
                }}
                onCancelado={cargar}
                onAtras={() => setPagando(false)}
                textoAtras="Cancelar"
              />
            ) : (
              <Button type="button" className="h-10 self-start px-5" onClick={() => { setResultado(null); setPagando(true); }}>
                Pagar saldo restante
              </Button>
            )}
          </div>
        )}

        {!puedePagar && reserva.estadoReserva === 'SENADA' && reserva.motivoNoPuedePagar && (
          <p className="flex items-start gap-2 rounded-xl border border-border bg-muted px-4 py-3 text-sm text-muted-foreground">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {reserva.motivoNoPuedePagar}
          </p>
        )}
      </section>
    </AccesoLayout>
  );
}
