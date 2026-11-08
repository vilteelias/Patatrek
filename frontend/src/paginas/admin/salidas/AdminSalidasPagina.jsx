import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AlertTriangle, CheckCircle2, Loader2, MessageSquareText, Plus, Search } from 'lucide-react';
import { Button, buttonVariants } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { FormField } from '@/components/form-field';
import { EstadoSalidaBadge } from '@/components/guia/estado-salida-badge';
import { CLASE_SELECT } from '@/components/reserva/formato';
import { ESTADOS_SALIDA, cronograma } from '@/components/salidas/resumen-salida';
import { darDeBajaSalida, listarSalidasAdmin } from '@/api/salidas';
import { cn } from '@/lib/utils';

/** Acciones de una fila (CU-11, CU-12, CU-13, CU-14): solo las que admite el estado de la salida. */
function Acciones({ salida, onBaja, onObservacion }) {
  const enlaces = [
    { texto: 'Modificar', ruta: `/admin/salidas/${salida.idSalida}/editar`, ver: salida.modificable },
    { texto: 'Personal y cupos', ruta: `/admin/salidas/${salida.idSalida}/personal`, ver: salida.modificable },
    { texto: 'Contingencia', ruta: `/admin/salidas/${salida.idSalida}/contingencia`, ver: salida.admiteContingencia },
  ].filter((e) => e.ver);
  // CU-17 Ver observación de salida finalizada
  if (salida.estadoSalida === 'FINALIZADA') {
    return (
      <button
        type="button"
        onClick={() => onObservacion(salida)}
        className="text-xs font-semibold whitespace-nowrap text-primary hover:underline"
      >
        Ver observación
      </button>
    );
  }
  if (enlaces.length === 0 && !salida.puedeDarseDeBaja) {
    return <span className="text-xs text-muted-foreground">—</span>;
  }
  return (
    <div className="flex flex-col items-end gap-1">
      {enlaces.map((e) => (
        <Link key={e.texto} to={e.ruta} className="text-xs font-semibold whitespace-nowrap text-primary hover:underline">
          {e.texto}
        </Link>
      ))}
      {salida.puedeDarseDeBaja && (
        <button
          type="button"
          onClick={() => onBaja(salida)}
          className="text-xs font-semibold whitespace-nowrap text-destructive hover:underline"
        >
          Dar de baja
        </button>
      )}
    </div>
  );
}

