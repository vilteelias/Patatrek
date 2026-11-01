import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { AlertCircle, Clock, Home, Tent, TimerOff } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button, buttonVariants } from '@/components/ui/button';
import { RequiredLegend } from '@/components/form-field';
import { CabeceraSalida } from '@/components/reserva/cabecera-salida';
import { PasosReserva } from '@/components/reserva/pasos-reserva';
import { SelectorCantidad } from '@/components/reserva/selector-cantidad';
import { FormularioParticipante } from '@/components/reserva/formulario-participante';
import { FormularioPago } from '@/components/reserva/formulario-pago';
import { ContadorRetencion } from '@/components/reserva/contador-retencion';
import { PARTICIPANTE_VACIO, validarParticipantes } from '@/components/reserva/validacion';
import { ALOJAMIENTO, pesos } from '@/components/reserva/formato';
import {
  crearReserva,
  obtenerDatosParaReservar,
  obtenerReserva,
  pagarReserva,
  simularVencimiento,
} from '@/api/reservas';
import { cn } from '@/lib/utils';
import { Aviso, Cargando, Volver } from './Estados';

/** Tarjeta blanca de cada paso. */
function Panel({ titulo, subtitulo, children }) {
  return (
    <section className="rounded-2xl border border-border bg-card px-5 py-6 sm:px-8 sm:py-7">
      {titulo && <h2 className="text-lg font-semibold text-foreground">{titulo}</h2>}
      {subtitulo && <p className="mt-1 text-sm text-muted-foreground">{subtitulo}</p>}
      <div className={cn(titulo && 'mt-5')}>{children}</div>
    </section>
  );
}

function Botonera({ onAtras, onContinuar, textoContinuar = 'Continuar', deshabilitado }) {
  return (
    <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
      {onAtras ? (
        <Button type="button" variant="outline" className="h-11 px-5" onClick={onAtras}>
          Atrás
        </Button>
      ) : (
        <span />
      )}
      <Button type="button" className="h-11 px-6 font-semibold" onClick={onContinuar} disabled={deshabilitado}>
        {textoContinuar}
      </Button>
    </div>
  );
}

/** Paso al que pertenece un error devuelto por el backend. */
function pasoDelError(campo) {
  if (campo.startsWith('participantes')) return 'datos';
  if (campo === 'modalidadAlojamiento') return 'alojamiento';
  if (campo.startsWith('equipamiento')) return 'equipamiento';
  if (campo === 'cantidadParticipantes') return 'participantes';
  return null;
}

/**
 * CU-18 Realizar reserva — asistente de 6 pasos (prototipo pág. 9 a 11).
 * Alojamiento y Equipamiento aparecen solo si la salida los necesita.
 * Al confirmar el resumen se crea la reserva "Pendiente de pago" y se retienen los recursos 10 minutos.
 */
