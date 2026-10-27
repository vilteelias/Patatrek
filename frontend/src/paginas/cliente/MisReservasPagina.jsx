import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import { buttonVariants } from '@/components/ui/button';
import { EstadoReservaBadge } from '@/components/reserva/estado-reserva-badge';
import { formatearRangoFechas, pesos } from '@/components/reserva/formato';
import { listarMisReservas } from '@/api/reservas';
import { cn } from '@/lib/utils';
import { Aviso, Cargando } from './Estados';

/** Listado de reservas del cliente (entrada a CU-19 Consultar y gestionar reserva). */
export default function MisReservasPagina() {
  const [reservas, setReservas] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    listarMisReservas()
      .then(setReservas)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <Aviso titulo="No pudimos cargar tus reservas" mensaje={error} />;
  if (!reservas) return <Cargando texto="Cargando tus reservas…" />;

  return (
    <div className="mx-auto flex max-w-[860px] flex-col gap-5">
      <div>
        <h1 className="font-serif text-3xl font-semibold text-foreground">Mis reservas</h1>
        <p className="text-muted-foreground">Consultá el estado de tus reservas y pagá el saldo pendiente.</p>
      </div>

      {reservas.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-input px-6 py-12 text-center">
          <p className="text-muted-foreground">Todavía no hiciste ninguna reserva.</p>
          <Link to="/" className={cn(buttonVariants(), 'h-10 px-5')}>
            Ver excursiones
          </Link>
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {reservas.map((r) => (
            <li key={r.idReserva}>
              <Link
                to={`/reservas/${r.idReserva}`}
                className="flex items-center gap-4 rounded-xl border border-border bg-card px-5 py-4 transition-colors hover:border-input hover:bg-muted/50"
              >
                <div className="flex min-w-0 flex-1 flex-col gap-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-foreground">{r.excursion}</span>
                    <EstadoReservaBadge estado={r.estadoReserva} />
                  </div>
                  <p className="text-sm text-muted-foreground">
                    Reserva #{r.idReserva} · {formatearRangoFechas(r.fechaInicio, r.fechaFinalizacion)} ·{' '}
                    {r.cantidadParticipantes === 1 ? '1 participante' : `${r.cantidadParticipantes} participantes`}
                  </p>
                </div>
                <div className="hidden text-right text-sm sm:block">
                  <p className="font-semibold text-foreground">{pesos(r.montoTotal)}</p>
                  {Number(r.saldoPendiente) > 0 && r.estadoReserva === 'SENADA' && (
                    <p className="text-muted-foreground">Saldo {pesos(r.saldoPendiente)}</p>
                  )}
                </div>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
