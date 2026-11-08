import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, Loader2, ShieldCheck } from 'lucide-react';
import { AccesoLayout } from '@/components/layouts/acceso-layout';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { FormField } from '@/components/form-field';
import { obtenerReservaParticipante } from '@/api/participante';

/**
 * CU-19 Consultar y gestionar reserva, acceso del Participante (pasos 1-2):
 * ingresa el N.º de reserva y su DNI, sin necesidad de tener cuenta.
 */
export default function AccesoParticipantePagina() {
  const navigate = useNavigate();
  const [idReserva, setIdReserva] = useState('');
  const [dni, setDni] = useState('');
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    const nuevos = {};
    if (!idReserva.trim()) nuevos.idReserva = 'Ingresá el número de reserva.';
    if (!dni.trim()) nuevos.dni = 'Ingresá tu DNI.';
    setErrores(nuevos);
    setError('');
    if (Object.keys(nuevos).length) return;
    setEnviando(true);
    try {
      await obtenerReservaParticipante(idReserva.trim(), dni.trim());
      navigate(`/participante/reservas/${idReserva.trim()}?dni=${dni.trim()}`);
    } catch (err) {
      setError(err.message);
      setEnviando(false);
    }
  };

  return (
    <AccesoLayout
      derecha={
        <Link to="/ingresar" className="text-sm font-medium text-foreground hover:underline">
          Iniciar sesión
        </Link>
      }
    >
      <section aria-labelledby="titulo-acceso" className="mx-auto flex max-w-[440px] flex-col gap-5">
        <header>
          <h1 id="titulo-acceso" className="font-serif text-3xl font-semibold text-foreground">
            Consultar mi reserva
          </h1>
          <p className="mt-2 text-pretty text-muted-foreground">
            Si te anotaron como participante en una reserva, podés consultarla, cargar tu certificado médico y pagar el
            saldo pendiente sin crear una cuenta.
          </p>
        </header>

        {error && (
          <p className="flex items-start gap-2 rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive" role="alert">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
          </p>
        )}

        <form onSubmit={enviar} noValidate className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-6">
          <FormField id="p-reserva" label="N.º de reserva" required error={errores.idReserva}
            hint="Figura en el correo que te llegó de PataTrek.">
            <Input
              id="p-reserva"
              inputMode="numeric"
              autoComplete="off"
              value={idReserva}
              onChange={(e) => setIdReserva(e.target.value.replace(/\D/g, ''))}
              aria-invalid={errores.idReserva ? true : undefined}
              className="h-11 bg-background"
            />
          </FormField>
          <FormField id="p-dni" label="Tu DNI" required error={errores.dni} hint="El que figura en la reserva, sin puntos.">
            <Input
              id="p-dni"
              inputMode="numeric"
              autoComplete="off"
              value={dni}
              onChange={(e) => setDni(e.target.value.replace(/\D/g, ''))}
              aria-invalid={errores.dni ? true : undefined}
              className="h-11 bg-background"
            />
          </FormField>
          <Button type="submit" className="h-11 font-semibold" disabled={enviando}>
            {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
            Consultar reserva
          </Button>
          <p className="flex items-start gap-2 text-xs text-muted-foreground">
            <ShieldCheck className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
            Por seguridad, después de dos intentos con datos incorrectos el acceso a la reserva se bloquea por 5 minutos.
          </p>
        </form>
      </section>
    </AccesoLayout>
  );
}
