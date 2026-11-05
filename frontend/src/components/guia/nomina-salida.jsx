import { AlertCircle, ArrowLeft, ClipboardCheck, Flag, Home, Mountain, Route, Users } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { cn } from '@/lib/utils';
import { EstadoSalidaBadge } from './estado-salida-badge';
import { formatearDni, formatearRangoFechas } from './formato';
const CARD_CLASE = 'gap-0 rounded-xl py-0 shadow-none ring-border';
function nombreCompleto(p) {
  return `${p.apellido}, ${p.nombre}`;
}
function textoAsistencia(p) {
  if (p.estadoAsistencia === 'PRESENTE') return 'Presente';
  if (p.estadoAsistencia === 'AUSENTE') return 'Ausente';
  return 'Sin registrar';
}
function ResultadoBadge({ participante }) {
  if (participante.estadoAsistencia === 'AUSENTE') {
    return <Badge className="bg-secondary text-secondary-foreground font-semibold">Ausente</Badge>;
  }
  if (participante.estadoRequisitos === 'APTO') {
    return <Badge className="bg-accent text-accent-foreground font-semibold">Apto</Badge>;
  }
  if (participante.estadoRequisitos === 'NO_APTO') {
    return <Badge className="bg-destructive/10 text-destructive font-semibold">No apto</Badge>;
  }
  return <span className="text-muted-foreground">—</span>;
}
function resumenResultados(participantes) {
  let aptos = 0;
  let noAptos = 0;
  let ausentes = 0;
  for (const p of participantes) {
    if (p.estadoAsistencia === 'AUSENTE') ausentes++;
    else if (p.estadoRequisitos === 'APTO') aptos++;
    else if (p.estadoRequisitos === 'NO_APTO') noAptos++;
  }
  const partes = [
    `${aptos} ${aptos === 1 ? 'apto' : 'aptos'}`,
    `${noAptos} ${noAptos === 1 ? 'no apto' : 'no aptos'}`,
    `${ausentes} ${ausentes === 1 ? 'ausente' : 'ausentes'}`,
  ];
  return partes.join(' · ');
}
export function NominaSalida({
  salida,
  participantes,
  cargando,
  error,
  onVolver,
  onControlAsistencia,
  onMarcarFinalizacion,
}) {
  const { estadoSalida } = salida;
  const controlRealizado = estadoSalida === 'EN_CURSO' || estadoSalida === 'FINALIZADA';
  const finalizada = estadoSalida === 'FINALIZADA';
  const hayParticipantes = participantes.length > 0;
  const hayResultados = controlRealizado && participantes.some((p) => p.estadoAsistencia !== null);
  const puedeControlar = (estadoSalida === 'DISPONIBLE' || estadoSalida === 'COMPLETA') && hayParticipantes;
  const motivoControl = puedeControlar
    ? null
    : controlRealizado
      ? 'El control de asistencia ya se realizó.'
      : !hayParticipantes
        ? 'No hay participantes para controlar.'
        : 'La salida no admite control de asistencia en este estado.';
  const esResponsable = salida.rolGuia === 'RESPONSABLE';
  const puedeFinalizar = esResponsable && estadoSalida === 'EN_CURSO';
  const motivoFinalizar = puedeFinalizar
    ? null
    : !esResponsable
      ? 'Solo el guía responsable puede finalizar la salida.'
      : 'Disponible cuando la salida esté En curso.';
  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <a
          href="/guia/salidas"
          onClick={(e) => {
            e.preventDefault();
            onVolver();
          }}
          className="inline-flex w-fit items-center gap-1.5 rounded-sm text-sm text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          Mis salidas
        </a>
        <h1 className="font-serif text-3xl font-semibold text-balance text-foreground sm:text-4xl">
          {salida.nombreExcursion}
        </h1>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
          <p className="text-sm text-muted-foreground sm:text-base">
            {formatearRangoFechas(salida.fechaInicio, salida.fechaFinalizacion)}
            <span aria-hidden="true"> · </span>
            Nómina de participantes de la salida
          </p>
          <EstadoSalidaBadge estado={estadoSalida} />
        </div>
      </header>

      {(salida.recorrido || salida.descripcionTecnica || salida.refugios?.length > 0) && (
        <Card className={cn(CARD_CLASE, 'gap-3 px-5 py-4 sm:px-6')}>
          {salida.recorrido && (
            <div className="flex gap-3">
              <Route className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
              <div className="flex flex-col gap-0.5">
                <h2 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Recorrido</h2>
                <p className="text-sm text-foreground">{salida.recorrido}</p>
              </div>
            </div>
          )}
          {salida.descripcionTecnica && (
            <div className="flex gap-3">
              <Mountain className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
              <div className="flex flex-col gap-0.5">
                <h2 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Información técnica</h2>
                <p className="text-sm text-foreground">{salida.descripcionTecnica}</p>
              </div>
            </div>
          )}
          {salida.refugios?.length > 0 && (
            <div className="flex gap-3">
              <Home className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
              <div className="flex flex-col gap-0.5">
                <h2 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Refugios</h2>
                <p className="text-sm text-foreground">{salida.refugios.join(' · ')}</p>
              </div>
            </div>
          )}
        </Card>
      )}

      {error ? (
        <Alert variant="destructive" className="rounded-xl bg-card">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>No pudimos cargar la nómina</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : cargando ? (
        <NominaSkeleton />
      ) : !hayParticipantes ? (
        <Card className={cn(CARD_CLASE, 'items-center gap-3 px-6 py-12 text-center')}>
          <span className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
            <Users className="size-5" aria-hidden="true" />
          </span>
          <p className="text-sm text-muted-foreground">Esta salida todavía no tiene participantes.</p>
        </Card>
      ) : (
        <section aria-labelledby="titulo-nomina" className="flex flex-col gap-3">
          <h2 id="titulo-nomina" className="sr-only">
            Participantes
          </h2>
          <p className="text-sm text-muted-foreground">
            <span className="font-semibold text-foreground">
              {participantes.length} {participantes.length === 1 ? 'participante' : 'participantes'}
            </span>
            {hayResultados && (
              <>
                <span aria-hidden="true"> · </span>
                {resumenResultados(participantes)}
              </>
            )}
          </p>

          <Card className={cn(CARD_CLASE, 'hidden overflow-hidden md:flex')}>
            <Table>
              <TableHeader className="bg-muted/60">
                <TableRow className="hover:bg-transparent">
                  {[
                    'Nombre y apellido',
                    'DNI',
                    'Teléfono',
                    'Email',
                    'Reserva',
                    ...(controlRealizado ? ['Asistencia', 'Resultado'] : []),
                  ].map((col, i) => (
                    <TableHead
                      key={col}
                      className={cn(
                        'h-12 text-xs font-semibold tracking-wide text-muted-foreground uppercase',
                        i === 0 && 'pl-6',
                      )}
                    >
                      {col}
                    </TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {participantes.map((p) => (
                  <TableRow key={p.idParticipante}>
                    <TableCell className="py-4 pl-6 font-medium text-foreground">{nombreCompleto(p)}</TableCell>
                    <TableCell className="text-muted-foreground tabular-nums">{formatearDni(p.dni)}</TableCell>
                    <TableCell className="text-muted-foreground tabular-nums">{p.telefono}</TableCell>
                    <TableCell className="text-muted-foreground">
                      <a href={`mailto:${p.correoElectronico}`} className="hover:text-foreground hover:underline">
                        {p.correoElectronico}
                      </a>
                    </TableCell>
                    <TableCell className="text-muted-foreground tabular-nums">#{p.idReserva}</TableCell>
                    {controlRealizado && (
                      <>
                        <TableCell
                          className={cn(p.estadoAsistencia === null ? 'text-muted-foreground' : 'text-foreground')}
                        >
                          {textoAsistencia(p)}
                        </TableCell>
                        <TableCell className="pr-6">
                          <ResultadoBadge participante={p} />
                        </TableCell>
                      </>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>

          <ul className="flex flex-col gap-3 md:hidden">
            {participantes.map((p) => (
              <li key={p.idParticipante}>
                <Card className={cn(CARD_CLASE, 'px-5 py-4')}>
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="font-semibold text-foreground">{nombreCompleto(p)}</p>
                      <p className="text-xs text-muted-foreground">Reserva #{p.idReserva}</p>
                    </div>
                    {controlRealizado && <ResultadoBadge participante={p} />}
                  </div>
                  <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                    <div>
                      <dt className="text-xs text-muted-foreground">DNI</dt>
                      <dd className="text-foreground tabular-nums">{formatearDni(p.dni)}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-muted-foreground">Teléfono</dt>
                      <dd className="text-foreground tabular-nums">{p.telefono}</dd>
                    </div>
                    <div className="col-span-2 min-w-0">
                      <dt className="text-xs text-muted-foreground">Email</dt>
                      <dd className="break-all text-foreground">
                        <a href={`mailto:${p.correoElectronico}`} className="hover:underline">
                          {p.correoElectronico}
                        </a>
                      </dd>
                    </div>
                    {controlRealizado && (
                      <div>
                        <dt className="text-xs text-muted-foreground">Asistencia</dt>
                        <dd className="text-foreground">{textoAsistencia(p)}</dd>
                      </div>
                    )}
                  </dl>
                </Card>
              </li>
            ))}
          </ul>
        </section>
      )}

      {!error &&
        !cargando &&
        (finalizada ? (
          <Card className={cn(CARD_CLASE, 'gap-2 px-5 py-5 sm:px-6')}>
            <h2 className="text-sm font-semibold text-foreground">Observaciones del guía</h2>
            <p
              className={cn(
                'text-sm leading-relaxed text-pretty',
                salida.observaciones ? 'text-foreground' : 'text-muted-foreground',
              )}
            >
              {salida.observaciones || 'Sin observaciones.'}
            </p>
          </Card>
        ) : (
          <div className="flex flex-col gap-4 sm:flex-row sm:justify-end sm:gap-3">
            <AccionConMotivo id="motivo-control" motivo={motivoControl}>
              <Button
                variant="outline"
                disabled={!puedeControlar}
                onClick={onControlAsistencia}
                aria-describedby={motivoControl ? 'motivo-control' : undefined}
                className="h-11 w-full bg-card px-6 text-sm font-semibold sm:w-auto"
              >
                <ClipboardCheck aria-hidden="true" />
                Control de asistencia
              </Button>
            </AccionConMotivo>
            <AccionConMotivo id="motivo-finalizar" motivo={motivoFinalizar}>
              <Button
                disabled={!puedeFinalizar}
                onClick={onMarcarFinalizacion}
                aria-describedby={motivoFinalizar ? 'motivo-finalizar' : undefined}
                className="h-11 w-full px-6 text-sm font-semibold sm:w-auto"
              >
                <Flag aria-hidden="true" />
                Marcar finalización de salida
              </Button>
            </AccionConMotivo>
          </div>
        ))}
    </div>
  );
}
function AccionConMotivo({ id, motivo, children }) {
  return (
    <div className="flex flex-col gap-1.5 sm:max-w-64 sm:items-end">
      {children}
      {motivo && (
        <p id={id} className="text-xs text-muted-foreground sm:text-right">
          {motivo}
        </p>
      )}
    </div>
  );
}
function NominaSkeleton() {
  return (
    <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando participantes">
      <div className="h-4 w-32 animate-pulse rounded-md bg-muted" />
      <Card className={cn(CARD_CLASE, 'px-5 py-2 sm:px-6')}>
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="flex items-center gap-6 border-b border-border py-4 last:border-0">
            <div className="h-4 w-40 animate-pulse rounded-md bg-muted" />
            <div className="hidden h-4 w-20 animate-pulse rounded-md bg-muted sm:block" />
            <div className="hidden h-4 w-24 animate-pulse rounded-md bg-muted md:block" />
            <div className="hidden h-4 w-44 animate-pulse rounded-md bg-muted md:block" />
          </div>
        ))}
      </Card>
    </div>
  );
}
