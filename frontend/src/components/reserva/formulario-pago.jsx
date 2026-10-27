import { useEffect, useState } from 'react';
import { ExternalLink, ShieldCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { MODO_PAGO, pesos } from './formato';
import { PasarelaPagoModal } from './pasarela-pago-modal';

function importeDe(reserva, modo) {
  if (modo === 'SENA') return reserva.importeSena;
  if (modo === 'TOTAL') return reserva.montoTotal;
  return reserva.saldoPendiente;
}

/**
 * CU-26 Realizar pago (prototipo pág. 11-12).
 * El cliente elige el modo de pago en PataTrek y la pasarela se abre en una ventana emergente.
 *
 * CORRECCIÓN DE LA CÁTEDRA: PataTrek NO pide número de tarjeta, vencimiento, CVV ni titular.
 *
 * onConfirmar(modo, aprobar) → promesa con el resultado del pago.
 * onTerminar(resultado) → el cliente volvió de la pasarela con un resultado.
 * onCancelado() → el cliente cerró la pasarela sin pagar o hubo un error (para refrescar la reserva).
 */
export function FormularioPago({
  reserva,
  onConfirmar,
  onTerminar,
  onCancelado,
  onAtras,
  onSimularVencimiento,
  textoAtras = 'Atrás',
}) {
  const modos = reserva.modosPagoDisponibles;
  const [modo, setModo] = useState(modos[0]);
  const [pasarelaAbierta, setPasarelaAbierta] = useState(false);

  useEffect(() => {
    if (!modos.includes(modo)) setModo(modos[0]);
  }, [modos, modo]);

  // Si la reserva deja de admitir pagos (por ejemplo, venció) se cierra la pasarela.
  useEffect(() => {
    if (modos.length === 0) setPasarelaAbierta(false);
  }, [modos.length]);

  if (!modo) return null;
  const importe = importeDe(reserva, modo);

  return (
    <div className="flex flex-col gap-5">
      <div>
        <p className="mb-2 text-sm font-medium text-foreground">Modo de pago</p>
        <div className="grid gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Modo de pago">
          {modos.map((m) => {
            const activo = m === modo;
            return (
              <button
                key={m}
                type="button"
                role="radio"
                aria-checked={activo}
                onClick={() => setModo(m)}
                className={cn(
                  'flex flex-col items-start rounded-xl border px-4 py-3 text-left transition-colors',
                  activo
                    ? 'border-primary bg-accent ring-2 ring-primary/30'
                    : 'border-border bg-card hover:border-input hover:bg-muted',
                )}
              >
                <span className="font-semibold text-foreground">{MODO_PAGO[m]}</span>
                <span className="text-sm text-muted-foreground">{pesos(importeDe(reserva, m))}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex items-start gap-3 rounded-xl border border-border bg-muted px-4 py-3 text-sm">
        <ShieldCheck className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" />
        <p className="text-muted-foreground">
          El pago se realiza en la <strong className="text-foreground">pasarela de pagos</strong>, que se abre en una
          ventana aparte. PataTrek no recibe ni guarda los datos de tu tarjeta: solo registra el resultado del pago y su
          número de transacción.
        </p>
      </div>

      {onSimularVencimiento && (
        <div className="rounded-xl border border-dashed border-input px-4 py-3">
          <p className="mb-1 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Simulación para la demo
          </p>
          <button
            type="button"
            onClick={onSimularVencimiento}
            className="text-sm text-muted-foreground underline underline-offset-3 hover:text-foreground"
          >
            Simular vencimiento del plazo de retención
          </button>
        </div>
      )}

      <div className="flex items-center justify-between rounded-xl bg-accent px-4 py-3">
        <span className="font-medium text-accent-foreground">Monto a pagar ahora</span>
        <span className="font-serif text-2xl font-semibold text-accent-foreground">{pesos(importe)}</span>
      </div>

      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
        {onAtras ? (
          <Button type="button" variant="outline" className="h-11 px-5" onClick={onAtras}>
            {textoAtras}
          </Button>
        ) : (
          <span />
        )}
        <Button type="button" className="h-11 px-6 font-semibold" onClick={() => setPasarelaAbierta(true)}>
          <ExternalLink aria-hidden="true" />
          Ir a la pasarela de pagos
        </Button>
      </div>

      <PasarelaPagoModal
        abierto={pasarelaAbierta}
        concepto={`PataTrek · Reserva #${reserva.idReserva} — ${reserva.salida.excursion}`}
        importe={importe}
        modo={modo}
        onPagar={(aprobar) => onConfirmar(modo, aprobar)}
        onCancelar={() => {
          setPasarelaAbierta(false);
          onCancelado?.();
        }}
        onTerminar={(resultado) => {
          setPasarelaAbierta(false);
          onTerminar(resultado);
        }}
      />
    </div>
  );
}
