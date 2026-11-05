import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, Loader2, Plus, UserMinus } from 'lucide-react';
import { Button, buttonVariants } from '@/components/ui/button';
import { CLASE_SELECT, hora } from '@/components/reserva/formato';
import { Migas, ResumenSalida } from '@/components/salidas/resumen-salida';
import { asignarAyudantes, obtenerSalidaAdmin, opcionesSalida } from '@/api/salidas';
import { cn } from '@/lib/utils';

/** RF-10: variación = cupo base × 0,25 × N ayudantes (hacia abajo); cupo total = cupo base + variación. */
export function cupoConAyudantes(cupoBase, ayudantes) {
  return cupoBase + Math.floor(cupoBase * 0.25 * ayudantes);
}

function Tarjeta({ titulo, valor, detalle, tono }) {
  return (
    <div
      className={cn(
        'flex flex-col gap-1 rounded-2xl border px-4 py-3',
        tono === 'error' ? 'border-destructive/40 bg-destructive/10' : 'border-border bg-card',
      )}
    >
      <span className="text-xs text-muted-foreground">{titulo}</span>
      <span className={cn('text-2xl font-semibold', tono === 'error' ? 'text-destructive' : 'text-foreground')}>{valor}</span>
      {detalle && <span className="text-xs text-muted-foreground">{detalle}</span>}
    </div>
  );
}

/**
 * CU-12 Asignar personal y cupos a salida (prototipo pág. 28).
 * Se agregan o quitan guías ayudantes y el sistema recalcula el cupo total con la fórmula de RF-10,
 * respetando que no baje de las plazas ocupadas (4a) ni supere el cupo máximo (4b).
 */
