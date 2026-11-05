import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { CalendarDays, Clock, Home, Mountain, Route, Users } from 'lucide-react';
import { buttonVariants } from '@/components/ui/button';
import { listarSalidasDeExcursion, obtenerExcursion } from '@/api/reservas';
import { formatearRangoFechas, hora, pesos } from '@/components/reserva/formato';
import { DIFICULTAD, dias } from '@/utilidades/formato';
import { cn } from '@/lib/utils';
import { Aviso, Cargando, Volver } from './Estados';

const FONDOS = [
  'linear-gradient(135deg, #2c6a54, #1d4a3c)',
  'linear-gradient(135deg, #3d7a64, #163a2f)',
  'linear-gradient(135deg, #c77446, #8f3f22)',
  'linear-gradient(135deg, #7d8a5a, #444f2f)',
];

const COLOR_DIFICULTAD = {
  BAJA: 'bg-accent text-accent-foreground',
  MEDIA: 'bg-secondary text-secondary-foreground',
  ALTA: 'bg-destructive/10 text-destructive',
};

/** Detalle de una excursión con sus próximas salidas, desde donde el cliente inicia la reserva (CU-18). */
export default function ExcursionDetallePagina() {
  const { id } = useParams();
  const [excursion, setExcursion] = useState(null);
  const [salidas, setSalidas] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([obtenerExcursion(id), listarSalidasDeExcursion(id)])
      .then(([e, s]) => {
        setExcursion(e);
        setSalidas(s);
      })
      .catch((e) => setError(e.message));
  }, [id]);

  if (error) return <Aviso titulo="No pudimos cargar la excursión" mensaje={error} />;
  if (!excursion) return <Cargando texto="Cargando excursión…" />;

  return (
    <div className="mx-auto flex max-w-[860px] flex-col gap-6">
      <Volver to="/">Excursiones</Volver>

      <article className="overflow-hidden rounded-2xl border border-border bg-card">
        <div className="h-48 sm:h-64" style={{ background: FONDOS[excursion.idExcursion % FONDOS.length] }}>
          {excursion.imagenPromocional && (
            <img
              src={excursion.imagenPromocional}
              alt={`Imagen de ${excursion.nombre}`}
              className="size-full object-cover"
            />
          )}
        </div>
        <div className="flex flex-col gap-4 px-5 py-6 sm:px-8">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <h1 className="font-serif text-3xl font-semibold text-foreground">{excursion.nombre}</h1>
            <span
              className={cn(
                'rounded-full px-3 py-1 text-xs font-semibold',
                COLOR_DIFICULTAD[excursion.dificultad],
              )}
            >
              Dificultad {DIFICULTAD[excursion.dificultad].toLowerCase()}
            </span>
          </div>
          <p className="text-muted-foreground">{excursion.descripcionGeneral}</p>
          {(excursion.recorridoBase || excursion.descripcionTecnica) && (
            <div className="flex flex-col gap-3 rounded-xl bg-muted/60 px-4 py-3">
              {excursion.recorridoBase && (
                <div className="flex gap-3">
                  <Route className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
                  <div className="flex flex-col gap-0.5">
                    <h2 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Recorrido</h2>
                    <p className="text-sm text-foreground">{excursion.recorridoBase}</p>
                  </div>
                </div>
              )}
              {excursion.descripcionTecnica && (
                <div className="flex gap-3">
                  <Mountain className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
                  <div className="flex flex-col gap-0.5">
                    <h2 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                      Información técnica
                    </h2>
                    <p className="text-sm text-foreground">{excursion.descripcionTecnica}</p>
                  </div>
                </div>
              )}
            </div>
          )}
          <ul className="flex flex-wrap gap-2 text-xs">
            <li className="rounded-md bg-muted px-2.5 py-1">{dias(excursion.duracion)}</li>
            {excursion.requiereCertificado && (
              <li className="rounded-md bg-muted px-2.5 py-1">Certificado médico requerido</li>
            )}
            <li className="rounded-md bg-muted px-2.5 py-1">
              {excursion.aceptaMenores ? 'Apta para menores' : 'Solo mayores de 18'}
            </li>
          </ul>
          <p className="text-lg font-semibold text-foreground">
            {pesos(excursion.precio)} <span className="text-sm font-normal text-muted-foreground">por persona</span>
          </p>
        </div>
      </article>

      <section className="flex flex-col gap-3" aria-labelledby="titulo-salidas">
        <h2 id="titulo-salidas" className="font-serif text-2xl font-semibold text-foreground">
          Próximas salidas
        </h2>
        {salidas.length === 0 && (
          <p className="rounded-xl border border-dashed border-input px-5 py-8 text-center text-muted-foreground">
            Por ahora no hay salidas programadas para esta excursión.
          </p>
        )}
        {salidas.map((s) => (
          <div
            key={s.idSalida}
            className="flex flex-col gap-3 rounded-xl border border-border bg-card px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex flex-col gap-1.5">
              <p className="flex items-center gap-2 font-semibold text-foreground">
                <CalendarDays className="size-4 text-primary" aria-hidden="true" />
                {formatearRangoFechas(s.fechaInicio, s.fechaFinalizacion)}
              </p>
              <p className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
                <span className="inline-flex items-center gap-1.5">
                  <Clock className="size-3.5" aria-hidden="true" />
                  {hora(s.horaInicio)} a {hora(s.horaFinalizacion)} h
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Users className="size-3.5" aria-hidden="true" />
                  {s.cuposDisponibles === 1 ? '1 cupo disponible' : `${s.cuposDisponibles} cupos disponibles`}
                </span>
                {s.tieneRefugio && (
                  <span className="inline-flex items-center gap-1.5">
                    <Home className="size-3.5" aria-hidden="true" /> Con refugio
                  </span>
                )}
              </p>
              {s.recorrido && s.recorrido !== excursion.recorridoBase && (
                <p className="flex items-start gap-1.5 text-sm text-amber-800 dark:text-amber-300">
                  <Route className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
                  <span>
                    <span className="font-semibold">Recorrido modificado:</span> {s.recorrido}
                  </span>
                </p>
              )}
            </div>
            {s.reservable ? (
              <Link
                to={`/reservar/${s.idSalida}`}
                className={cn(buttonVariants(), 'h-10 px-5 font-semibold')}
              >
                Reservar
              </Link>
            ) : (
              <p className="max-w-[260px] text-sm text-muted-foreground sm:text-right">{s.motivoNoReservable}</p>
            )}
          </div>
        ))}
      </section>
    </div>
  );
}
