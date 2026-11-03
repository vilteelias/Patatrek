import { useEffect, useState } from 'react';
import { CheckCircle2, Clock, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { ejecutarCancelacionAutomatica } from '@/api/reservas';

/**
 * CU-30 Cancelar reservas automáticamente. Lo dispara el temporizador todos los días;
 * el administrador puede ejecutarlo ahora para no esperar a la ejecución programada.
 * onTerminar(huboCambios): al cerrar, para volver a cargar el listado.
 */
export function CancelacionAutomaticaDialog({ abierto, onTerminar }) {
  const [etapa, setEtapa] = useState('confirmar'); // confirmar | ejecutando | resultado
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (abierto) {
      setEtapa('confirmar');
      setResultado(null);
      setError('');
    }
  }, [abierto]);

  const ejecutar = async () => {
    setEtapa('ejecutando');
    setError('');
    try {
      setResultado(await ejecutarCancelacionAutomatica());
      setEtapa('resultado');
    } catch (e) {
      setError(e.message);
      setEtapa('confirmar');
    }
  };

  const cerrar = () => {
    if (etapa === 'ejecutando') return;
    onTerminar(Boolean(resultado?.canceladas?.length));
  };

  return (
    <Dialog open={abierto} onOpenChange={(abrir) => !abrir && cerrar()}>
      <DialogContent className="max-h-[90dvh] overflow-y-auto rounded-xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="font-serif text-xl font-semibold">Cancelación automática de reservas</DialogTitle>
          <DialogDescription>
            Lo hace el sistema todos los días a las 03:00. Podés ejecutarlo ahora para no esperar.
          </DialogDescription>
        </DialogHeader>

        {etapa !== 'resultado' ? (
          <div className="flex flex-col gap-3 text-sm text-foreground">
            <p>
              Revisa las salidas que empiezan dentro de las <strong>próximas 72 horas</strong> y cancela las reservas que:
            </p>
            <ul className="list-disc space-y-1 pl-5">
              <li>
                siguen <strong>Señadas</strong> porque no se pagó el saldo restante, o
              </li>
              <li>
                son de una excursión que exige certificado médico y <strong>ningún participante</strong> tiene uno
                cargado o aprobado.
              </li>
            </ul>
            <p className="text-muted-foreground">
              Se liberan los cupos, las plazas de refugio y el equipamiento, no se devuelve lo abonado y se le avisa por
              correo a cada cliente titular.
            </p>
            {error && (
              <p className="text-destructive" role="alert">
                {error}
              </p>
            )}
          </div>
        ) : (
          <div className="flex flex-col gap-3 text-sm" aria-live="polite">
            <p className="flex items-start gap-2 rounded-lg bg-accent px-3 py-2.5 text-accent-foreground">
              <CheckCircle2 className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              <span>{resultado.mensaje}</span>
            </p>
            <p className="text-xs text-muted-foreground">
              Se revisaron {resultado.salidasRevisadas === 1 ? '1 salida' : `${resultado.salidasRevisadas} salidas`} y{' '}
              {resultado.reservasRevisadas === 1 ? '1 reserva' : `${resultado.reservasRevisadas} reservas`}.
            </p>
            {resultado.canceladas.length > 0 && (
              <ul className="flex flex-col divide-y divide-border rounded-lg border border-border">
                {resultado.canceladas.map((c) => (
                  <li key={c.idReserva} className="flex flex-col gap-0.5 px-3 py-2.5">
                    <span className="font-semibold text-foreground">
                      Reserva #{c.idReserva} · {c.cliente}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {c.excursion} (salida #{c.idSalida})
                    </span>
                    <span className="text-xs text-foreground">{c.motivo}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <DialogFooter>
          {etapa === 'resultado' ? (
            <Button onClick={cerrar}>Listo</Button>
          ) : (
            <>
              <Button variant="outline" onClick={cerrar} disabled={etapa === 'ejecutando'}>
                Cerrar
              </Button>
              <Button onClick={ejecutar} disabled={etapa === 'ejecutando'}>
                {etapa === 'ejecutando' ? (
                  <Loader2 className="animate-spin" aria-hidden="true" />
                ) : (
                  <Clock aria-hidden="true" />
                )}
                Ejecutar ahora
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