export default function PersonalCuposPagina() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [salida, setSalida] = useState(null);
  const [guias, setGuias] = useState([]);
  const [ayudantes, setAyudantes] = useState([]);
  const [elegido, setElegido] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    obtenerSalidaAdmin(id)
      .then((s) => {
        setSalida(s);
        setAyudantes(s.ayudantes.map((a) => ({ idGuia: a.idGuia, nombre: a.nombre, capacidadEstandar: a.capacidadEstandar })));
        return opcionesSalida({
          fechaInicio: s.fechaInicio,
          horaInicio: hora(s.horaInicio),
          fechaFinalizacion: s.fechaFinalizacion,
          horaFinalizacion: hora(s.horaFinalizacion),
          excluirSalida: s.idSalida,
        });
      })
      .then((o) => setGuias(o.guias))
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

  const ocupados = salida.cuposOcupados;
  const total = cupoConAyudantes(salida.cupoBase, ayudantes.length);
  const variacion = total - salida.cupoBase;
  const bajoOcupados = total < ocupados;
  const sobreMaximo = total > salida.cupoMaximo;
  const proximo = cupoConAyudantes(salida.cupoBase, ayudantes.length + 1);
  const anterior = cupoConAyudantes(salida.cupoBase, Math.max(ayudantes.length - 1, 0));
  const idsActuales = new Set(ayudantes.map((a) => a.idGuia));
  const candidatos = guias.filter((g) => g.idGuia !== salida.guiaResponsable?.idGuia && !idsActuales.has(g.idGuia));
  const sinCambios =
    ayudantes.length === salida.ayudantes.length && salida.ayudantes.every((a) => idsActuales.has(a.idGuia));

  const agregar = () => {
    setError('');
    const g = guias.find((x) => String(x.idGuia) === elegido);
    if (!g) return setError('Elegí el guía que querés agregar.');
    if (!g.disponible) return setError(g.motivoNoDisponible);
    if (proximo > salida.cupoMaximo) {
      return setError(
        `No es posible agregar personal: el cupo resultante (${proximo}) superaría el cupo máximo permitido (${salida.cupoMaximo}).`,
      );
    }
    setAyudantes((l) => [...l, g]);
    setElegido('');
  };

  const quitar = (idGuia) => {
    setError('');
    if (anterior < ocupados) {
      return setError(
        `No es posible remover personal: las plazas ocupadas (${ocupados}) superan el cupo resultante (${anterior}).`,
      );
    }
    setAyudantes((l) => l.filter((a) => a.idGuia !== idGuia));
  };

  const guardar = async () => {
    setEnviando(true);
    setError('');
    try {
      const s = await asignarAyudantes(id, ayudantes.map((a) => a.idGuia));
      navigate('/admin/salidas', {
        state: {
          aviso: `Personal de la salida #${s.idSalida} actualizado: ${s.ayudantes.length} ${s.ayudantes.length === 1 ? 'guía ayudante' : 'guías ayudantes'}, cupo total ${s.cupoTotal}.`,
        },
      });
    } catch (e) {
      setError(e.errores?.idsGuias ?? e.message);
      setEnviando(false);
    }
  };

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-5">
      <div>
        <Migas salida={salida} pantalla="Personal y cupos" />
        <h1 className="font-serif text-3xl font-semibold text-foreground">Personal y cupos</h1>
        <p className="text-muted-foreground">
          Agregá o quitá guías ayudantes. Cada ayudante suma un 25% del cupo base (redondeado hacia abajo).
        </p>
      </div>

      <ResumenSalida salida={salida} />

      {error && (
        <p className="flex items-start gap-2 rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">
          <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
        </p>
      )}

      <section aria-label="Recálculo de cupos" className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Tarjeta titulo="Cupo base" valor={salida.cupoBase} detalle="Capacidad del guía responsable" />
        <Tarjeta
          titulo="Variación"
          valor={`+${variacion}`}
          detalle={`${salida.cupoBase} × 0,25 × ${ayudantes.length} ${ayudantes.length === 1 ? 'ayudante' : 'ayudantes'}`}
        />
        <Tarjeta
          titulo="Cupo total resultante"
          valor={total}
          detalle={`Máximo permitido: ${salida.cupoMaximo}`}
          tono={sobreMaximo || bajoOcupados ? 'error' : undefined}
        />
        <Tarjeta
          titulo="Plazas ocupadas"
          valor={ocupados}
          detalle={`Quedarían ${Math.max(total - ocupados, 0)} libres`}
          tono={bajoOcupados ? 'error' : undefined}
        />
      </section>

      <section className="rounded-2xl border border-border bg-card px-5 py-5 sm:px-6">
        <h2 className="font-semibold text-foreground">Guías asignados</h2>
        <ul className="mt-3 flex flex-col divide-y divide-border">
          <li className="flex items-center justify-between gap-3 py-2.5">
            <span className="min-w-0">
              <span className="block truncate font-medium text-foreground">{salida.guiaResponsable?.nombre ?? '—'}</span>
              <span className="text-xs text-muted-foreground">
                Responsable · capacidad {salida.guiaResponsable?.capacidadEstandar ?? salida.cupoBase}
              </span>
            </span>
            <span className="text-xs text-muted-foreground">Se cambia en Modificar salida</span>
          </li>
          {ayudantes.map((a) => (
            <li key={a.idGuia} className="flex items-center justify-between gap-3 py-2.5">
              <span className="min-w-0">
                <span className="block truncate font-medium text-foreground">{a.nombre}</span>
                <span className="text-xs text-muted-foreground">Ayudante</span>
              </span>
              <Button type="button" variant="outline" size="sm" onClick={() => quitar(a.idGuia)} aria-label={`Quitar a ${a.nombre}`}>
                <UserMinus aria-hidden="true" /> Quitar
              </Button>
            </li>
          ))}
          {ayudantes.length === 0 && <li className="py-2.5 text-sm text-muted-foreground">La salida no tiene guías ayudantes.</li>}
        </ul>

        <div className="mt-4 flex flex-col gap-2 border-t border-border pt-4 sm:flex-row sm:items-end">
          <label className="flex min-w-0 flex-1 flex-col gap-1.5 text-sm font-medium text-foreground" htmlFor="nuevo-ayudante">
            Agregar guía ayudante
            <select id="nuevo-ayudante" value={elegido} onChange={(e) => setElegido(e.target.value)} className={CLASE_SELECT}>
              <option value="">Seleccioná un guía</option>
              {candidatos.map((g) => (
                <option key={g.idGuia} value={g.idGuia} disabled={!g.disponible}>
                  {g.nombre}{g.disponible ? '' : ` — ${g.motivoNoDisponible}`}
                </option>
              ))}
            </select>
          </label>
          <Button type="button" variant="secondary" className="h-10" onClick={agregar}>
            <Plus aria-hidden="true" /> Agregar
          </Button>
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          Con un ayudante más el cupo sería {proximo}
          {proximo > salida.cupoMaximo ? ' (supera el máximo, no se puede agregar)' : ''}.
        </p>
      </section>

      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Link to="/admin/salidas" className={cn(buttonVariants({ variant: 'outline' }), 'h-10 px-5')}>
          Cancelar
        </Link>
        <Button type="button" className="h-10 px-5" onClick={guardar} disabled={enviando || sinCambios || sobreMaximo || bajoOcupados}>
          {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
          Guardar cambios
        </Button>
      </div>
    </div>
  );
}
