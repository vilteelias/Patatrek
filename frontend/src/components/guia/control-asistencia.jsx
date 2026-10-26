import { useState } from 'react';
import { AlertCircle, ArrowLeft, Check, Loader2, Package, Play, X } from 'lucide-react';
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
import { Switch } from '@/components/ui/switch';
import { cn } from '@/lib/utils';
import { formatearDni } from './formato';
const MOTIVO_EQUIPAMIENTO = 'Motivo: Falta el equipamiento obligatorio mínimo.';
const MOTIVO_CERTIFICADO = 'Motivo: El certificado médico no está en estado Aprobado.';
const CERTIFICADOS = {
  APROBADO: { etiqueta: 'Aprobado', clase: 'bg-accent text-accent-foreground' },
  CARGADO: { etiqueta: 'Cargado (sin revisar)', clase: 'bg-secondary text-secondary-foreground' },
  RECHAZADO: { etiqueta: 'Rechazado', clase: 'bg-destructive/10 text-destructive' },
  RECHAZADO_DEFINITIVO: {
    etiqueta: 'Rechazado definitivo',
    clase: 'bg-destructive/10 text-destructive',
  },
  PENDIENTE: { etiqueta: 'Sin cargar', clase: 'bg-muted text-muted-foreground' },
  SIN_CARGAR: { etiqueta: 'Sin cargar', clase: 'bg-muted text-muted-foreground' },
};
const BADGE_CLASE = 'h-7 shrink-0 px-3 text-xs font-semibold sm:text-sm';
function nombreCompleto(p) {
  return `${p.apellido}, ${p.nombre}`;
}
function calcularResultado(salida, participante, estado) {
  if (estado.asistencia === null) return { tipo: null, motivos: [] };
  if (estado.asistencia === 'AUSENTE') return { tipo: 'AUSENTE', motivos: [] };
  const motivos = [];
  const faltaEquipamiento = salida.equipamientoObligatorio.some(
    (item) => !estado.equipamiento.includes(item.idEquipamiento),
  );
  if (faltaEquipamiento) motivos.push(MOTIVO_EQUIPAMIENTO);
  if (salida.requiereCertificado && participante.certificado?.estado !== 'APROBADO') {
    motivos.push(MOTIVO_CERTIFICADO);
  }
  return { tipo: motivos.length ? 'NO_APTO' : 'APTO', motivos };
}
function plural(n, singular, pluralTexto) {
  return `${n} ${n === 1 ? singular : pluralTexto}`;
}
export function ControlAsistencia({ salida, participantes, enviando, errorGeneral, onVolver, onFinalizarControl }) {
  const [estados, setEstados] = useState(() =>
    Object.fromEntries(participantes.map((p) => [p.idParticipante, { asistencia: null, equipamiento: [] }])),
  );
  const [intentoFinalizar, setIntentoFinalizar] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const estadoDe = (id) => estados[id] ?? { asistencia: null, equipamiento: [] };
  function actualizar(id, cambio) {
    setEstados((prev) => ({ ...prev, [id]: { ...estadoDe(id), ...prev[id], ...cambio } }));
  }
  function alternarItem(id, idItem, tiene) {
    const actual = estadoDe(id).equipamiento;
    actualizar(id, {
      equipamiento: tiene ? [...new Set([...actual, idItem])] : actual.filter((i) => i !== idItem),
    });
  }
  const resultados = participantes.map((p) => ({
    participante: p,
    resultado: calcularResultado(salida, p, estadoDe(p.idParticipante)),
  }));
  const pendientes = participantes.filter((p) => estadoDe(p.idParticipante).asistencia === null);
  const registrados = participantes.length - pendientes.length;
  const porcentaje = participantes.length ? (registrados / participantes.length) * 100 : 0;
  const mostrarPendientes = intentoFinalizar && pendientes.length > 0;
  const aptos = resultados.filter((r) => r.resultado.tipo === 'APTO').length;
  const noAptos = resultados.filter((r) => r.resultado.tipo === 'NO_APTO').length;
  const ausentes = resultados.filter((r) => r.resultado.tipo === 'AUSENTE').length;
  const resumen = [
    plural(aptos, 'apto', 'aptos'),
    plural(noAptos, 'no apto', 'no aptos'),
    plural(ausentes, 'ausente', 'ausentes'),
  ].join(' · ');
  function handleFinalizar() {
    setIntentoFinalizar(true);
    if (pendientes.length > 0) return;
    setConfirmando(true);
  }
  function handleConfirmar() {
    const registros = participantes.map((p) => {
      const estado = estadoDe(p.idParticipante);
      const presente = estado.asistencia === 'PRESENTE';
      return {
        idParticipante: p.idParticipante,
        estadoAsistencia: estado.asistencia,
        equipamientoPresentado: presente ? estado.equipamiento : [],
      };
    });
    onFinalizarControl(registros);
  }
  const textoBoton = enviando ? 'Iniciando salida…' : 'Finalizar control · Iniciar salida';
  return (
    <div className="flex flex-col gap-6 pb-28 sm:pb-0">
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
          Control de asistencia
        </h1>
        <p className="max-w-2xl text-sm leading-relaxed text-pretty text-muted-foreground sm:text-base">
          Registrá la asistencia de cada participante y verificá el equipamiento y el certificado médico. Al finalizar,
          la salida pasa a estado &quot;En curso&quot;.
        </p>
      </header>

      <div className="flex flex-col gap-2">
        <p className="text-sm text-foreground" aria-live="polite">
          <span className="font-semibold tabular-nums">{registrados}</span> de{' '}
          <span className="tabular-nums">{participantes.length}</span> participantes registrados
        </p>
        <div
          role="progressbar"
          aria-label="Participantes registrados"
          aria-valuemin={0}
          aria-valuemax={participantes.length}
          aria-valuenow={registrados}
          className="h-2.5 w-full overflow-hidden rounded-full bg-muted ring-1 ring-border ring-inset"
        >
          <div
            className="h-full rounded-full bg-primary transition-[width] duration-300"
            style={{ width: `${porcentaje}%` }}
          />
        </div>
      </div>

      <ul className="flex flex-col gap-4">
        {resultados.map(({ participante: p, resultado }) => {
          const estado = estadoDe(p.idParticipante);
          const pendiente = mostrarPendientes && estado.asistencia === null;
          const idNombre = `participante-${p.idParticipante}`;
          return (
            <li key={p.idParticipante}>
              <Card
                aria-labelledby={idNombre}
                className={cn(
                  'gap-0 rounded-xl px-5 py-5 shadow-none ring-border sm:px-6',
                  pendiente && 'ring-2 ring-destructive',
                )}
              >
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <p id={idNombre} className="text-lg font-semibold text-foreground">
                      {nombreCompleto(p)}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      DNI {formatearDni(p.dni)}
                      <span aria-hidden="true"> · </span>
                      Reserva #{p.idReserva}
                    </p>
                    {pendiente && (
                      <p className="mt-1 text-sm font-medium text-destructive">Marcá si está presente o ausente.</p>
                    )}
                  </div>
                  <div
                    role="group"
                    aria-label={`Asistencia de ${nombreCompleto(p)}`}
                    className="grid grid-cols-2 gap-2 sm:flex"
                  >
                    <Button
                      type="button"
                      variant="outline"
                      aria-pressed={estado.asistencia === 'PRESENTE'}
                      disabled={enviando}
                      onClick={() => actualizar(p.idParticipante, { asistencia: 'PRESENTE' })}
                      className={cn(
                        'h-12 bg-card px-5 text-base font-semibold sm:min-w-32',
                        estado.asistencia === 'PRESENTE' &&
                          'border-primary bg-primary text-primary-foreground hover:bg-primary/90 hover:text-primary-foreground',
                      )}
                    >
                      <Check aria-hidden="true" />
                      Presente
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      aria-pressed={estado.asistencia === 'AUSENTE'}
                      disabled={enviando}
                      onClick={() => actualizar(p.idParticipante, { asistencia: 'AUSENTE' })}
                      className={cn(
                        'h-12 bg-card px-5 text-base font-semibold sm:min-w-32',
                        estado.asistencia === 'AUSENTE' &&
                          'border-destructive bg-destructive text-primary-foreground hover:bg-destructive/90 hover:text-primary-foreground',
                      )}
                    >
                      <X aria-hidden="true" />
                      Ausente
                    </Button>
                  </div>
                </div>

                {estado.asistencia === 'PRESENTE' && (
                  <div className="mt-5 flex flex-col border-t border-border pt-2">
                    <Requisito titulo="Equipamiento obligatorio">
                      {salida.equipamientoObligatorio.length === 0 ? (
                        <p className="py-2 text-sm text-muted-foreground">
                          La excursión no requiere equipamiento obligatorio.
                        </p>
                      ) : (
                        <ul className="flex flex-col">
                          {salida.equipamientoObligatorio.map((item) => {
                            const idSwitch = `eq-${p.idParticipante}-${item.idEquipamiento}`;
                            const lo_entrega = p.equipamientoSolicitado.includes(item.idEquipamiento);
                            return (
                              <li key={item.idEquipamiento}>
                                <label
                                  htmlFor={idSwitch}
                                  className="flex min-h-12 cursor-pointer items-center justify-between gap-4 py-1"
                                >
                                  <span className="flex flex-wrap items-center gap-x-2 gap-y-1 text-base text-foreground">
                                    {item.nombre}
                                    {lo_entrega && (
                                      <Badge className="h-6 gap-1 bg-secondary px-2 text-xs font-medium text-secondary-foreground">
                                        <Package className="size-3" aria-hidden="true" />
                                        Lo entrega la empresa
                                      </Badge>
                                    )}
                                  </span>
                                  <Switch
                                    id={idSwitch}
                                    disabled={enviando}
                                    checked={estado.equipamiento.includes(item.idEquipamiento)}
                                    onCheckedChange={(v) => alternarItem(p.idParticipante, item.idEquipamiento, v)}
                                    className="scale-125"
                                  />
                                </label>
                              </li>
                            );
                          })}
                        </ul>
                      )}
                    </Requisito>

                    <div className="flex min-h-12 items-center justify-between gap-4 py-1">
                      <span className="text-base text-foreground">Certificado médico</span>
                      {salida.requiereCertificado ? (
                        <CertificadoBadge estado={p.certificado?.estado ?? null} />
                      ) : (
                        <span className="text-sm text-muted-foreground">No requerido</span>
                      )}
                    </div>
                  </div>
                )}

                {estado.asistencia !== null && (
                  <div
                    className={cn(
                      'flex flex-col gap-2 border-t border-border pt-4',
                      estado.asistencia === 'PRESENTE' ? 'mt-2' : 'mt-5',
                    )}
                    aria-live="polite"
                  >
                    <div className="flex items-center justify-between gap-4">
                      <span className="text-base font-semibold text-foreground">Resultado</span>
                      <ResultadoBadge tipo={resultado.tipo} />
                    </div>
                    {resultado.motivos.map((m) => (
                      <p key={m} className="text-sm text-destructive">
                        {m}
                      </p>
                    ))}
                  </div>
                )}
              </Card>
            </li>
          );
        })}
      </ul>

      {mostrarPendientes && (
        <Alert variant="destructive" className="rounded-xl bg-card">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>Falta completar la asistencia</AlertTitle>
          <AlertDescription>
            Faltan registrar la asistencia de: {pendientes.map(nombreCompleto).join('; ')}.
          </AlertDescription>
        </Alert>
      )}

      {errorGeneral && (
        <Alert variant="destructive" className="rounded-xl bg-card">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>No pudimos iniciar la salida</AlertTitle>
          <AlertDescription>{errorGeneral}</AlertDescription>
        </Alert>
      )}

      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-card px-4 pt-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] sm:static sm:z-auto sm:flex sm:justify-end sm:border-0 sm:bg-transparent sm:p-0">
        <Button
          type="button"
          onClick={handleFinalizar}
          disabled={enviando || participantes.length === 0}
          className="h-14 w-full px-8 text-base font-semibold sm:w-auto"
        >
          {enviando ? <Loader2 className="animate-spin" aria-hidden="true" /> : <Play aria-hidden="true" />}
          {textoBoton}
        </Button>
      </div>

      <Dialog open={confirmando} onOpenChange={(abierto) => !enviando && setConfirmando(abierto)}>
        <DialogContent showCloseButton={false} className="rounded-xl sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="font-serif text-xl font-semibold">
              ¿Finalizar el control e iniciar la salida?
            </DialogTitle>
            <DialogDescription className="text-base text-foreground">{resumen}</DialogDescription>
          </DialogHeader>
          <p className="rounded-lg bg-secondary px-3 py-2 text-sm text-secondary-foreground">
            Después no se puede modificar el control.
          </p>
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
              onClick={handleConfirmar}
              className="h-12 px-5 text-base font-semibold"
            >
              {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
              {enviando ? 'Iniciando salida…' : 'Confirmar e iniciar salida'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
function Requisito({ titulo, children }) {
  return (
    <section aria-label={titulo} className="flex flex-col">
      <h3 className="sr-only">{titulo}</h3>
      {children}
    </section>
  );
}
function CertificadoBadge({ estado }) {
  const { etiqueta, clase } = CERTIFICADOS[estado ?? 'SIN_CARGAR'];
  return <Badge className={cn(BADGE_CLASE, clase)}>{etiqueta}</Badge>;
}
function ResultadoBadge({ tipo }) {
  if (tipo === 'APTO') {
    return <Badge className={cn(BADGE_CLASE, 'bg-accent text-accent-foreground')}>Apto</Badge>;
  }
  if (tipo === 'NO_APTO') {
    return <Badge className={cn(BADGE_CLASE, 'bg-destructive/10 text-destructive')}>No apto</Badge>;
  }
  if (tipo === 'AUSENTE') {
    return <Badge className={cn(BADGE_CLASE, 'bg-secondary text-secondary-foreground')}>Ausente</Badge>;
  }
  return null;
}
