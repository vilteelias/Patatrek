import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { CheckCircle2, ChevronRight, Star } from 'lucide-react';
import { Button, buttonVariants } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { EstadoReservaBadge } from '@/components/reserva/estado-reserva-badge';
import {
  CLASE_SELECT,
  ESTADO_PAGO,
  ESTADOS_RESERVA,
  formatearRangoFechas,
  hora,
  pesos,
} from '@/components/reserva/formato';
import { listarMisReservas } from '@/api/reservas';
import { listarValoracionesPendientes } from '@/api/valoraciones';
import { cn } from '@/lib/utils';
import { Aviso } from './Estados';

const SIN_FILTROS = { desde: '', hasta: '', estado: '' };

function Dato({ etiqueta, children }) {
  return (
    <div className="flex flex-col">
      <span className="text-xs text-muted-foreground">{etiqueta}</span>
      <span className="text-sm font-semibold text-foreground">{children}</span>
    </div>
  );
}

/** CU-21 Listar reservas realizadas: solo las reservas en las que el cliente figura como titular (prototipo pág. 13). */
export default function MisReservasPagina() {
  const location = useLocation();
  const navigate = useNavigate();
  const [aviso, setAviso] = useState(location.state?.aviso ?? '');
  const [filtros, setFiltros] = useState(SIN_FILTROS);
  const [reservas, setReservas] = useState(null);
  const [error, setError] = useState('');
  const [errorFiltro, setErrorFiltro] = useState('');
  const [pendientes, setPendientes] = useState([]);

  // CU-28: salidas finalizadas que el cliente todavía no valoró
  useEffect(() => {
    listarValoracionesPendientes()
      .then(setPendientes)
      .catch(() => setPendientes([]));
  }, []);

  // El aviso (por ejemplo "Cancelaste la reserva #12") no reaparece al recargar.
  useEffect(() => {
    if (location.state?.aviso) navigate(location.pathname, { replace: true, state: null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (filtros.desde && filtros.hasta && filtros.hasta < filtros.desde) {
      setErrorFiltro('La fecha "hasta" no puede ser anterior a la fecha "desde".');
      return;
    }
    setErrorFiltro('');
    let vigente = true;
    listarMisReservas(filtros)
      .then((r) => vigente && setReservas(r))
      .catch((e) => vigente && setError(e.message));
    return () => {
      vigente = false;
    };
  }, [filtros]);

  if (error) return <Aviso titulo="No pudimos cargar tus reservas" mensaje={error} />;

  const cambiar = (campo) => (e) => setFiltros((f) => ({ ...f, [campo]: e.target.value }));
  const hayFiltros = filtros.desde || filtros.hasta || filtros.estado;

  return (
    <div className="mx-auto flex max-w-[860px] flex-col gap-5">
      <div>
        <p className="text-sm text-muted-foreground">Mi cuenta</p>
        <h1 className="font-serif text-3xl font-semibold text-foreground">Mis reservas</h1>
        <p className="text-muted-foreground">Se muestran únicamente las reservas en las que figurás como titular.</p>
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

      {pendientes.length > 0 && (
        <section aria-labelledby="titulo-valoraciones" className="rounded-2xl border border-warning/30 bg-secondary px-5 py-4">
          <h2 id="titulo-valoraciones" className="flex items-center gap-2 font-semibold text-secondary-foreground">
            <Star className="size-4 fill-warning text-warning" aria-hidden="true" />
            Valoraciones pendientes
          </h2>
          <ul className="mt-2 flex flex-col divide-y divide-border">
            {pendientes.map((v) => (
              <li key={`${v.idReserva}-${v.dni}`} className="flex flex-wrap items-center justify-between gap-3 py-2.5">
                <span className="text-sm text-foreground">
                  ¿Cómo fue <strong>{v.excursion}</strong>?{' '}
                  <span className="text-muted-foreground">
                    Salida del {formatearRangoFechas(v.fechaInicio, v.fechaFinalizacion)}
                  </span>
                </span>
                <Link
                  to={`/valorar?reserva=${v.idReserva}&dni=${v.dni}`}
                  className={cn(buttonVariants(), 'h-9 px-4')}
                >
                  Valorar
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section aria-label="Filtros" className="rounded-2xl border border-border bg-card px-5 py-4">
        <div className="grid gap-3 sm:grid-cols-[1fr_1fr_1.2fr_auto] sm:items-end">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="f-desde">Salida desde</Label>
            <Input id="f-desde" type="date" value={filtros.desde} onChange={cambiar('desde')} className="h-10 bg-card" />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="f-hasta">Salida hasta</Label>
            <Input
              id="f-hasta"
              type="date"
              value={filtros.hasta}
              onChange={cambiar('hasta')}
              aria-invalid={errorFiltro ? true : undefined}
              className="h-10 bg-card"
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="f-estado">Estado de reserva</Label>
            <select id="f-estado" value={filtros.estado} onChange={cambiar('estado')} className={CLASE_SELECT}>
              <option value="">Todos los estados</option>
              {ESTADOS_RESERVA.map((e) => (
                <option key={e.valor} value={e.valor}>
                  {e.etiqueta}
                </option>
              ))}
            </select>
          </div>
          <Button
            type="button"
            variant="outline"
            className="h-10 px-4"
            onClick={() => setFiltros(SIN_FILTROS)}
            disabled={!hayFiltros}
          >
            Limpiar filtros
          </Button>
        </div>
        {errorFiltro && (
          <p className="mt-2 text-xs text-destructive" role="alert">
            {errorFiltro}
          </p>
        )}
      </section>

      {reservas === null ? (
        <p className="py-8 text-muted-foreground">Cargando tus reservas…</p>
      ) : reservas.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-input px-6 py-12 text-center">
          <p className="text-muted-foreground">
            {hayFiltros ? 'No hay reservas para los filtros seleccionados.' : 'Todavía no hiciste ninguna reserva.'}
          </p>
          {!hayFiltros && (
            <Link to="/" className={cn(buttonVariants(), 'h-10 px-5')}>
              Ver excursiones
            </Link>
          )}
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {reservas.map((r) => (
            <li key={r.idReserva} className="rounded-2xl border border-border bg-card px-5 py-4">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <p className="font-semibold text-foreground">{r.excursion}</p>
                  <p className="text-xs text-muted-foreground">
                    Reserva #{r.idReserva} · {formatearRangoFechas(r.fechaInicio, r.fechaFinalizacion)} ·{' '}
                    {r.cantidadParticipantes === 1 ? '1 plaza' : `${r.cantidadParticipantes} plazas`}
                  </p>
                </div>
                <EstadoReservaBadge estado={r.estadoReserva} />
              </div>
              <div className="mt-3 flex flex-wrap gap-x-8 gap-y-2 border-t border-border pt-3">
                <Dato etiqueta="Importe total">{pesos(r.montoTotal)}</Dato>
                {['SENADA', 'PENDIENTE_PAGO'].includes(r.estadoReserva) && (
                  <Dato etiqueta="Importe pendiente">{pesos(r.saldoPendiente)}</Dato>
                )}
                <Dato etiqueta="Inicio">
                  {formatearRangoFechas(r.fechaInicio, r.fechaInicio)}, {hora(r.horaInicio)} h
                </Dato>
                <Dato etiqueta="Estado del pago">{ESTADO_PAGO[r.estadoPago] ?? r.estadoPago}</Dato>
              </div>
              <div className="mt-3 flex justify-end">
                <Link
                  to={`/reservas/${r.idReserva}`}
                  className="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline"
                >
                  Gestionar reserva <ChevronRight className="size-4" aria-hidden="true" />
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
