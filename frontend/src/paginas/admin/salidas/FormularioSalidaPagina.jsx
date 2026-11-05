import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, Clock, Loader2 } from 'lucide-react';
import { Button, buttonVariants } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { FormField, RequiredLegend } from '@/components/form-field';
import { CLASE_SELECT, hora } from '@/components/reserva/formato';
import { Migas, cronograma } from '@/components/salidas/resumen-salida';
import { modificarSalida, obtenerSalidaAdmin, opcionesSalida, planificarSalida } from '@/api/salidas';
import { cn } from '@/lib/utils';

const VACIO = {
  idExcursion: '',
  fechaInicio: '',
  horaInicio: '',
  fechaFinalizacion: '',
  horaFinalizacion: '',
  idGuiaResponsable: '',
  cupoMaximo: '',
  plazasPorRefugio: '',
  refugiosPorNoche: [],
};

/** El campo de fecha del navegador deja escribir años de hasta 6 cifras: se limita a 4. */
const FECHA_MAXIMA = '9999-12-31';
/** Ninguna excursión dura tanto: un rango mayor es un error de tipeo y no se calculan sus noches. */
const NOCHES_MAXIMAS = 60;

/** "AAAA-MM-DD" con año de 4 cifras y que exista en el calendario. */
function fechaValida(f) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(f ?? '')) return false;
  const t = Date.parse(`${f}T00:00:00Z`);
  return !Number.isNaN(t) && new Date(t).toISOString().slice(0, 10) === f;
}

/** Días entre dos fechas "AAAA-MM-DD" (UTC para no depender del horario de verano). NaN si alguna no es válida. */
function diasEntre(a, b) {
  if (!fechaValida(a) || !fechaValida(b)) return NaN;
  return Math.round((Date.parse(`${b}T00:00:00Z`) - Date.parse(`${a}T00:00:00Z`)) / 86400000);
}

