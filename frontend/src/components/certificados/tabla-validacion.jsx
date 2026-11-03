import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { formatearDni, formatearRangoFechas } from '@/components/guia/formato';
import { formatearFechaCorta } from '@/utilidades/fechas';
import { cn } from '@/lib/utils';

const RESULTADOS = {
  APROBADO: { etiqueta: 'Aprobado', clase: 'bg-accent text-accent-foreground' },
  RECHAZADO: { etiqueta: 'Rechazado', clase: 'bg-secondary text-secondary-foreground' },
  RECHAZADO_DEFINITIVO: { etiqueta: 'Rechazado definitivo', clase: 'bg-destructive/10 text-destructive' },
};

/** resueltos: { [idCertificado]: 'APROBADO' | 'RECHAZADO' | 'RECHAZADO_DEFINITIVO' } (revisados en esta sesión) */
export function TablaValidacion({ certificados, resueltos, onRevisar }) {
  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      <Table>
        <TableHeader>
          <TableRow className="bg-muted/60 hover:bg-muted/60">
            {['Participante', 'Reserva', 'Excursión / Salida', 'Cargado', 'Envío'].map((t) => (
              <TableHead key={t} className="h-11 px-4 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                {t}
              </TableHead>
            ))}
            <TableHead className="h-11 px-4 text-right text-xs font-semibold tracking-wide text-muted-foreground uppercase">
              Acción
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {certificados.map((c) => {
            const resultado = RESULTADOS[resueltos[c.idCertificadoMedico]];
            const segundoEnvio = c.intentosEnvio >= 2;
            return (
              <TableRow key={c.idCertificadoMedico} className={cn(resultado && 'bg-muted/50')}>
                <TableCell className="px-4 py-3.5 whitespace-normal">
                  <span className="block font-semibold text-foreground">
                    {c.apellido}, {c.nombre}
                  </span>
                  <span className="text-xs text-muted-foreground">DNI {formatearDni(c.dni)}</span>
                </TableCell>
                <TableCell className="px-4 py-3.5">#{c.idReserva}</TableCell>
                <TableCell className="px-4 py-3.5 whitespace-normal">
                  <span className="block text-foreground">{c.nombreExcursion}</span>
                  <span className="text-xs text-muted-foreground">
                    {formatearRangoFechas(c.fechaInicioSalida, c.fechaFinalizacionSalida)}
                  </span>
                </TableCell>
                <TableCell className="px-4 py-3.5 text-muted-foreground">{formatearFechaCorta(c.fechaSubida)}</TableCell>
                <TableCell
                  className={cn('px-4 py-3.5 text-xs', segundoEnvio ? 'font-semibold text-destructive' : 'text-muted-foreground')}
                >
                  {segundoEnvio ? '2do envío' : '1er envío'}
                </TableCell>
                <TableCell className="px-4 py-3.5 text-right">
                  {resultado ? (
                    <span className={cn('inline-block rounded-md px-3 py-1.5 text-xs font-semibold whitespace-nowrap', resultado.clase)}>
                      {resultado.etiqueta}
                    </span>
                  ) : (
                    <Button
                      size="lg"
                      onClick={() => onRevisar(c)}
                      aria-label={`Revisar certificado de ${c.apellido}, ${c.nombre}`}
                    >
                      Revisar
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
