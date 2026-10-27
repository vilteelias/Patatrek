import { useState } from 'react';
import { AlertCircle, ArrowLeft, Flag, Loader2, Mail } from 'lucide-react';
import { FormField } from '@/components/form-field';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { cn } from '@/lib/utils';
const MAX_OBSERVACIONES = 1000;
const BADGE_CLASE = 'h-7 shrink-0 px-3 text-xs font-semibold sm:text-sm';
function nombreCompleto(p) {
  return `${p.apellido}, ${p.nombre}`;
}
function ResultadoBadge({ participante }) {
  if (participante.estadoAsistencia === 'AUSENTE') {
    return <Badge className={cn(BADGE_CLASE, 'bg-secondary text-secondary-foreground')}>Ausente</Badge>;
  }
  if (participante.estadoRequisitos === 'APTO') {
    return <Badge className={cn(BADGE_CLASE, 'bg-accent text-accent-foreground')}>Apto</Badge>;
  }
  if (participante.estadoRequisitos === 'NO_APTO') {
    return <Badge className={cn(BADGE_CLASE, 'bg-destructive/10 text-destructive')}>No apto</Badge>;
  }
  return <Badge className={cn(BADGE_CLASE, 'bg-muted text-muted-foreground')}>Sin registrar</Badge>;
}
export function FinalizarSalida({ salida, participantes, enviando, errorGeneral, onVolver, onConfirmar }) {
  const [observaciones, setObservaciones] = useState('');
  const [confirmando, setConfirmando] = useState(false);
  const notificados = participantes.filter((p) => p.estadoAsistencia === 'PRESENTE' && p.estadoRequisitos === 'APTO');
  return (
    <div className="mx-auto flex w-full max-w-[620px] flex-col gap-6">
      <header className="flex flex-col gap-2">
        <a
          href="#"
          onClick={(e) => {
            e.preventDefault();
            onVolver();
          }}
          className="inline-flex w-fit items-center gap-1.5 rounded-sm text-sm text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          {salida.nombreExcursion}
        </a>
        <h1 className="font-serif text-3xl font-semibold text-balance text-foreground sm:text-4xl">
          Marcar finalización de salida
        </h1>
      </header>

      <Card className="gap-4 rounded-xl px-5 py-6 shadow-none ring-border sm:px-6">
        <h2 className="text-base font-semibold text-foreground">Resultado del control de asistencia</h2>
        {participantes.length === 0 ? (
          <p className="text-sm text-muted-foreground">La salida no tiene participantes.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {participantes.map((p) => (
              <li
                key={p.idParticipante}
                className="flex min-h-9 items-center justify-between gap-4 text-base text-foreground"
              >
                <span className="min-w-0 truncate">{nombreCompleto(p)}</span>
                <ResultadoBadge participante={p} />
              </li>
            ))}
          </ul>
        )}
        <div className="flex gap-3 rounded-lg bg-secondary px-4 py-3 text-sm leading-relaxed text-secondary-foreground">
          <Mail className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <p className="text-pretty">
            {notificados.length > 0
              ? `Recibirán la solicitud de valoración al finalizar: ${notificados.map(nombreCompleto).join('; ')}.`
              : 'Ningún participante quedó como Presente y Apto: la salida se finalizará sin enviar solicitudes de valoración.'}
          </p>
        </div>
      </Card>

      <Card className="gap-6 rounded-xl px-5 py-6 shadow-none ring-border sm:px-6">
        <FormField
          id="observaciones"
          label="Observaciones"
          optional
          hint={`${observaciones.length}/${MAX_OBSERVACIONES}`}
        >
          <Textarea
            id="observaciones"
            value={observaciones}
            maxLength={MAX_OBSERVACIONES}
            disabled={enviando}
            onChange={(e) => setObservaciones(e.target.value.slice(0, MAX_OBSERVACIONES))}
            placeholder="Ej. La salida se completó sin incidentes. Se acortó el último tramo por clima."
            aria-describedby="observaciones-hint"
            className="min-h-32 bg-card text-base"
          />
        </FormField>

        {errorGeneral && (
          <Alert variant="destructive" className="rounded-xl bg-card">
            <AlertCircle aria-hidden="true" />
            <AlertTitle>No pudimos finalizar la salida</AlertTitle>
            <AlertDescription>{errorGeneral}</AlertDescription>
          </Alert>
        )}

        <div className="flex justify-end">
          <Button
            type="button"
            onClick={() => setConfirmando(true)}
            disabled={enviando}
            className="h-14 w-full px-8 text-base font-semibold sm:w-auto"
          >
            {enviando ? <Loader2 className="animate-spin" aria-hidden="true" /> : <Flag aria-hidden="true" />}
            {enviando ? 'Finalizando…' : 'Marcar como finalizada'}
          </Button>
        </div>
      </Card>

      <Dialog open={confirmando} onOpenChange={(abierto) => !enviando && setConfirmando(abierto)}>
        <DialogContent showCloseButton={false} className="rounded-xl sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="font-serif text-xl font-semibold">
              ¿Confirmás la finalización de la salida?
            </DialogTitle>
            <DialogDescription className="text-base leading-relaxed text-foreground">
              Se registrará la fecha y hora actual y la salida pasará a estado Finalizada. Esta acción no se puede
              deshacer.
            </DialogDescription>
          </DialogHeader>
          {errorGeneral && (
            <p role="alert" className="text-sm text-destructive">
              {errorGeneral}
            </p>
          )}
          <DialogFooter className="gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={enviando}
              onClick={() => setConfirmando(false)}
              className="h-12 bg-card px-5 text-base"
            >
              Cancelar
            </Button>
            <Button
              type="button"
              disabled={enviando}
              onClick={() => onConfirmar(observaciones.trim())}
              className="h-12 px-5 text-base font-semibold"
            >
              {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
              {enviando ? 'Finalizando…' : 'Confirmar finalización'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
