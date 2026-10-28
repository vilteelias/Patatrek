import { useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { AlertCircle, Eye, EyeOff, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { FormField } from '@/components/form-field';
import { INICIO_POR_ROL, useSesion } from '@/sesion/SesionProvider';
import { PantallaAcceso, rutaSegura } from './PantallaAcceso';

// Cuentas cargadas por los datos de ejemplo (DatosInicialesUsuarios). Solo para la demostración.
const CUENTAS_DEMO = [
  { rol: 'Cliente', correo: 'sofia.ibarra@mail.com' },
  { rol: 'Guía', correo: 'martin.almeida@patatrek.com' },
  { rol: 'Administrador', correo: 'admin@patatrek.com' },
];
const CLAVE_DEMO = 'Patatrek2026';

/** CU-02 Iniciar sesión (prototipo pág. 3). */
export default function IngresarPagina() {
  const { usuario, cargando, ingresar } = useSesion();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const volver = rutaSegura(params.get('volver'));

  const [correo, setCorreo] = useState('');
  const [clave, setClave] = useState('');
  const [verClave, setVerClave] = useState(false);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  // Si ya tiene sesión, no tiene sentido mostrar el login
  if (!cargando && usuario && !enviando) {
    return <Navigate to={volver ?? INICIO_POR_ROL[usuario.rol] ?? '/'} replace />;
  }

  const enviar = async (e) => {
    e.preventDefault();
    const nuevos = {};
    if (!correo.trim()) nuevos.correoElectronico = 'Ingresá tu correo electrónico.';
    if (!clave) nuevos.contrasenia = 'Ingresá tu contraseña.';
    setErrores(nuevos);
    setError('');
    if (Object.keys(nuevos).length) return;

    setEnviando(true);
    try {
      const u = await ingresar(correo.trim(), clave);
      // El cliente vuelve a donde estaba (por ejemplo, reservar); guía y administrador van a su panel
      navigate(u.rol === 'CLIENTE' ? (volver ?? '/') : INICIO_POR_ROL[u.rol], { replace: true });
    } catch (err) {
      setErrores(err.errores ?? {});
      setError(err.message);
      setClave('');
      setEnviando(false);
    }
  };

  return (
    <PantallaAcceso>
      <main className="w-full max-w-[400px] rounded-2xl bg-background px-7 py-8 shadow-2xl sm:px-9">
        <h1 className="text-center font-serif text-3xl font-semibold text-foreground">Iniciar sesión</h1>
        <p className="mt-2 text-center text-sm text-muted-foreground">
          Ingresá con tu correo electrónico y contraseña para continuar.
        </p>

        {volver && !error && (
          <p className="mt-4 rounded-lg bg-secondary px-3 py-2 text-center text-xs text-secondary-foreground">
            Para continuar necesitás iniciar sesión.
          </p>
        )}
        {error && (
          <p className="mt-4 flex items-start gap-2 rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive" role="alert">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
          </p>
        )}

        <form onSubmit={enviar} noValidate className="mt-6 flex flex-col gap-4">
          <FormField id="correo" label="Correo electrónico" error={errores.correoElectronico}>
            <Input
              id="correo"
              type="email"
              autoComplete="email"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              placeholder="nombre@correo.com"
              aria-invalid={errores.correoElectronico ? true : undefined}
              className="h-11 bg-card"
            />
          </FormField>
          <FormField id="clave" label="Contraseña" error={errores.contrasenia}>
            <div className="relative">
              <Input
                id="clave"
                type={verClave ? 'text' : 'password'}
                autoComplete="current-password"
                value={clave}
                onChange={(e) => setClave(e.target.value)}
                placeholder="••••••••"
                aria-invalid={errores.contrasenia ? true : undefined}
                className="h-11 bg-card pr-11"
              />
              <button
                type="button"
                onClick={() => setVerClave((v) => !v)}
                className="absolute top-1/2 right-2 inline-flex size-8 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground hover:bg-muted"
                aria-label={verClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {verClave ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
              </button>
            </div>
          </FormField>
          <Button type="submit" className="mt-2 h-11 font-semibold" disabled={enviando}>
            {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
            Iniciar sesión
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-foreground">
          ¿No tenés cuenta?{' '}
          <Link
            to={`/registrarme${volver ? `?volver=${encodeURIComponent(volver)}` : ''}`}
            className="font-semibold text-warning hover:underline"
          >
            Registrate
          </Link>
        </p>

        <details className="mt-6 rounded-lg border border-dashed border-input px-3 py-2 text-xs text-muted-foreground">
          <summary className="cursor-pointer font-semibold">Cuentas de demostración</summary>
          <p className="mt-2">
            Contraseña de todas: <code className="font-semibold text-foreground">{CLAVE_DEMO}</code>
          </p>
          <ul className="mt-1 flex flex-col gap-1">
            {CUENTAS_DEMO.map((c) => (
              <li key={c.correo}>
                <button
                  type="button"
                  className="text-left underline-offset-2 hover:text-foreground hover:underline"
                  onClick={() => {
                    setCorreo(c.correo);
                    setClave(CLAVE_DEMO);
                    setErrores({});
                    setError('');
                  }}
                >
                  {c.rol}: {c.correo}
                </button>
              </li>
            ))}
          </ul>
        </details>
      </main>
    </PantallaAcceso>
  );
}
