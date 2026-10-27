import { useEffect, useState } from 'react';
import { CheckCircle2, CreditCard, Landmark, Loader2, Lock, Wallet, XCircle } from 'lucide-react';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog';
import { cn } from '@/lib/utils';
import { MODO_PAGO, pesos } from './formato';

/*
 * Ventana emergente que SIMULA una pasarela de pagos externa, al estilo de un checkout de billetera virtual.
 * Sugerencia de las profesoras: la pasarela se muestra en una ventanita aparte de PataTrek.
 *
 * CORRECCIÓN DE LA CÁTEDRA: acá tampoco se escriben datos de tarjeta. El cliente elige un medio
 * que ya tiene guardado "en la pasarela" (son datos ficticios de la simulación) y PataTrek
 * solo recibe el resultado: aprobado o rechazado, con su número de transacción.
 */

// Colores propios de la pasarela, para que se note que es otro sistema (no PataTrek).
const AZUL = 'bg-[#1f6fb2]';
const AZUL_HOVER = 'hover:bg-[#185a92]';

const MEDIOS = [
  { id: 'credito', icono: CreditCard, titulo: 'Tarjeta de crédito', detalle: 'Visa terminada en 4242' },
  { id: 'debito', icono: Landmark, titulo: 'Tarjeta de débito', detalle: 'Mastercard terminada en 5100' },
  { id: 'cuenta', icono: Wallet, titulo: 'Dinero disponible en la cuenta', detalle: 'Saldo de la billetera' },
];

/**
 * abierto: si se muestra.
 * concepto / importe / modo: lo que se cobra.
 * onPagar(aprobar) → promesa con el resultado de CU-26 ({ estadoPago, nroTransaccion, mensaje, ... }).
 * onCancelar(): el usuario cierra sin pagar (flujo 4c: no se genera ningún pago).
 * onTerminar(resultado): el usuario vuelve a PataTrek después de ver el resultado.
 */
