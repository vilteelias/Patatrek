import { AlertCircle, Clock } from 'lucide-react';
import { formatearFechaHora } from '@/utilidades/fechas';

/** propio: true cuando el texto habla de "tu certificado" (participante) en lugar de "certificados" (cliente). */
export function AvisoPlazo({ horarioLimite, vencido, propio = false }) {
  if (vencido) {
    return (
      <div
        role="alert"
        className="flex items-start gap-2.5 rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive"
      >
        <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <p>
          {propio
            ? 'El plazo para cargar o reemplazar tu certificado venció. Ya no es posible subir documentación.'
            : 'El plazo para cargar o reemplazar certificados venció. Ya no es posible subir documentación para esta reserva.'}
        </p>
      </div>
    );
  }

  return (
    <div className="flex items-start gap-2.5 rounded-lg bg-accent px-4 py-3 text-sm text-accent-foreground">
      <Clock className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      <p>
        Podés cargar o reemplazar {propio ? 'tu certificado' : 'certificados'} hasta el{' '}
        <strong>{formatearFechaHora(horarioLimite)}</strong> (72 horas antes de la salida).
      </p>
    </div>
  );
}