/** Suma días a una fecha "AAAA-MM-DD". Devuelve '' si la fecha no es válida. */
function sumarDias(fecha, dias) {
  if (!fechaValida(fecha)) return '';
  const d = new Date(`${fecha}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + dias);
  return fechaValida(d.toISOString().slice(0, 10)) ? d.toISOString().slice(0, 10) : '';
}

function Seccion({ numero, titulo, descripcion, deshabilitada, aviso, children }) {
  return (
    <section
      aria-disabled={deshabilitada || undefined}
      className={cn('rounded-2xl border border-border bg-card px-5 py-5 sm:px-6', deshabilitada && 'opacity-60')}
    >
      {deshabilitada && aviso && (
        <p className="mb-4 rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">{aviso}</p>
      )}
      <h2 className="font-semibold text-foreground">
        {numero}. {titulo}
      </h2>
      {descripcion && <p className="mb-4 text-sm text-muted-foreground">{descripcion}</p>}
      <fieldset disabled={deshabilitada} className="flex flex-col gap-4">
        {children}
      </fieldset>
    </section>
  );
}

/** Validaciones rápidas en pantalla (el backend vuelve a validar todo). */
function validar(d, excursion, guia, noches, editando) {
  const e = {};
  if (!editando) {
    if (!d.idExcursion) e.idExcursion = 'Elegí la excursión base.';
    if (!d.fechaInicio) e.fechaInicio = 'Ingresá la fecha de inicio.';
    if (!d.horaInicio) e.horaInicio = 'Ingresá la hora de inicio.';
    if (!d.fechaFinalizacion) e.fechaFinalizacion = 'Ingresá la fecha de finalización.';
    if (!d.horaFinalizacion) e.horaFinalizacion = 'Ingresá la hora de finalización.';
    if (d.fechaInicio && !fechaValida(d.fechaInicio)) e.fechaInicio = 'Ingresá una fecha válida.';
    if (d.fechaFinalizacion && !fechaValida(d.fechaFinalizacion)) e.fechaFinalizacion = 'Ingresá una fecha válida.';
    if (!e.fechaInicio && d.fechaInicio && d.horaInicio && new Date(`${d.fechaInicio}T${d.horaInicio}`) <= new Date()) {
      e.fechaInicio = 'La salida tiene que empezar en una fecha y hora futuras.';
    }
    if (!e.fechaInicio && !e.fechaFinalizacion && d.fechaInicio && d.fechaFinalizacion) {
      if (d.fechaFinalizacion < d.fechaInicio) {
        e.fechaFinalizacion = 'La fecha de finalización no puede ser anterior a la de inicio.';
      } else if (excursion && diasEntre(d.fechaInicio, d.fechaFinalizacion) + 1 !== excursion.duracionDias) {
        e.fechaFinalizacion = `La excursión dura ${excursion.duracionDias} ${excursion.duracionDias === 1 ? 'día' : 'días'}.`;
      } else if (d.fechaInicio === d.fechaFinalizacion && d.horaInicio && d.horaFinalizacion && d.horaFinalizacion <= d.horaInicio) {
        e.horaFinalizacion = 'La hora de finalización tiene que ser posterior a la de inicio.';
      }
    }
    if (!d.cupoMaximo || Number(d.cupoMaximo) <= 0) e.cupoMaximo = 'Ingresá el cupo máximo de la salida.';
    else if (guia && Number(d.cupoMaximo) <= guia.capacidadEstandar) {
      e.cupoMaximo = `Tiene que ser mayor que el cupo base del guía (${guia.capacidadEstandar}).`;
    }
  }
  if (!d.idGuiaResponsable) e.idGuiaResponsable = 'Elegí el guía responsable.';
  else if (guia && !guia.disponible) e.idGuiaResponsable = guia.motivoNoDisponible;
  if (noches > 0) {
    if (!d.plazasPorRefugio || Number(d.plazasPorRefugio) <= 0) e.plazasPorRefugio = 'Ingresá cuántas plazas reservar en cada refugio.';
    if (d.refugiosPorNoche.length !== noches || d.refugiosPorNoche.some((r) => !r)) {
      e.refugiosPorNoche = `Elegí un refugio para cada una de las ${noches} ${noches === 1 ? 'noche' : 'noches'}.`;
    }
  }
  return e;
}

/**
 * CU-09 Planificar salida (prototipo pág. 26) y CU-11 Modificar salida.
 * Al modificar, la excursión, el cronograma y el cupo máximo quedan fijos: se cambian el guía responsable
 * y los refugios con sus plazas (RF-10). El cronograma solo cambia con una contingencia (CU-14).
 */
export default function FormularioSalidaPagina() {
  const { id } = useParams();
  const editando = Boolean(id);
  const navigate = useNavigate();

  const [datos, setDatos] = useState(VACIO);
  const [salida, setSalida] = useState(null);
  const [opciones, setOpciones] = useState(null);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [cargandoDisponibilidad, setCargandoDisponibilidad] = useState(false);

  // Carga inicial: la salida (si se modifica) y las opciones
  useEffect(() => {
    if (!editando) {
      opcionesSalida().then(setOpciones).catch((e) => setError(e.message));
      return;
    }
    obtenerSalidaAdmin(id)
      .then((s) => {
        setSalida(s);
        const noches = diasEntre(s.fechaInicio, s.fechaFinalizacion);
        // Reconstruye el refugio de cada noche a partir del orden de visita
        const porOrden = [...s.refugios].sort((a, b) => b.ordenVisita - a.ordenVisita);
        const porNoche = Array.from({ length: noches }, (_, i) => {
          const r = porOrden.find((x) => x.ordenVisita <= i + 1) ?? porOrden[porOrden.length - 1];
          return r ? String(r.idRefugio) : '';
        });
        setDatos({
          ...VACIO,
          idExcursion: String(s.idExcursion),
          fechaInicio: s.fechaInicio,
          horaInicio: hora(s.horaInicio),
          fechaFinalizacion: s.fechaFinalizacion,
          horaFinalizacion: hora(s.horaFinalizacion),
          idGuiaResponsable: s.guiaResponsable ? String(s.guiaResponsable.idGuia) : '',
          cupoMaximo: String(s.cupoMaximo),
          plazasPorRefugio: s.refugios[0] ? String(s.refugios[0].plazasReservadas) : '',
          refugiosPorNoche: porNoche,
        });
      })
      .catch((e) => setError(e.message));
  }, [editando, id]);

  const excursion = opciones?.excursiones.find((e) => String(e.idExcursion) === datos.idExcursion)
    ?? (salida && { idExcursion: salida.idExcursion, nombre: salida.excursion, duracionDias: salida.duracionDias });
  const nochesDelRango = diasEntre(datos.fechaInicio, datos.fechaFinalizacion);
  // Solo con fechas válidas y un rango razonable se consultan guías y refugios (evita cálculos con años irreales)
  const cronogramaCompleto =
    Boolean(datos.horaInicio && datos.horaFinalizacion) &&
    Number.isInteger(nochesDelRango) && nochesDelRango >= 0 && nochesDelRango <= NOCHES_MAXIMAS;
  const noches = cronogramaCompleto ? nochesDelRango : 0;
  const guia = opciones?.guias.find((g) => String(g.idGuia) === datos.idGuiaResponsable);

  // Disponibilidad de guías y refugios para el período elegido
  useEffect(() => {
    if (!cronogramaCompleto) return undefined;
    let vigente = true;
    setCargandoDisponibilidad(true);
    const t = setTimeout(() => {
      opcionesSalida({
        fechaInicio: datos.fechaInicio,
        horaInicio: datos.horaInicio,
        fechaFinalizacion: datos.fechaFinalizacion,
        horaFinalizacion: datos.horaFinalizacion,
        excluirSalida: id,
      })
        .then((o) => vigente && setOpciones(o))
        .catch((e) => vigente && setError(e.message))
        .finally(() => vigente && setCargandoDisponibilidad(false));
    }, 300);
    return () => {
      vigente = false;
      clearTimeout(t);
    };
  }, [cronogramaCompleto, datos.fechaInicio, datos.horaInicio, datos.fechaFinalizacion, datos.horaFinalizacion, id]);

  // Un refugio por noche: ajusta el largo de la lista cuando cambian las fechas
  useEffect(() => {
    setDatos((d) => {
      if (d.refugiosPorNoche.length === noches) return d;
      const lista = Array.from({ length: noches }, (_, i) => d.refugiosPorNoche[i] ?? d.refugiosPorNoche[i - 1] ?? '');
      return { ...d, refugiosPorNoche: lista };
    });
  }, [noches]);

  const cambiar = (campo) => (e) => {
    const valor = e.target.value;
    setDatos((d) => {
      const n = { ...d, [campo]: valor };
      // Sugiere la fecha de finalización según la duración de la excursión
      if ((campo === 'fechaInicio' || campo === 'idExcursion') && n.fechaInicio) {
        const ex = opciones?.excursiones.find((x) => String(x.idExcursion) === n.idExcursion);
        if (ex) n.fechaFinalizacion = sumarDias(n.fechaInicio, ex.duracionDias - 1);
      }
      return n;
    });
    setErrores(({ [campo]: _x, ...resto }) => {
      // La fecha de finalización se completa sola al elegir la excursión o la fecha de inicio
      if (campo === 'fechaInicio' || campo === 'idExcursion') delete resto.fechaFinalizacion;
      return resto;
    });
  };

  const cambiarNoche = (i) => (e) => {
    const valor = e.target.value;
    setDatos((d) => {
      const lista = [...d.refugiosPorNoche];
      lista[i] = valor;
      // Si las noches siguientes estaban vacías, propone el mismo refugio
      for (let k = i + 1; k < lista.length; k += 1) if (!lista[k]) lista[k] = valor;
      return { ...d, refugiosPorNoche: lista };
    });
    setErrores(({ refugiosPorNoche: _x, ...resto }) => resto);
  };

  const refugiosOrdenados = useMemo(() => opciones?.refugios ?? [], [opciones]);

  const enviar = async (e) => {
    e.preventDefault();
    const encontrados = validar(datos, excursion, guia, noches, editando);
    setErrores(encontrados);
    setError('');
    if (Object.keys(encontrados).length) {
      setError('Revisá los datos marcados.');
      setTimeout(() => document.querySelector('[aria-invalid="true"]')?.focus(), 30);
      return;
    }
    setEnviando(true);
    const refugios = noches > 0 ? datos.refugiosPorNoche.map(Number) : [];
    const plazas = noches > 0 ? Number(datos.plazasPorRefugio) : null;
    try {
      const s = editando
        ? await modificarSalida(id, {
            idGuiaResponsable: Number(datos.idGuiaResponsable),
            plazasPorRefugio: plazas,
            refugiosPorNoche: refugios,
          })
        : await planificarSalida({
            idExcursion: Number(datos.idExcursion),
            fechaInicio: datos.fechaInicio,
            horaInicio: datos.horaInicio,
            fechaFinalizacion: datos.fechaFinalizacion,
            horaFinalizacion: datos.horaFinalizacion,
            idGuiaResponsable: Number(datos.idGuiaResponsable),
            cupoMaximo: Number(datos.cupoMaximo),
            plazasPorRefugio: plazas,
            refugiosPorNoche: refugios,
          });
      navigate('/admin/salidas', {
        state: {
          aviso: editando
            ? `Se guardaron los cambios de la salida #${s.idSalida}. Cupo total: ${s.cupoTotal}.`
            : `Salida #${s.idSalida} planificada: ${s.excursion}, ${cronograma(s)}. Ya se puede reservar.`,
        },
      });
    } catch (err) {
      setErrores(err.errores ?? {});
      setError(err.message);
      setEnviando(false);
    }
  };

  if (editando && !salida && !error) {
    return (
      <p className="flex items-center gap-2 py-8 text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando…
      </p>
    );
  }

  const prop = (campo) => ({
    id: `s-${campo}`,
    value: datos[campo],
    onChange: cambiar(campo),
    'aria-invalid': errores[campo] ? true : undefined,
  });

  return (
    <form onSubmit={enviar} noValidate className="mx-auto flex w-full max-w-3xl flex-col gap-5">
      <div>
        <Migas salida={salida} pantalla={editando ? 'Modificar salida' : 'Nueva salida'} />
        <h1 className="font-serif text-3xl font-semibold text-foreground">
          {editando ? 'Modificar salida' : 'Planificar salida'}
        </h1>
        <p className="text-muted-foreground">
          {editando
            ? 'Cambiá el guía responsable o los refugios. El cronograma solo cambia registrando una contingencia.'
            : 'Definí el cronograma, el guía responsable y los recursos de esta nueva realización de una excursión ya registrada en el catálogo.'}
        </p>
      </div>

      {error && (
        <p className="flex items-start gap-2 rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">
          <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
        </p>
      )}
      {!editando && <RequiredLegend />}

      <Seccion numero={1} titulo="Excursión base" descripcion="La salida hereda el precio, la dificultad y las condiciones de la excursión." deshabilitada={editando}>
        <FormField id="s-idExcursion" label="Excursión" required={!editando} locked={editando} error={errores.idExcursion}>
          <select {...prop('idExcursion')} className={CLASE_SELECT}>
            <option value="">Seleccioná una excursión</option>
            {(opciones?.excursiones ?? (salida ? [{ idExcursion: salida.idExcursion, nombre: salida.excursion, duracionDias: salida.duracionDias }] : [])).map((e) => (
              <option key={e.idExcursion} value={e.idExcursion}>
                {e.nombre} · {e.duracionDias === 1 ? '1 día' : `${e.duracionDias} días`}
              </option>
            ))}
          </select>
        </FormField>
      </Seccion>

      <Seccion
        numero={2}
        titulo="Cronograma"
        descripcion="Si la fecha de finalización es distinta a la de inicio, la salida es de más de un día y requiere refugios."
        deshabilitada={editando || !datos.idExcursion}
        aviso={!editando ? 'Seleccioná una excursión base para habilitar esta sección.' : undefined}
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField id="s-fechaInicio" label="Fecha de inicio" required={!editando} locked={editando} error={errores.fechaInicio}>
            <Input type="date" min={new Date().toISOString().slice(0, 10)} max={FECHA_MAXIMA} className="h-10 bg-card" {...prop('fechaInicio')} />
          </FormField>
          <FormField id="s-horaInicio" label="Hora de inicio" required={!editando} locked={editando} error={errores.horaInicio}>
            <Input type="time" className="h-10 bg-card" {...prop('horaInicio')} />
          </FormField>
          <FormField
            id="s-fechaFinalizacion"
            label="Fecha de finalización"
            required={!editando}
            locked={editando}
            error={errores.fechaFinalizacion}
            hint={excursion && !editando ? `La excursión dura ${excursion.duracionDias === 1 ? '1 día' : `${excursion.duracionDias} días`}.` : undefined}
          >
            <Input type="date" max={FECHA_MAXIMA} className="h-10 bg-card" {...prop('fechaFinalizacion')} />
          </FormField>
          <FormField id="s-horaFinalizacion" label="Hora de finalización" required={!editando} locked={editando} error={errores.horaFinalizacion}>
            <Input type="time" className="h-10 bg-card" {...prop('horaFinalizacion')} />
          </FormField>
        </div>
        {noches > 0 && (
          <p className="flex items-center gap-2 rounded-lg bg-secondary px-3 py-2 text-xs text-secondary-foreground">
            <Clock className="size-3.5" aria-hidden="true" />
            Salida de más de un día ({noches} {noches === 1 ? 'noche' : 'noches'}): se habilita la asignación de refugios más abajo.
          </p>
        )}
      </Seccion>

      <Seccion
        numero={3}
        titulo="Guía responsable y cupos"
        descripcion="El cupo base de la salida se toma de la capacidad estándar del guía responsable."
        deshabilitada={!cronogramaCompleto}
        aviso="Completá el cronograma para ver qué guías están disponibles."
      >
        <FormField
          id="s-idGuiaResponsable"
          label="Guía responsable"
          required
          error={errores.idGuiaResponsable}
          hint={cargandoDisponibilidad ? 'Revisando la disponibilidad de los guías…' : 'Los guías que ya tienen otra salida en esas fechas aparecen como no disponibles.'}
        >
          <select {...prop('idGuiaResponsable')} className={CLASE_SELECT}>
            <option value="">Seleccioná un guía</option>
            {(opciones?.guias ?? []).map((g) => (
              <option key={g.idGuia} value={g.idGuia} disabled={!g.disponible && String(g.idGuia) !== datos.idGuiaResponsable}>
                {g.nombre} (capacidad {g.capacidadEstandar}){g.disponible ? '' : ' — no disponible'}
              </option>
            ))}
          </select>
        </FormField>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <span className="text-sm font-medium text-foreground">Cupo base</span>
            <span className="flex h-10 items-center rounded-lg border border-dashed border-input px-3 text-sm text-muted-foreground">
              {guia ? `${guia.capacidadEstandar} (capacidad estándar del guía)` : 'Elegí un guía'}
            </span>
          </div>
          <FormField
            id="s-cupoMaximo"
            label="Cupo máximo de la salida"
            required={!editando}
            locked={editando}
            error={errores.cupoMaximo}
            hint={!editando ? 'Tiene que ser mayor que el cupo base. Con guías ayudantes el cupo puede crecer hasta este máximo.' : undefined}
          >
            <Input type="number" min="1" inputMode="numeric" placeholder="Ej. 18" className="h-10 bg-card" {...prop('cupoMaximo')} disabled={editando} />
          </FormField>
        </div>
      </Seccion>

      <Seccion
        numero={4}
        titulo="Refugios y plazas de alojamiento"
        descripcion="Obligatorio para salidas de más de un día. Las plazas reservadas quedan disponibles para que los clientes elijan la modalidad Refugio al reservar."
        deshabilitada={!cronogramaCompleto || noches === 0}
        aviso={cronogramaCompleto && noches === 0 ? 'La salida dura un solo día: no necesita refugios.' : 'Completá el cronograma para asignar refugios.'}
      >
        {noches > 0 && (
          <>
            <FormField
              id="s-plazasPorRefugio"
              label="Plazas a reservar por refugio"
              required
              error={errores.plazasPorRefugio}
              hint="Se aplica por igual a cada refugio de la salida."
              className="sm:max-w-xs"
            >
              <Input type="number" min="1" inputMode="numeric" className="h-10 bg-card" {...prop('plazasPorRefugio')} />
            </FormField>
            <div className="flex flex-col gap-2" role="group" aria-labelledby="titulo-noches">
              <span id="titulo-noches" className="text-sm font-medium text-foreground">
                Refugio de cada noche <span className="text-destructive">*</span>
              </span>
              {datos.refugiosPorNoche.map((valor, i) => {
                const fecha = sumarDias(datos.fechaInicio, i);
                return (
                  <div key={i} className="flex items-center gap-3 rounded-xl border border-border bg-muted/40 px-3 py-2">
                    <span className="w-20 shrink-0 text-xs font-semibold text-muted-foreground">
                      Noche {i + 1}
                      <span className="block font-normal">{fecha.slice(8, 10)}/{fecha.slice(5, 7)}</span>
                    </span>
                    <select
                      aria-label={`Refugio de la noche ${i + 1}`}
                      value={valor}
                      onChange={cambiarNoche(i)}
                      aria-invalid={errores.refugiosPorNoche && !valor ? true : undefined}
                      className={cn(CLASE_SELECT, 'min-w-0 flex-1')}
                    >
                      <option value="">Seleccioná un refugio</option>
                      {refugiosOrdenados.map((r) => (
                        <option key={r.idRefugio} value={r.idRefugio}>
                          {r.nombre} — {r.plazasLibres} de {r.capacidadPlazas} plazas libres
                        </option>
                      ))}
                    </select>
                  </div>
                );
              })}
              {errores.refugiosPorNoche && (
                <p role="alert" className="text-xs text-destructive">
                  {errores.refugiosPorNoche}
                </p>
              )}
            </div>
          </>
        )}
      </Seccion>

      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Link to="/admin/salidas" className={cn(buttonVariants({ variant: 'outline' }), 'h-10 px-5')}>
          Cancelar
        </Link>
        <Button type="submit" className="h-10 px-5" disabled={enviando}>
          {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
          {editando ? 'Guardar cambios' : 'Planificar salida'}
        </Button>
      </div>
    </form>
  );
}