export function PasarelaPagoModal({ abierto, concepto, importe, modo, onPagar, onCancelar, onTerminar }) {
  const [etapa, setEtapa] = useState('elegir'); // elegir | procesando | resultado
  const [medio, setMedio] = useState('credito');
  const [aprobar, setAprobar] = useState(true);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (abierto) {
      setEtapa('elegir');
      setMedio('credito');
      setAprobar(true);
      setResultado(null);
      setError('');
    }
  }, [abierto]);

  const pagar = async () => {
    setEtapa('procesando');
    try {
      // Pausa para que en la demo se vea que la pasarela "procesa" el cobro.
      await new Promise((r) => setTimeout(r, 1500));
      setResultado(await onPagar(aprobar));
    } catch (e) {
      setError(e.message);
    }
    setEtapa('resultado');
  };

  const cerrar = () => {
    if (etapa === 'procesando') return;
    if (etapa === 'resultado' && resultado) onTerminar(resultado);
    else onCancelar();
  };

  const medioElegido = MEDIOS.find((m) => m.id === medio);
  const aprobado = resultado?.estadoPago === 'APROBADO';

  return (
    <Dialog open={abierto} onOpenChange={(abrir) => !abrir && cerrar()}>
      <DialogContent showCloseButton={false} className="gap-0 overflow-hidden p-0 sm:max-w-[420px]">
        {/* Encabezado de la pasarela */}
        <div className={cn('flex items-center justify-between px-5 py-3 text-white', AZUL)}>
          <span className="flex items-center gap-2 font-semibold">
            <Wallet className="size-5" aria-hidden="true" /> Pasarela de pagos
          </span>
          <span className="rounded-full bg-white/20 px-2.5 py-0.5 text-[11px] font-semibold tracking-wide uppercase">
            Simulación
          </span>
        </div>

        <div className="flex flex-col gap-4 bg-[#f5f7fa] px-5 py-5">
          {/* Lo que se paga */}
          <div className="rounded-xl bg-white px-4 py-3 shadow-sm">
            <DialogDescription className="text-xs text-[#5f6b7a]">{concepto}</DialogDescription>
            <DialogTitle className="mt-1 text-3xl font-semibold text-[#1b2733]">{pesos(importe)}</DialogTitle>
            <p className="mt-0.5 text-xs text-[#5f6b7a]">{MODO_PAGO[modo]} · Cobra: PataTrek</p>
          </div>

          {etapa === 'elegir' && (
            <>
              <div>
                <p className="mb-2 text-sm font-semibold text-[#1b2733]">¿Cómo querés pagar?</p>
                <div className="flex flex-col gap-2" role="radiogroup" aria-label="Medio de pago">
                  {MEDIOS.map(({ id, icono: Icono, titulo, detalle }) => {
                    const activo = medio === id;
                    return (
                      <button
                        key={id}
                        type="button"
                        role="radio"
                        aria-checked={activo}
                        onClick={() => setMedio(id)}
                        className={cn(
                          'flex items-center gap-3 rounded-xl border bg-white px-4 py-3 text-left transition-colors',
                          activo ? 'border-[#1f6fb2] ring-2 ring-[#1f6fb2]/25' : 'border-[#dde3ea] hover:border-[#b9c4d0]',
                        )}
                      >
                        <span
                          className={cn(
                            'flex size-9 shrink-0 items-center justify-center rounded-full',
                            activo ? 'bg-[#1f6fb2] text-white' : 'bg-[#eef2f6] text-[#1f6fb2]',
                          )}
                        >
                          <Icono className="size-4" aria-hidden="true" />
                        </span>
                        <span className="flex flex-col">
                          <span className="text-sm font-semibold text-[#1b2733]">{titulo}</span>
                          <span className="text-xs text-[#5f6b7a]">{detalle}</span>
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="rounded-xl border border-dashed border-[#b9c4d0] bg-white px-4 py-3">
                <p className="mb-2 text-[11px] font-semibold tracking-wide text-[#5f6b7a] uppercase">
                  Simulación para la demo · respuesta de la pasarela
                </p>
                <div className="flex gap-2">
                  {[
                    { valor: true, texto: 'Aprobar pago' },
                    { valor: false, texto: 'Rechazar pago' },
                  ].map(({ valor, texto }) => (
                    <button
                      key={texto}
                      type="button"
                      aria-pressed={aprobar === valor}
                      onClick={() => setAprobar(valor)}
                      className={cn(
                        'flex-1 rounded-lg border px-3 py-1.5 text-xs font-semibold transition-colors',
                        aprobar === valor
                          ? valor
                            ? 'border-[#00a650] bg-[#00a650] text-white'
                            : 'border-[#d93a3a] bg-[#d93a3a] text-white'
                          : 'border-[#dde3ea] bg-white text-[#5f6b7a] hover:bg-[#eef2f6]',
                      )}
                    >
                      {texto}
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex flex-col gap-2">
                <button
                  type="button"
                  onClick={pagar}
                  className={cn('h-11 rounded-lg text-sm font-semibold text-white transition-colors', AZUL, AZUL_HOVER)}
                >
                  Pagar {pesos(importe)}
                </button>
                <button
                  type="button"
                  onClick={onCancelar}
                  className="h-10 rounded-lg text-sm font-semibold text-[#1f6fb2] hover:bg-[#e8f0f8]"
                >
                  Cancelar y volver a PataTrek
                </button>
              </div>
            </>
          )}

          {etapa === 'procesando' && (
            <div className="flex flex-col items-center gap-3 py-8 text-center" role="status">
              <Loader2 className="size-10 animate-spin text-[#1f6fb2]" aria-hidden="true" />
              <p className="font-semibold text-[#1b2733]">Estamos procesando tu pago…</p>
              <p className="text-xs text-[#5f6b7a]">
                {medioElegido.titulo} · {medioElegido.detalle}
              </p>
            </div>
          )}

          {etapa === 'resultado' && (
            <div className="flex flex-col items-center gap-2 py-4 text-center" role="status">
              {aprobado ? (
                <CheckCircle2 className="size-14 text-[#00a650]" aria-hidden="true" />
              ) : (
                <XCircle className="size-14 text-[#d93a3a]" aria-hidden="true" />
              )}
              <p className="text-lg font-semibold text-[#1b2733]">
                {error
                  ? 'No pudimos procesar el pago'
                  : aprobado
                    ? '¡Listo! Se acreditó tu pago'
                    : resultado?.estadoPago === 'RECHAZADO'
                      ? 'Tu pago fue rechazado'
                      : 'No se realizó el pago'}
              </p>
              <p className="max-w-[320px] text-sm text-[#5f6b7a]">
                {error ||
                  (aprobado
                    ? `Pagaste ${pesos(importe)} con ${medioElegido.titulo.toLowerCase()}.`
                    : resultado?.estadoPago === 'RECHAZADO'
                      ? 'La entidad emisora rechazó el pago (fondos insuficientes). No se realizó ningún cobro.'
                      : resultado?.mensaje)}
              </p>
              {resultado?.nroTransaccion && (
                <p className="text-xs text-[#5f6b7a]">Operación N.º {resultado.nroTransaccion}</p>
              )}
              <button
                type="button"
                onClick={cerrar}
                className={cn(
                  'mt-3 h-11 w-full rounded-lg text-sm font-semibold text-white transition-colors',
                  AZUL,
                  AZUL_HOVER,
                )}
              >
                Volver a PataTrek
              </button>
            </div>
          )}
        </div>

        <p className="flex items-center justify-center gap-1.5 border-t border-[#dde3ea] bg-white px-5 py-2.5 text-center text-[11px] text-[#5f6b7a]">
          <Lock className="size-3 shrink-0" aria-hidden="true" />
          Pasarela externa simulada. PataTrek no recibe ni guarda datos de tarjetas.
        </p>
      </DialogContent>
    </Dialog>
  );
}
