import { Button } from '@/components/ui/button';
import { MAX_INTENTOS } from '@/api/certificados';
import { formatearFechaCorta } from '@/utilidades/fechas';
import { cn } from '@/lib/utils';
import { ESTADOS, iniciales } from './estado-certificado.js';

/** Una fila con el participante, el estado de su certificado y el botón para subirlo. */
export function FilaCertificado({ certificado: c, plazoVencido, grande = false, onCargar }) {
  const estado = ESTADOS[c.estadoCertificado];
  const nombreCompleto = `${c.apellido}, ${c.nombre}`;
  const habilitado = estado.puedeCargar && !plazoVencido;
  // Tras el primer rechazo queda un único intento: se avisa con una insignia
  const ultimoIntento = c.estadoCertificado === 'RECHAZADO' && (c.intentosEnvio ?? 0) + 1 >= MAX_INTENTOS;
  const nota = estado.puedeCargar && plazoVencido ? 'Plazo vencido' : estado.nota;

  return (
    <div className={cn('flex items-center gap-4', grande ? 'p-6' : 'px-4 py-4 sm:px-6')}>
      <span
        aria-hidden="true"
        className={cn(
          'flex shrink-0 items-center justify-center rounded-full bg-secondary font-semibold text-secondary-foreground',
          grande ? 'size-11 text-sm' : 'size-10 text-xs',
        )}
      >
        {iniciales(c.apellido, c.nombre)}
      </span>

      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className={cn('truncate font-semibold text-foreground', grande ? 'text-base' : 'text-sm')}>
          {nombreCompleto}
          {c.esTitular && ' (vos)'}
        </span>
        <span className="flex flex-wrap items-center gap-2">
          <span className={cn('text-sm font-medium', estado.clase)}>
            {estado.etiqueta}
            {c.estadoCertificado === 'CARGADO' && c.fechaSubida && ` · ${formatearFechaCorta(c.fechaSubida)}`}
          </span>
          {ultimoIntento && (
            <span className="rounded-full bg-destructive/10 px-2 py-0.5 text-xs font-semibold text-destructive">
              Último intento ({MAX_INTENTOS} de {MAX_INTENTOS})
            </span>
          )}
        </span>
        {c.motivoRechazo && <span className="text-xs text-destructive">Motivo: {c.motivoRechazo}</span>}
      </div>

      <div className="shrink-0 text-right">
        {habilitado ? (
          <Button size="lg" onClick={() => onCargar(c)} aria-label={`Cargar certificado de ${nombreCompleto}`}>
            {c.estadoCertificado === 'RECHAZADO' ? 'Volver a cargar' : 'Subir certificado'}
          </Button>
        ) : (
          nota && <span className="text-sm text-muted-foreground">{nota}</span>
        )}
      </div>
    </div>
  );
}
