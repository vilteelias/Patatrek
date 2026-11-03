import { useEffect, useState } from 'react';
import { Link, useOutletContext, useSearchParams } from 'react-router-dom';
import { AlertCircle, CheckCircle2, Loader2 } from 'lucide-react';
import { Alert, AlertTitle } from '@/components/ui/alert';
import { Button, buttonVariants } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { FormField, RequiredLegend } from '@/components/form-field';
import { EstrellasFijas, SelectorEstrellas } from '@/components/valoracion/estrellas';
import { formatearRangoFechas } from '@/components/reserva/formato';
import { obtenerFormularioValoracion, registrarValoracion } from '@/api/valoraciones';
import { cn } from '@/lib/utils';

const MAXIMO = 1000;

/**
 * CU-28 Registrar valoración de excursión (prototipo pág. 7 y 18).
 * - El cliente llega desde "Mis reservas" (valoraciones pendientes).
 * - El participante sin cuenta llega con el enlace del correo: /valorar?reserva=ID&dni=DNI.
 *   Si entra sin el enlace, ingresa su N.º de reserva y DNI.
 */
export default function ValorarPagina() {
  const [params, setParams] = useSearchParams();
  const { cliente } = useOutletContext() ?? {};
  const idReserva = params.get('reserva') ?? '';
  const dni = params.get('dni') ?? '';

  const [formulario, setFormulario] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [errorCarga, setErrorCarga] = useState(null); // { estado, mensaje }

  const [calificacion, setCalificacion] = useState(0);
  const [comentario, setComentario] = useState('');
  const [errores, setErrores] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [recienEnviada, setRecienEnviada] = useState(false);

  useEffect(() => {
    if (!idReserva || !dni) {
      setFormulario(null);
      return;
    }
    setCargando(true);
    setErrorCarga(null);
    obtenerFormularioValoracion(idReserva, dni)
      .then(setFormulario)
      .catch((e) => setErrorCarga({ estado: e.estado, mensaje: e.message }))
      .finally(() => setCargando(false));
  }, [idReserva, dni]);

  const enviar = async (e) => {
    e.preventDefault();
    if (!calificacion) {
      // 5a: se resalta el campo y se muestra el mensaje
      setErrores({ calificacion: 'Debe seleccionar una calificación entre 1 y 5 estrellas para continuar' });
      return;
    }
    setEnviando(true);
    setErrores({});
    try {
      const r = await registrarValoracion({ idReserva, dni, calificacion, comentario: comentario.trim() });
      setFormulario(r);
      setRecienEnviada(true);
    } catch (err) {
      setErrores({ ...err.errores, general: err.message });
    } finally {
      setEnviando(false);
    }
  };

  // ---------- Sin datos: el participante se identifica ----------
  if (!idReserva || !dni || errorCarga?.estado === 404) {
    return (
      <Identificacion
        inicialReserva={idReserva}
        inicialDni={dni}
        error={errorCarga?.estado === 404 ? errorCarga.mensaje : ''}
        onIngresar={(r, d) => setParams({ reserva: r, dni: d })}
      />
    );
  }

  if (cargando || (!formulario && !errorCarga)) {
    return (
      <p className="flex items-center gap-2 py-12 text-muted-foreground">
        <Loader2 className="size-4 animate-spin" aria-hidden="true" /> Cargando…
      </p>
    );
  }

  return (
    <div className="mx-auto flex max-w-[600px] flex-col gap-5">
      <div>
        <p className="text-sm text-muted-foreground">
          {cliente ? (
            <>
              <Link to="/mis-reservas" className="hover:text-foreground">
                Mis reservas
              </Link>{' '}
              / Registrar valoración
            </>
          ) : (
            'Registrar valoración'
          )}
        </p>
        <h1 className="font-serif text-3xl font-semibold text-foreground">¿Cómo fue tu experiencia?</h1>
      </div>

      {errorCarga && (
        <Alert variant="destructive" className="rounded-xl">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>{errorCarga.mensaje}</AlertTitle>
        </Alert>
      )}

      {formulario && (
        <>
          <dl className="rounded-2xl border border-border bg-card px-5 py-4 text-sm sm:px-6">
            <div className="flex justify-between gap-4 py-1">
              <dt className="text-muted-foreground">Excursión</dt>
              <dd className="text-right font-semibold">{formulario.excursion}</dd>
            </div>
            <div className="flex justify-between gap-4 py-1">
              <dt className="text-muted-foreground">Salida</dt>
              <dd className="text-right font-semibold">
                {formatearRangoFechas(formulario.fechaInicio, formulario.fechaFinalizacion)}
              </dd>
            </div>
            <div className="flex justify-between gap-4 py-1">
              <dt className="text-muted-foreground">Participante</dt>
              <dd className="text-right">{formulario.participante}</dd>
            </div>
            <div className="flex items-center justify-between gap-4 py-1">
              <dt className="text-muted-foreground">Tu acreditación</dt>
              <dd>
                <span className="rounded-full bg-accent px-2.5 py-0.5 text-xs font-semibold text-accent-foreground">
                  {formulario.acreditacion}
                </span>
              </dd>
            </div>
          </dl>

          {formulario.yaValorada ? (
            <section
              className="flex flex-col items-center gap-3 rounded-2xl border border-primary/30 bg-accent px-6 py-8 text-center"
              role="status"
            >
              <CheckCircle2 className="size-10 text-primary" aria-hidden="true" />
              <h2 className="font-serif text-2xl font-semibold text-foreground">
                {recienEnviada ? '¡Gracias por tu valoración!' : 'Ya registraste tu valoración'}
              </h2>
              <EstrellasFijas valor={formulario.calificacion} tamano="size-7" />
              {formulario.comentario && (
                <p className="max-w-md text-sm text-accent-foreground italic">“{formulario.comentario}”</p>
              )}
              <p className="max-w-md text-sm text-muted-foreground">
                {recienEnviada
                  ? 'Te enviamos un correo de confirmación. Tu opinión nos ayuda a mejorar cada salida.'
                  : 'Solo se admite una valoración por participante para cada salida.'}
              </p>
              {cliente && (
                <Link to="/mis-reservas" className={cn(buttonVariants({ variant: 'outline' }), 'mt-1 h-10 bg-card px-5')}>
                  Volver a mis reservas
                </Link>
              )}
            </section>
          ) : (
            <form
              onSubmit={enviar}
              noValidate
              className="flex flex-col gap-5 rounded-2xl border border-border bg-card px-5 py-6 sm:px-8"
            >
              <RequiredLegend />
              <div className="flex flex-col items-center gap-1">
                <p className="text-sm font-medium text-foreground">
                  Tu calificación <span className="text-destructive" aria-hidden="true">*</span>
                </p>
                <SelectorEstrellas
                  valor={calificacion}
                  onChange={(n) => {
                    setCalificacion(n);
                    setErrores(({ calificacion: _c, ...resto }) => resto);
                  }}
                  invalido={!!errores.calificacion}
                  describedBy={errores.calificacion ? 'error-calificacion' : undefined}
                />
                {errores.calificacion && (
                  <p id="error-calificacion" role="alert" className="text-center text-sm text-destructive">
                    {errores.calificacion}
                  </p>
                )}
              </div>

              <FormField
                id="comentario"
                label="Comentario"
                optional
                error={errores.comentario}
                hint={`${comentario.length}/${MAXIMO} caracteres`}
              >
                <Textarea
                  id="comentario"
                  value={comentario}
                  maxLength={MAXIMO}
                  onChange={(e) => setComentario(e.target.value)}
                  placeholder="Contanos cómo fue tu experiencia en la excursión…"
                  className="min-h-24 bg-card"
                />
              </FormField>

              {errores.general && !errores.calificacion && (
                <p className="text-sm text-destructive" role="alert">
                  {errores.general}
                </p>
              )}

              <div className="flex justify-end">
                <Button type="submit" className="h-11 px-6 font-semibold" disabled={enviando}>
                  {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
                  Enviar valoración
                </Button>
              </div>
            </form>
          )}
        </>
      )}
    </div>
  );
}

/** Acceso del participante sin cuenta: N.º de reserva + DNI (CU-28 paso 2). */
function Identificacion({ inicialReserva, inicialDni, error, onIngresar }) {
  const [reserva, setReserva] = useState(inicialReserva);
  const [dni, setDni] = useState(inicialDni);
  const [errores, setErrores] = useState({});

  const ingresar = (e) => {
    e.preventDefault();
    const nuevos = {};
    if (!reserva.trim()) nuevos.reserva = 'Ingresá el número de reserva.';
    else if (!/^\d+$/.test(reserva.trim())) nuevos.reserva = 'El número de reserva tiene solo números.';
    if (!dni.trim()) nuevos.dni = 'Ingresá tu DNI.';
    else if (!/^\d{7,8}$/.test(dni.trim())) nuevos.dni = 'El DNI debe tener 7 u 8 números, sin puntos.';
    setErrores(nuevos);
    if (Object.keys(nuevos).length === 0) onIngresar(reserva.trim(), dni.trim());
  };

  return (
    <div className="mx-auto flex max-w-[460px] flex-col gap-5">
      <div>
        <p className="text-sm text-muted-foreground">Registrar valoración</p>
        <h1 className="font-serif text-3xl font-semibold text-foreground">¿Cómo fue tu experiencia?</h1>
        <p className="mt-1 text-muted-foreground">
          Ingresá el número de reserva y tu DNI. Los encontrás en el correo que te enviamos al terminar la salida.
        </p>
      </div>
      {error && (
        <Alert variant="destructive" className="rounded-xl">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>{error}</AlertTitle>
        </Alert>
      )}
      <form onSubmit={ingresar} noValidate className="flex flex-col gap-4 rounded-2xl border border-border bg-card px-5 py-6 sm:px-7">
        <RequiredLegend />
        <FormField id="v-reserva" label="N.º de reserva" required error={errores.reserva}>
          <Input
            id="v-reserva"
            inputMode="numeric"
            value={reserva}
            onChange={(e) => setReserva(e.target.value)}
            aria-invalid={errores.reserva ? true : undefined}
            className="h-10 bg-card"
          />
        </FormField>
        <FormField id="v-dni" label="DNI" required error={errores.dni}>
          <Input
            id="v-dni"
            inputMode="numeric"
            maxLength={8}
            value={dni}
            onChange={(e) => setDni(e.target.value)}
            placeholder="Sin puntos"
            aria-invalid={errores.dni ? true : undefined}
            className="h-10 bg-card"
          />
        </FormField>
        <Button type="submit" className="h-11 font-semibold">
          Continuar
        </Button>
      </form>
    </div>
  );
}