/** CU-10 Listar salidas (vista del Administrador, RF-09). */
export default function AdminSalidasPagina() {
  const location = useLocation();
  const navigate = useNavigate();
  const [filtros, setFiltros] = useState({ excursion: '', estado: '' });
  const [salidas, setSalidas] = useState(null);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState(location.state?.aviso ?? '');
  const [baja, setBaja] = useState(null);
  const [observacion, setObservacion] = useState(null);
  const [recarga, setRecarga] = useState(0);

  useEffect(() => {
    if (location.state?.aviso) navigate(location.pathname, { replace: true, state: null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let vigente = true;
    const t = setTimeout(() => {
      listarSalidasAdmin(filtros)
        .then((s) => vigente && (setSalidas(s), setError('')))
        .catch((e) => vigente && setError(e.message));
    }, 250);
    return () => {
      vigente = false;
      clearTimeout(t);
    };
  }, [filtros, recarga]);

  const cambiar = (campo) => (e) => setFiltros((f) => ({ ...f, [campo]: e.target.value }));

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm text-muted-foreground">Gestión operativa</p>
          <h1 className="font-serif text-3xl font-semibold text-foreground">Salidas</h1>
          <p className="text-muted-foreground">Las realizaciones programadas de cada excursión.</p>
        </div>
        <Link to="/admin/salidas/nueva" className={cn(buttonVariants(), 'h-10 shrink-0 px-4')}>
          <Plus aria-hidden="true" /> Planificar salida
        </Link>
      </div>

      {aviso && (
        <div className="flex items-start gap-3 rounded-xl border border-primary/30 bg-accent px-4 py-3 text-sm text-accent-foreground" role="status">
          <CheckCircle2 className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <p className="flex-1">{aviso}</p>
          <button type="button" className="text-xs underline" onClick={() => setAviso('')}>
            Cerrar
          </button>
        </div>
      )}

      <section aria-label="Filtros" className="grid gap-3 rounded-2xl border border-border bg-card px-5 py-4 sm:grid-cols-[2fr_1fr]">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="fs-excursion">Excursión</Label>
          <div className="relative">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
            <Input
              id="fs-excursion"
              value={filtros.excursion}
              onChange={cambiar('excursion')}
              placeholder="Nombre de la excursión"
              className="h-10 bg-card pl-9"
            />
          </div>
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="fs-estado">Estado</Label>
          <select id="fs-estado" value={filtros.estado} onChange={cambiar('estado')} className={CLASE_SELECT}>
            <option value="">Todos los estados</option>
            {ESTADOS_SALIDA.map((e) => (
              <option key={e.valor} value={e.valor}>
                {e.etiqueta}
              </option>
            ))}
          </select>
        </div>
      </section>

      {error && (
        <p className="rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">
          {error}
        </p>
      )}

      {salidas === null ? (
        !error && (
          <p className="flex items-center gap-2 py-8 text-muted-foreground">
            <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando salidas…
          </p>
        )
      ) : salidas.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-input px-6 py-12 text-center text-muted-foreground">
          {filtros.excursion || filtros.estado ? 'No hay salidas para los filtros elegidos.' : 'Todavía no hay salidas planificadas.'}
        </p>
      ) : (
        <div className="relative overflow-x-auto rounded-2xl border border-border bg-card">
          <table className="w-full min-w-[980px] text-left text-sm">
            <thead className="border-b border-border bg-muted/60 text-xs uppercase tracking-wide text-muted-foreground">
              <tr>
                <th className="px-4 py-3 font-medium">Salida</th>
                <th className="px-4 py-3 font-medium">Fecha y horario</th>
                <th className="px-4 py-3 font-medium">Guías</th>
                <th className="px-4 py-3 font-medium">Refugios</th>
                <th className="px-4 py-3 text-right font-medium">Cupos</th>
                <th className="px-4 py-3 font-medium">Estado</th>
                <th className="px-4 py-3 text-right font-medium">
                  <span className="sr-only">Acciones</span>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {salidas.map((s) => (
                <tr key={s.idSalida} className="align-top">
                  <td className="px-4 py-3">
                    <p className="font-semibold text-foreground">{s.excursion}</p>
                    <p className="text-xs text-muted-foreground">Salida #{s.idSalida}</p>
                  </td>
                  <td className="px-4 py-3 text-foreground">{cronograma(s)}</td>
                  <td className="px-4 py-3">
                    <p className="text-foreground">{s.guiaResponsable?.nombre ?? '—'}</p>
                    {s.ayudantes.length > 0 && (
                      <p className="text-xs text-muted-foreground">
                        {s.ayudantes.length === 1 ? 'Ayudante: ' : 'Ayudantes: '}
                        {s.ayudantes.map((a) => a.nombre).join(' · ')}
                      </p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">
                    {s.refugios.length === 0
                      ? '—'
                      : s.refugios.map((r) => (
                          <span key={r.idRefugio} className="block">
                            {r.nombre} · {r.plazasReservadas} plazas
                          </span>
                        ))}
                  </td>
                  <td className="px-4 py-3 text-right tabular-nums">
                    <p className="font-semibold text-foreground">
                      {s.cuposOcupados} / {s.cupoTotal}
                    </p>
                    <p className="text-xs text-muted-foreground">máx. {s.cupoMaximo}</p>
                  </td>
                  <td className="px-4 py-3">
                    <EstadoSalidaBadge estado={s.estadoSalida} className="h-5 px-2.5" />
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Acciones salida={s} onBaja={setBaja} onObservacion={setObservacion} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ObservacionDialog salida={observacion} onCerrar={() => setObservacion(null)} />
      <BajaDialog
        salida={baja}
        onCerrar={() => setBaja(null)}
        onHecho={(s) => {
          setBaja(null);
          setAviso(`La salida #${s.idSalida} (${s.excursion}) quedó dada de baja.`);
          setRecarga((n) => n + 1);
        }}
      />
    </div>
  );
}

/** CU-13 Dar de baja salida: pide el motivo (obligatorio). */
/** CU-17 Ver observación de salida finalizada: lo que registró el guía responsable al finalizarla (CU-16). */
function ObservacionDialog({ salida, onCerrar }) {
  const fin = salida?.fechaHoraFinReal ? new Date(salida.fechaHoraFinReal) : null;
  return (
    <Dialog open={!!salida} onOpenChange={(a) => !a && onCerrar()}>
      <DialogContent className="rounded-xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="font-serif text-xl font-semibold">Observación de la salida</DialogTitle>
          <DialogDescription>{salida && `${salida.excursion} · ${cronograma(salida)}`}</DialogDescription>
        </DialogHeader>
        {salida && (
          <div className="flex flex-col gap-3 text-sm">
            <dl className="grid gap-2 sm:grid-cols-2">
              <div>
                <dt className="text-xs text-muted-foreground">Guía responsable</dt>
                <dd className="font-medium text-foreground">{salida.guiaResponsable?.nombre ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Finalizada el</dt>
                <dd className="font-medium text-foreground">
                  {fin
                    ? `${fin.toLocaleDateString('es-AR', { day: 'numeric', month: 'long', year: 'numeric' })} · ${fin.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })} h`
                    : '—'}
                </dd>
              </div>
            </dl>
            <div className="flex gap-2 rounded-lg border border-border bg-muted/50 px-3 py-3">
              <MessageSquareText className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
              {salida.observaciones?.trim() ? (
                <p className="whitespace-pre-line text-foreground">{salida.observaciones}</p>
              ) : (
                <p className="text-muted-foreground">El guía no dejó observaciones al finalizar la salida.</p>
              )}
            </div>
          </div>
        )}
        <DialogFooter>
          <Button type="button" variant="outline" onClick={onCerrar}>
            Cerrar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function BajaDialog({ salida, onCerrar, onHecho }) {
  const [motivo, setMotivo] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (salida) {
      setMotivo('');
      setError('');
      setEnviando(false);
    }
  }, [salida]);

  const confirmar = async () => {
    if (!motivo.trim()) {
      setError('Ingresá el motivo de la baja.');
      return;
    }
    setEnviando(true);
    try {
      onHecho(await darDeBajaSalida(salida.idSalida, motivo.trim()));
    } catch (e) {
      setError(e.errores?.motivo ?? e.message);
      setEnviando(false);
    }
  };

  return (
    <Dialog open={!!salida} onOpenChange={(a) => !a && !enviando && onCerrar()}>
      <DialogContent showCloseButton={false} className="rounded-xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="font-serif text-xl font-semibold">Dar de baja salida</DialogTitle>
          <DialogDescription>
            {salida && `${salida.excursion} · ${cronograma(salida)}`}
          </DialogDescription>
        </DialogHeader>
        <p className="flex gap-2 rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2.5 text-sm text-destructive">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          La salida pasará a Cancelada, se desvinculan los guías y se liberan las plazas reservadas en los refugios.
        </p>
        <FormField id="baja-motivo" label="Motivo" required error={error}>
          <Textarea
            id="baja-motivo"
            value={motivo}
            maxLength={400}
            onChange={(e) => setMotivo(e.target.value)}
            aria-invalid={error ? true : undefined}
            placeholder="Ej. Se reorganizó el calendario de la temporada."
          />
        </FormField>
        <DialogFooter>
          <Button variant="outline" onClick={onCerrar} disabled={enviando}>
            Volver
          </Button>
          <Button variant="destructive" onClick={confirmar} disabled={enviando}>
            {enviando && <Loader2 className="animate-spin" aria-hidden="true" />} Dar de baja
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
