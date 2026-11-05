import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, AlertTriangle, Loader2 } from 'lucide-react';
import { Button, buttonVariants } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { FormField, RequiredLegend } from '@/components/form-field';
import { hora } from '@/components/reserva/formato';
import { Migas, ResumenSalida } from '@/components/salidas/resumen-salida';
import { obtenerSalidaAdmin, registrarContingencia } from '@/api/salidas';
import { cn } from '@/lib/utils';

const TIPOS = [
  { valor: 'CLIMATICA', etiqueta: 'Climática', detalle: 'Tormenta, nevada, viento, crecida…' },
  { valor: 'OPERATIVA', etiqueta: 'Operativa', detalle: 'Cierre de sendero, refugio, transporte…' },
];

const RESOLUCIONES = [
  {
    valor: 'CANCELACION',
    etiqueta: 'Cancelar salida',
    aviso: 'La salida queda Cancelada. Las reservas señadas o confirmadas pasan a Pendiente de devolución (devolución total) y se avisa por correo a titulares y participantes.',
  },
  {
    valor: 'REPROGRAMACION',
    etiqueta: 'Reprogramar fecha',
    aviso: 'Se cambia la fecha y el horario manteniendo la duración. Cada titular recibe un correo y tiene que aceptar la nueva fecha o rechazarla (si la rechaza, se le devuelve el total).',
  },
  {
    valor: 'MODIFICACION',
    etiqueta: 'Modificar recorrido',
    aviso: 'La salida mantiene la fecha y se reemplaza el recorrido por el alternativo. Se avisa por correo a titulares y participantes.',
  },
];

function Opciones({ nombre, opciones, valor, onChange, error, deshabilitadas = [] }) {
  return (
    <div role="radiogroup" aria-labelledby={`t-${nombre}`} aria-invalid={error ? true : undefined} className="grid gap-2 sm:grid-cols-3">
      {opciones.map((o) => {
        const off = deshabilitadas.includes(o.valor);
        const activa = valor === o.valor;
        return (
          <button
            key={o.valor}
            type="button"
            role="radio"
            aria-checked={activa}
            disabled={off}
            onClick={() => onChange(o.valor)}
            className={cn(
              'flex flex-col items-start gap-0.5 rounded-xl border px-4 py-3 text-left text-sm transition-colors',
              activa ? 'border-primary bg-primary/10 text-foreground ring-1 ring-primary' : 'border-border bg-card hover:bg-muted',
              off && 'cursor-not-allowed opacity-50 hover:bg-card',
            )}
          >
            <span className="font-semibold">{o.etiqueta}</span>
            {o.detalle && <span className="text-xs text-muted-foreground">{o.detalle}</span>}
            {off && <span className="text-xs text-muted-foreground">No disponible con la salida en curso</span>}
          </button>
        );
      })}
    </div>
  );
}

/**
 * CU-14 Gestionar contingencias (prototipo pág. 29).
 * Tipo (climática u operativa), motivo y resolución: cancelación, reprogramación o modificación del recorrido.
 */