export default function ReservarPagina() {
  const { idSalida } = useParams();
  const navigate = useNavigate();
  const { cliente } = useOutletContext() ?? {};

  const [datos, setDatos] = useState(null);
  const [errorCarga, setErrorCarga] = useState('');
  const [paso, setPaso] = useState('participantes');
  const [alcanzado, setAlcanzado] = useState(0);
  const [cantidad, setCantidad] = useState(1);
  const [participantes, setParticipantes] = useState([{ ...PARTICIPANTE_VACIO }]);
  const [modalidad, setModalidad] = useState(null);
  const [equipo, setEquipo] = useState({});
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [reserva, setReserva] = useState(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    obtenerDatosParaReservar(idSalida)
      .then(setDatos)
      .catch((e) => setErrorCarga(e.message));
  }, [idSalida]);

  const pasos = useMemo(() => {
    if (!datos) return [];
    return [
      { clave: 'participantes', titulo: 'Participantes' },
      { clave: 'datos', titulo: 'Datos' },
      datos.multiDia && { clave: 'alojamiento', titulo: 'Alojamiento' },
      datos.equipamiento.length > 0 && { clave: 'equipamiento', titulo: 'Equipamiento' },
      { clave: 'resumen', titulo: 'Resumen' },
      { clave: 'pago', titulo: 'Pago' },
    ].filter(Boolean);
  }, [datos]);

  const indice = pasos.findIndex((p) => p.clave === paso);
  const habilitados = useMemo(
    () => new Set(reserva ? [] : pasos.slice(0, alcanzado + 1).map((p) => p.clave)),
    [pasos, alcanzado, reserva],
  );

  const ir = (clave) => {
    const i = pasos.findIndex((p) => p.clave === clave);
    setPaso(clave);
    setAlcanzado((a) => Math.max(a, i));
    setErrorGeneral('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  const siguiente = () => ir(pasos[indice + 1].clave);
  const anterior = () => ir(pasos[indice - 1].clave);

  const cambiarCantidad = (n) => {
    setCantidad(n);
    setParticipantes((lista) =>
      n > lista.length
        ? [...lista, ...Array.from({ length: n - lista.length }, () => ({ ...PARTICIPANTE_VACIO }))]
        : lista.slice(0, n),
    );
    setErrores({});
  };

  const cambiarParticipante = (i, campo, valor) => {
    setParticipantes((lista) => lista.map((p, j) => (j === i ? { ...p, [campo]: valor } : p)));
    const clave = `participantes[${i}].${campo}`;
    if (errores[clave]) {
      setErrores(({ [clave]: _quitado, ...resto }) => resto);
    }
  };

  const usarMisDatos = () => {
    if (!cliente) return;
    setParticipantes((lista) =>
      lista.map((p, j) =>
        j === 0
          ? {
              nombre: cliente.nombre ?? '',
              apellido: cliente.apellido ?? '',
              dni: cliente.dni ?? '',
              fechaNacimiento: cliente.fechaNacimiento ?? '',
              correoElectronico: cliente.correoElectronico ?? '',
              nroTelefono: cliente.telefono ?? '',
            }
          : p,
      ),
    );
    setErrores({});
  };

  // ---------- Validaciones por paso ----------
  const continuarDatos = () => {
    const encontrados = validarParticipantes(participantes, datos);
    setErrores(encontrados);
    if (Object.keys(encontrados).length > 0) {
      setErrorGeneral('Revisá los datos de los participantes marcados.');
      setTimeout(() => document.querySelector('[aria-invalid="true"]')?.focus(), 50);
      return;
    }
    siguiente();
  };

  const plazasInsuficientes = datos?.refugios.length > 0 && datos.plazasRefugioDisponibles < cantidad;
  const continuarAlojamiento = () => {
    if (!modalidad) {
      setErrores({ modalidadAlojamiento: 'Elegí si van a dormir en refugio o en carpa propia.' });
      return;
    }
    setErrores({});
    siguiente();
  };

  const cantidadEquipo = (id) => Math.min(equipo[id] ?? 0, cantidad);

  // ---------- Monto ----------
  const subtotalParticipantes = datos ? Number(datos.precioPorPersona) * cantidad : 0;
  const subtotalRefugio =
    datos && modalidad === 'REFUGIO' ? Number(datos.alojamientoRefugioPorPersona) * cantidad : 0;
  const total = subtotalParticipantes + subtotalRefugio;

  // ---------- Confirmar resumen: crea la reserva y retiene los recursos ----------
  const confirmarResumen = async () => {
    setEnviando(true);
    setErrorGeneral('');
    try {
      const creada = await crearReserva({
        idSalida: Number(idSalida),
        participantes: participantes.map((p) => ({
          ...p,
          dni: p.dni.trim(),
          nombre: p.nombre.trim(),
          apellido: p.apellido.trim(),
          correoElectronico: p.correoElectronico.trim(),
          nroTelefono: p.nroTelefono.trim(),
          fechaNacimiento: p.fechaNacimiento || null,
        })),
        modalidadAlojamiento: datos.multiDia ? modalidad : null,
        equipamiento: datos.equipamiento.map((e) => ({
          idEquipamiento: e.idEquipamiento,
          cantidad: cantidadEquipo(e.idEquipamiento),
        })),
      });
      setReserva(creada);
      ir('pago');
    } catch (e) {
      const errs = e.errores ?? {};
      setErrores(errs);
      setErrorGeneral(e.message);
      const destino = Object.keys(errs).map(pasoDelError).find(Boolean);
      if (destino && pasos.some((p) => p.clave === destino)) {
        setPaso(destino);
      }
      // Si cambió la disponibilidad (otro cliente reservó), se actualizan los datos de la salida.
      obtenerDatosParaReservar(idSalida).then(setDatos).catch(() => {});
    } finally {
      setEnviando(false);
    }
  };

  // ---------- Pago ----------
  const actualizarReserva = useCallback(() => {
    if (reserva) obtenerReserva(reserva.idReserva).then(setReserva).catch(() => {});
  }, [reserva]);

  const vencerAhora = async () => {
    try {
      setReserva(await simularVencimiento(reserva.idReserva));
    } catch (e) {
      setErrorGeneral(e.message);
    }
  };

  // La pasarela (ventana emergente) llama a CU-26 y muestra el resultado; al volver se abre el detalle.
  const pagar = async (modo) => {
    setErrorGeneral('');
    return pagarReserva(reserva.idReserva, modo);
  };
  const terminarPago = (resultado) => navigate(`/reservas/${reserva.idReserva}`, { state: { resultado } });

  // ---------- Render ----------
  if (errorCarga) {
    return <Aviso titulo="No se puede reservar esta salida" mensaje={errorCarga} />;
  }
  if (!datos) return <Cargando texto="Preparando la reserva…" />;

  const cancelada = reserva?.estadoReserva === 'CANCELADA';

  return (
    <div className="mx-auto flex max-w-[660px] flex-col gap-5">
      {!reserva && <Volver to={`/excursiones/${datos.idExcursion}`}>Elegir otra salida</Volver>}

      <CabeceraSalida
        excursion={datos.excursion}
        fechaInicio={datos.fechaInicio}
        fechaFinalizacion={datos.fechaFinalizacion}
        precio={datos.precioPorPersona}
        cupos={reserva ? undefined : datos.cuposDisponibles}
      />

      <PasosReserva pasos={pasos} actual={paso} habilitados={habilitados} onIr={ir} />

      {errorGeneral && (
        <Alert variant="destructive" className="rounded-xl">
          <AlertCircle aria-hidden="true" />
          <AlertTitle>{errorGeneral}</AlertTitle>
        </Alert>
      )}

      {/* 1 · Participantes */}
      {paso === 'participantes' && (
        <Panel titulo="¿Cuántos participantes van a ser?" subtitulo="Toda reserva debe incluir al menos un participante.">
          <SelectorCantidad
            id="cantidad"
            etiqueta="Cantidad de participantes"
            valor={cantidad}
            min={1}
            max={datos.cuposDisponibles}
            onChange={cambiarCantidad}
          />
          {cantidad >= datos.cuposDisponibles && (
            <p className="mt-3 text-sm text-muted-foreground">
              Llegaste al máximo de cupos disponibles para esta salida.
            </p>
          )}
          {errores.cantidadParticipantes && (
            <p className="mt-3 text-sm text-destructive" role="alert">
              {errores.cantidadParticipantes}
            </p>
          )}
          <Botonera onContinuar={siguiente} />
        </Panel>
      )}

      {/* 2 · Datos de los participantes (campos obligatorios marcados) */}
      {paso === 'datos' && (
        <div className="flex flex-col gap-4">
          <RequiredLegend />
          {participantes.map((p, i) => (
            <FormularioParticipante
              key={i}
              indice={i}
              valor={p}
              errores={errores}
              onChange={(campo, valor) => cambiarParticipante(i, campo, valor)}
              onUsarMisDatos={i === 0 && cliente ? usarMisDatos : undefined}
              notaEdad={datos.aceptaMenores ? undefined : 'Esta excursión no admite menores de 18 años.'}
            />
          ))}
          {datos.requiereCertificado && (
            <p className="rounded-xl bg-secondary px-4 py-3 text-sm text-secondary-foreground">
              Esta excursión requiere certificado médico. Cada participante podrá cargarlo después, desde la consulta
              de la reserva.
            </p>
          )}
          <Botonera onAtras={anterior} onContinuar={continuarDatos} />
        </div>
      )}

      {/* 3 · Alojamiento */}
      {paso === 'alojamiento' && (
        <Panel
          titulo="Alojamiento"
          subtitulo="La salida dura más de un día. Elegí dónde van a pasar las noches."
        >
          <div className="grid gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Modalidad de alojamiento">
            {[
              {
                valor: 'REFUGIO',
                icono: Home,
                titulo: 'Refugio',
                detalle:
                  datos.refugios.length === 0
                    ? 'Esta salida no tiene refugios asignados.'
                    : `${datos.refugios.map((r) => r.nombre).join(', ')} · ${pesos(datos.alojamientoRefugioPorPersona)} por persona`,
                deshabilitado: datos.refugios.length === 0 || plazasInsuficientes,
                aviso: plazasInsuficientes
                  ? `Quedan ${datos.plazasRefugioDisponibles} plazas: no alcanzan para ${cantidad} participantes.`
                  : datos.refugios.length > 0
                    ? `${datos.plazasRefugioDisponibles} plazas disponibles`
                    : null,
              },
              {
                valor: 'CARPA_PROPIA',
                icono: Tent,
                titulo: 'Carpa propia',
                detalle: 'Acampan con su propio equipo, sin costo adicional.',
              },
            ].map(({ valor, icono: Icono, titulo, detalle, deshabilitado, aviso }) => {
              const activo = modalidad === valor;
              return (
                <button
                  key={valor}
                  type="button"
                  role="radio"
                  aria-checked={activo}
                  disabled={deshabilitado}
                  onClick={() => {
                    setModalidad(valor);
                    setErrores({});
                  }}
                  className={cn(
                    'flex flex-col items-start gap-1 rounded-xl border px-4 py-4 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-60',
                    activo
                      ? 'border-primary bg-accent ring-2 ring-primary/30'
                      : 'border-border bg-card hover:border-input hover:bg-muted',
                  )}
                >
                  <span className="flex items-center gap-2 font-semibold text-foreground">
                    <Icono className="size-4 text-primary" aria-hidden="true" /> {titulo}
                  </span>
                  <span className="text-sm text-muted-foreground">{detalle}</span>
                  {aviso && (
                    <span className={cn('text-xs', plazasInsuficientes ? 'text-destructive' : 'text-muted-foreground')}>
                      {aviso}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
          {errores.modalidadAlojamiento && (
            <p className="mt-3 text-sm text-destructive" role="alert">
              {errores.modalidadAlojamiento}
            </p>
          )}
          <Botonera onAtras={anterior} onContinuar={continuarAlojamiento} />
        </Panel>
      )}

      {/* 4 · Equipamiento obligatorio */}
      {paso === 'equipamiento' && (
        <Panel
          titulo="Equipamiento obligatorio"
          subtitulo="Indicá cuántas unidades de cada ítem le solicitás en préstamo a la empresa. Lo que no pidas, cada participante lo lleva por su cuenta."
        >
          <ul className="flex flex-col gap-3">
            {datos.equipamiento.map((e) => {
              const max = Math.min(e.disponibles, cantidad);
              const error = errores[`equipamiento.${e.idEquipamiento}`];
              return (
                <li
                  key={e.idEquipamiento}
                  className={cn(
                    'flex flex-wrap items-center justify-between gap-3 rounded-xl border px-4 py-3',
                    error ? 'border-destructive' : 'border-border',
                  )}
                >
                  <div>
                    <p className="font-semibold text-foreground">{e.nombre}</p>
                    <p className="text-xs text-muted-foreground">
                      Stock disponible: {e.disponibles}
                      {e.disponibles > cantidad && ` · máximo ${cantidad} (uno por participante)`}
                    </p>
                    {error && (
                      <p className="mt-1 text-xs text-destructive" role="alert">
                        {error}
                      </p>
                    )}
                  </div>
                  <SelectorCantidad
                    etiqueta={e.nombre}
                    valor={cantidadEquipo(e.idEquipamiento)}
                    min={0}
                    max={max}
                    onChange={(n) => setEquipo((eq) => ({ ...eq, [e.idEquipamiento]: n }))}
                  />
                </li>
              );
            })}
          </ul>
          <Botonera onAtras={anterior} onContinuar={siguiente} />
        </Panel>
      )}

      {/* 5 · Resumen */}
      {paso === 'resumen' && (
        <Panel titulo="Resumen de tu reserva">
          <dl className="flex flex-col gap-2 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-muted-foreground">
                Participantes ({cantidad} × {pesos(datos.precioPorPersona)})
              </dt>
              <dd className="font-semibold">{pesos(subtotalParticipantes)}</dd>
            </div>
            {datos.multiDia && (
              <div className="flex justify-between gap-4">
                <dt className="text-muted-foreground">
                  Alojamiento: {modalidad === 'REFUGIO' ? datos.refugios.map((r) => r.nombre).join(', ') : ALOJAMIENTO[modalidad]}
                </dt>
                <dd className="font-semibold">{modalidad === 'REFUGIO' ? pesos(subtotalRefugio) : pesos(0)}</dd>
              </div>
            )}
            {datos.equipamiento
              .filter((e) => cantidadEquipo(e.idEquipamiento) > 0)
              .map((e) => (
                <div key={e.idEquipamiento} className="flex justify-between gap-4">
                  <dt className="text-muted-foreground">
                    Préstamo: {e.nombre} × {cantidadEquipo(e.idEquipamiento)}
                  </dt>
                  <dd className="text-muted-foreground">sin cargo</dd>
                </div>
              ))}
            <div className="mt-1 flex items-baseline justify-between gap-4 border-t border-border pt-3">
              <dt className="font-semibold text-foreground">Monto total</dt>
              <dd className="font-serif text-2xl font-semibold">{pesos(total)}</dd>
            </div>
          </dl>

          <div className="mt-5 rounded-xl border border-border bg-muted px-4 py-3 text-sm">
            <p className="mb-1 font-medium text-foreground">
              {cantidad === 1 ? 'Participante' : `${cantidad} participantes`}
            </p>
            <ul className="text-muted-foreground">
              {participantes.map((p) => (
                <li key={p.dni}>
                  {p.apellido}, {p.nombre} · DNI {p.dni}
                </li>
              ))}
            </ul>
          </div>

          <p className="mt-5 flex items-start gap-2 rounded-xl bg-secondary px-4 py-3 text-sm text-secondary-foreground">
            <Clock className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <span>
              Al continuar, el cupo, la plaza de refugio (si corresponde) y el equipamiento quedan retenidos por{' '}
              <strong>{datos.minutosRetencion} minutos</strong> mientras completás el pago.
            </span>
          </p>
          <Botonera
            onAtras={anterior}
            onContinuar={confirmarResumen}
            textoContinuar={enviando ? 'Reservando…' : 'Continuar al pago'}
            deshabilitado={enviando}
          />
        </Panel>
      )}

      {/* 6 · Pago (CU-26) */}
      {paso === 'pago' && reserva && !cancelada && (
        <Panel titulo="Realizar pago" subtitulo={`Reserva #${reserva.idReserva} · Monto total ${pesos(reserva.montoTotal)}`}>
          <ContadorRetencion
            segundos={reserva.segundosParaVencer}
            onVencido={actualizarReserva}
            className="mb-5"
          />
          <FormularioPago
            reserva={reserva}
            onConfirmar={pagar}
            onTerminar={terminarPago}
            onCancelado={actualizarReserva}
            onSimularVencimiento={vencerAhora}
          />
        </Panel>
      )}

      {paso === 'pago' && cancelada && (
        <Panel>
          <div className="flex flex-col items-center gap-3 py-4 text-center">
            <span className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
              <TimerOff className="size-6" aria-hidden="true" />
            </span>
            <h2 className="font-serif text-2xl font-semibold">Venció el plazo para pagar</h2>
            <p className="max-w-md text-sm text-muted-foreground">
              {reserva.motivoCancelacion ?? 'La reserva se canceló.'} No se realizó ningún cobro.
            </p>
            <Link to={`/excursiones/${datos.idExcursion}`} className={cn(buttonVariants(), 'mt-2 h-11 px-6')}>
              Volver a las salidas
            </Link>
          </div>
        </Panel>
      )}
    </div>
  );
}
