import { useState } from 'react';
import { AlertCircle, AlertTriangle, CheckCircle2, ExternalLink, FileText, Loader2, XCircle } from 'lucide-react';
import { Button, buttonVariants } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { aprobarCertificado, rechazarCertificado, urlArchivoCertificado } from '@/api/certificados';
import { formatearFechaCorta } from '@/utilidades/fechas';
import { cn } from '@/lib/utils';

const TITULO = 'font-serif text-xl font-semibold text-foreground';

/**
 * Pasos: revisando → (rechazando) → procesando → exito.
 * Montar con key={idCertificado}. onResuelto(id, estado) actualiza la tabla de fondo.
 */
export function ModalRevision({ certificado: c, onResuelto, onCerrar }) {
  const [paso, setPaso] = useState('revisando');
  const [motivo, setMotivo] = useState('');
  const [error, setError] = useState('');
  const [resultado, setResultado] = useState(null);
  const procesando = paso === 'procesando';
  const nombre = `${c.apellido}, ${c.nombre}`;
  const esSegundoEnvio = c.intentosEnvio >= 2;

  async function resolver(accion, volverA) {
    setPaso('procesando');
    setError('');
    try {
      const estado = await accion();
      setResultado(estado);
      onResuelto(c.idCertificadoMedico, estado);
      setPaso('exito');
    } catch (e) {
      setError(e.message);
      setPaso(volverA);
    }
  }

  const aprobar = () =>
    resolver(async () => {
      await aprobarCertificado(c.idCertificadoMedico);
      return 'APROBADO';
    }, 'revisando');

  const rechazar = () =>
    resolver(async () => {
      const r = await rechazarCertificado(c.idCertificadoMedico, motivo.trim());
      // Si el backend no devuelve el estado, el segundo envío rechazado es definitivo (CU-25)
      return r?.estadoCertificado ?? (esSegundoEnvio ? 'RECHAZADO_DEFINITIVO' : 'RECHAZADO');
    }, 'rechazando');

  const mensajeError = error && (
    <p
      role="alert"
      className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive"
    >
      <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      {error}
    </p>
  );

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && !procesando && onCerrar()}>
      <DialogContent showCloseButton={false} className="gap-5 rounded-xl p-6 sm:max-w-[500px]">
        {paso === 'revisando' && (
          <>
            <DialogHeader>
              <DialogTitle className={TITULO}>Revisar certificado</DialogTitle>
              <DialogDescription>
                {nombre} · Reserva #{c.idReserva}
              </DialogDescription>
            </DialogHeader>

            {esSegundoEnvio && (
              <p className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/10 px-3.5 py-2.5 text-sm text-destructive">
                <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                <span>
                  Este es el segundo envío. Si lo rechazás, quedará <strong>Rechazado Definitivo</strong> y se liberará
                  el cupo del participante.
                </span>
              </p>
            )}

            <div className="flex flex-col items-center gap-2 rounded-lg border border-border bg-muted/60 p-5 text-center">
              <FileText className="size-7 text-muted-foreground" aria-hidden="true" />
              <span className="max-w-full truncate text-sm text-foreground">{c.nombreArchivo ?? 'Certificado médico'}</span>
              <span className="text-xs text-muted-foreground">Cargado el {formatearFechaCorta(c.fechaSubida)}</span>
              <a
                href={urlArchivoCertificado(c.idCertificadoMedico)}
                target="_blank"
                rel="noreferrer"
                className={cn(buttonVariants({ variant: 'outline', size: 'lg' }), 'mt-1')}
              >
                <ExternalLink aria-hidden="true" />
                Abrir archivo
                <span className="sr-only">(se abre en una pestaña nueva)</span>
              </a>
            </div>

            {mensajeError}

            <DialogFooter>
              <Button variant="ghost" size="lg" onClick={onCerrar} className="text-muted-foreground">
                Cancelar
              </Button>
              <Button
                variant="outline"
                size="lg"
                onClick={() => setPaso('rechazando')}
                className="border-destructive/30 text-destructive hover:bg-destructive/10"
              >
                Rechazar
              </Button>
              <Button size="lg" onClick={aprobar}>
                Aprobar
              </Button>
            </DialogFooter>
          </>
        )}

        {paso === 'rechazando' && (
          <>
            <DialogHeader>
              <DialogTitle className={TITULO}>Motivo del rechazo</DialogTitle>
              <DialogDescription>Se incluirá en el correo que recibe el cliente.</DialogDescription>
            </DialogHeader>
            <Textarea
              rows={3}
              maxLength={500}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Ej. La constancia no incluye fecha de vencimiento."
              aria-label="Motivo del rechazo"
            />
            {mensajeError}
            <DialogFooter>
              <Button variant="outline" size="lg" onClick={() => setPaso('revisando')}>
                Volver
              </Button>
              <Button
                size="lg"
                disabled={!motivo.trim()}
                onClick={rechazar}
                className="bg-destructive text-white hover:bg-destructive/90"
              >
                Confirmar rechazo
              </Button>
            </DialogFooter>
          </>
        )}

        {procesando && (
          <div className="flex flex-col items-center gap-4 py-6" role="status" aria-live="polite">
            <Loader2 className="size-7 animate-spin text-primary" aria-hidden="true" />
            <DialogTitle className="text-sm font-normal text-muted-foreground">
              Notificando la decisión al cliente por correo…
            </DialogTitle>
          </div>
        )}

        {paso === 'exito' && (
          <div className="flex flex-col items-center gap-3 pt-2 text-center">
            <span
              className={cn(
                'flex size-12 items-center justify-center rounded-full',
                resultado === 'APROBADO' ? 'bg-accent text-accent-foreground' : 'bg-destructive/10 text-destructive',
              )}
            >
              {resultado === 'APROBADO' ? (
                <CheckCircle2 className="size-6" aria-hidden="true" />
              ) : (
                <XCircle className="size-6" aria-hidden="true" />
              )}
            </span>
            <DialogTitle className="text-base font-semibold text-foreground">
              {resultado === 'APROBADO' && 'Certificado aprobado'}
              {resultado === 'RECHAZADO' && 'Certificado rechazado'}
              {resultado === 'RECHAZADO_DEFINITIVO' && 'Rechazado definitivo'}
            </DialogTitle>
            <DialogDescription className="max-w-[380px]">
              {resultado === 'APROBADO' &&
                `${nombre} queda habilitado/a para la actividad, sujeto al control de asistencia el día de la salida.`}
              {resultado === 'RECHAZADO' &&
                `${nombre} fue notificado/a y podrá volver a cargar el certificado antes del plazo límite.`}
              {resultado === 'RECHAZADO_DEFINITIVO' &&
                `Se liberó el cupo y la plaza de ${nombre}. No podrá volver a cargar documentación para esta reserva.`}
            </DialogDescription>
            <Button size="lg" onClick={onCerrar} className="mt-2 px-6">
              Listo
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