export default function ContingenciaPagina() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [salida, setSalida] = useState(null);
  const [datos, setDatos] = useState({
    tipoContingencia: '',
    resolucion: '',
    motivo: '',
    nuevaFechaInicio: '',
    nuevaHoraInicio: '',
    nuevaHoraFinalizacion: '',
    nuevoRecorrido: '',
  });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [confirmando, setConfirmando] = useState(false);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    obtenerSalidaAdmin(id)
      .then((s) => {
        setSalida(s);
        setDatos((d) => ({ ...d, nuevaHoraInicio: hora(s.horaInicio), nuevaHoraFinalizacion: hora(s.horaFinalizacion), nuevoRecorrido: '' }));
      })
      .catch((e) => setError(e.message));
  }, [id]);

  if (!salida) {
    return error ? (
      <p className="py-8 text-destructive" role="alert">{error}</p>
    ) : (
      <p className="flex items-center gap-2 py-8 text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando…
      </p>
    );
  }

  const enCurso = salida.estadoSalida === 'EN_CURSO';
  const resolucion = RESOLUCIONES.find((r) => r.valor === datos.resolucion);
  const fijar = (campo, valor) => {
    setDatos((d) => ({ ...d, [campo]: valor }));
    setErrores(({ [campo]: _x, ...resto }) => resto);
    setConfirmando(false);
  };

  const validar = () => {
    const e = {};
    if (!datos.tipoContingencia) e.tipoContingencia = 'Elegí el tipo de contingencia.';
    if (!datos.motivo.trim()) e.motivo = 'Ingresá el motivo de la contingencia.';
    if (!datos.resolucion) e.resolucion = 'Elegí cómo se resuelve.';
    if (datos.resolucion === 'REPROGRAMACION') {
      if (!datos.nuevaFechaInicio) e.nuevaFechaInicio = 'Ingresá la nueva fecha de inicio.';
      if (!datos.nuevaHoraInicio) e.nuevaHoraInicio = 'Ingresá el nuevo horario de inicio.';
      if (datos.nuevaFechaInicio && datos.nuevaHoraInicio && new Date(`${datos.nuevaFechaInicio}T${datos.nuevaHoraInicio}`) <= new Date()) {
        e.nuevaFechaInicio = 'La nueva fecha y hora tienen que ser posteriores a la actual.';
      }
    }
    if (datos.resolucion === 'MODIFICACION' && !datos.nuevoRecorrido.trim()) e.nuevoRecorrido = 'Ingresá el recorrido alternativo.';
    return e;
  };

  const revisar = (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    setError(Object.keys(e).length ? 'Revisá los datos marcados.' : '');
    if (Object.keys(e).length) return;
    setConfirmando(true);
  };

  const confirmar = async () => {
    setEnviando(true);
    setError('');
    const cuerpo = { tipoContingencia: datos.tipoContingencia, resolucion: datos.resolucion, motivo: datos.motivo.trim() };
    if (datos.resolucion === 'REPROGRAMACION') {
      Object.assign(cuerpo, {
        nuevaFechaInicio: datos.nuevaFechaInicio,
        nuevaHoraInicio: datos.nuevaHoraInicio,
        nuevaHoraFinalizacion: datos.nuevaHoraFinalizacion || null,
        nuevoRecorrido: datos.nuevoRecorrido.trim() || null,
      });
    }
    if (datos.resolucion === 'MODIFICACION') cuerpo.nuevoRecorrido = datos.nuevoRecorrido.trim();
    try {
      const r = await registrarContingencia(id, cuerpo);
      navigate('/admin/salidas', { state: { aviso: `Contingencia registrada en la salida #${id}. ${r.mensaje}` } });
    } catch (e) {
      setErrores(e.errores ?? {});
      setError(e.message);
      setConfirmando(false);
      setEnviando(false);
    }
  };

  return (
    <form onSubmit={revisar} noValidate className="mx-auto flex w-full max-w-3xl flex-col gap-5">
      <div>
        <Migas salida={salida} pantalla="Gestionar contingencia" />
        <h1 className="font-serif text-3xl font-semibold text-foreground">Gestionar contingencia</h1>
        <p className="text-muted-foreground">Registrá un imprevisto climático u operativo y elegí cómo se resuelve la salida.</p>
      </div>

      <ResumenSalida
        salida={salida}
        extra={
          <div className="flex flex-col gap-0.5 sm:col-span-2 lg:col-span-4">
            <span className="text-xs text-muted-foreground">Reservas vigentes que se verían afectadas</span>
            <span className="text-sm font-semibold text-foreground">
              {salida.reservasActivas} {salida.reservasActivas === 1 ? 'reserva' : 'reservas'} · {salida.cuposOcupados} participantes
            </span>
          </div>
        }
      />

      {error && (
        <p className="flex items-start gap-2 rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">
          <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
        </p>
      )}
      <RequiredLegend />

      <section className="flex flex-col gap-4 rounded-2xl border border-border bg-card px-5 py-5 sm:px-6">
        <div className="flex flex-col gap-1.5">
          <span id="t-tipoContingencia" className="text-sm font-medium text-foreground">
            Tipo de contingencia <span className="text-destructive">*</span>
          </span>
          <Opciones nombre="tipoContingencia" opciones={TIPOS} valor={datos.tipoContingencia} onChange={(v) => fijar('tipoContingencia', v)} error={errores.tipoContingencia} />
          {errores.tipoContingencia && <p role="alert" className="text-xs text-destructive">{errores.tipoContingencia}</p>}
        </div>

        <FormField id="c-motivo" label="Motivo" required error={errores.motivo} hint={`${datos.motivo.length}/500 caracteres. Se incluye en el correo a los clientes.`}>
          <Textarea
            id="c-motivo"
            rows={3}
            maxLength={500}
            value={datos.motivo}
            onChange={(e) => fijar('motivo', e.target.value)}
            aria-invalid={errores.motivo ? true : undefined}
            placeholder="Ej. Alerta por vientos fuertes en la zona del cerro."
          />
        </FormField>

        <div className="flex flex-col gap-1.5">
          <span id="t-resolucion" className="text-sm font-medium text-foreground">
            Resolución <span className="text-destructive">*</span>
          </span>
          <Opciones
            nombre="resolucion"
            opciones={RESOLUCIONES}
            valor={datos.resolucion}
            onChange={(v) => fijar('resolucion', v)}
            error={errores.resolucion}
            deshabilitadas={enCurso ? ['REPROGRAMACION'] : []}
          />
          {errores.resolucion && <p role="alert" className="text-xs text-destructive">{errores.resolucion}</p>}
        </div>

        {datos.resolucion === 'REPROGRAMACION' && (
          <div className="grid gap-4 sm:grid-cols-3">
            <FormField id="c-nuevaFechaInicio" label="Nueva fecha de inicio" required error={errores.nuevaFechaInicio}
              hint={salida.duracionDias > 1 ? `Se mantiene la duración de ${salida.duracionDias} días.` : undefined}>
              <Input id="c-nuevaFechaInicio" type="date" className="h-10 bg-card" value={datos.nuevaFechaInicio}
                onChange={(e) => fijar('nuevaFechaInicio', e.target.value)} aria-invalid={errores.nuevaFechaInicio ? true : undefined} />
            </FormField>
            <FormField id="c-nuevaHoraInicio" label="Hora de inicio" required error={errores.nuevaHoraInicio}>
              <Input id="c-nuevaHoraInicio" type="time" className="h-10 bg-card" value={datos.nuevaHoraInicio}
                onChange={(e) => fijar('nuevaHoraInicio', e.target.value)} aria-invalid={errores.nuevaHoraInicio ? true : undefined} />
            </FormField>
            <FormField id="c-nuevaHoraFinalizacion" label="Hora de finalización" optional error={errores.nuevaHoraFinalizacion}>
              <Input id="c-nuevaHoraFinalizacion" type="time" className="h-10 bg-card" value={datos.nuevaHoraFinalizacion}
                onChange={(e) => fijar('nuevaHoraFinalizacion', e.target.value)} aria-invalid={errores.nuevaHoraFinalizacion ? true : undefined} />
            </FormField>
          </div>
        )}

        {(datos.resolucion === 'MODIFICACION' || datos.resolucion === 'REPROGRAMACION') && (
          <FormField
            id="c-nuevoRecorrido"
            label="Recorrido alternativo"
            required={datos.resolucion === 'MODIFICACION'}
            optional={datos.resolucion === 'REPROGRAMACION'}
            error={errores.nuevoRecorrido}
            hint={datos.resolucion === 'REPROGRAMACION' ? 'Solo si además cambia el recorrido.' : `Recorrido actual: ${salida.recorrido || '—'}`}
          >
            <Textarea id="c-nuevoRecorrido" rows={3} maxLength={1000} value={datos.nuevoRecorrido}
              onChange={(e) => fijar('nuevoRecorrido', e.target.value)} aria-invalid={errores.nuevoRecorrido ? true : undefined} />
          </FormField>
        )}

        {resolucion && (
          <p className="flex items-start gap-2 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-200">
            <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {resolucion.aviso}
          </p>
        )}
      </section>

      {confirmando ? (
        <div role="alertdialog" aria-labelledby="t-confirmar" className="flex flex-col gap-3 rounded-2xl border border-destructive/40 bg-destructive/5 px-5 py-4">
          <p id="t-confirmar" className="text-sm font-medium text-foreground">
            ¿Confirmás {resolucion.etiqueta.toLowerCase()}? Esta acción no se puede deshacer
            {salida.reservasActivas > 0 ? ` y se va a avisar a ${salida.reservasActivas} ${salida.reservasActivas === 1 ? 'reserva' : 'reservas'} por correo.` : '.'}
          </p>
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" onClick={() => setConfirmando(false)} disabled={enviando}>Volver</Button>
            <Button type="button" variant="destructive" onClick={confirmar} disabled={enviando} autoFocus>
              {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
              Sí, confirmar
            </Button>
          </div>
        </div>
      ) : (
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Link to="/admin/salidas" className={cn(buttonVariants({ variant: 'outline' }), 'h-10 px-5')}>Cancelar</Link>
          <Button type="submit" className="h-10 px-5">Registrar contingencia</Button>
        </div>
      )}
    </form>
  );
}
