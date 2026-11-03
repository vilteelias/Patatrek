import { useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { AlertCircle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { FormField, RequiredLegend } from '@/components/form-field';
import { INICIO_POR_ROL, useSesion } from '@/sesion/SesionProvider';
import { PantallaAcceso, rutaSegura } from './PantallaAcceso';

const VACIO = {
  nombre: '',
  apellido: '',
  dni: '',
  fechaNacimiento: '',
  correoElectronico: '',
  telefono: '',
  contrasenia: '',
  confirmarContrasenia: '',
};

const CAMPOS = [
  { campo: 'nombre', label: 'Nombre', autoComplete: 'given-name' },
  { campo: 'apellido', label: 'Apellido', autoComplete: 'family-name' },
  { campo: 'dni', label: 'DNI', inputMode: 'numeric', maxLength: 8, placeholder: 'Sin puntos' },
  { campo: 'fechaNacimiento', label: 'Fecha de nacimiento', type: 'date', autoComplete: 'bday' },
  { campo: 'correoElectronico', label: 'Correo electrónico', type: 'email', autoComplete: 'email', placeholder: 'nombre@correo.com', ancho: true },
  { campo: 'telefono', label: 'Teléfono', type: 'tel', autoComplete: 'tel', placeholder: 'Ej. 2804-667788', ancho: true },
  { campo: 'contrasenia', label: 'Contraseña', type: 'password', autoComplete: 'new-password', hint: 'Al menos 8 caracteres, con letras y números.' },
  { campo: 'confirmarContrasenia', label: 'Repetir contraseña', type: 'password', autoComplete: 'new-password' },
];

/** Mismas reglas que el backend (AuthService), para avisar antes de enviar. */
function validar(d) {
  const e = {};
  const hoy = new Date().toISOString().slice(0, 10);
  if (!d.nombre.trim()) e.nombre = 'Ingresá tu nombre.';
  if (!d.apellido.trim()) e.apellido = 'Ingresá tu apellido.';
  if (!d.dni.trim()) e.dni = 'Ingresá tu DNI.';
  else if (!/^\d{7,8}$/.test(d.dni.trim())) e.dni = 'El DNI debe tener 7 u 8 números, sin puntos.';
  if (!d.fechaNacimiento) e.fechaNacimiento = 'Ingresá tu fecha de nacimiento.';
  else if (d.fechaNacimiento >= hoy) e.fechaNacimiento = 'La fecha de nacimiento debe ser anterior a hoy.';
  if (!d.correoElectronico.trim()) e.correoElectronico = 'Ingresá tu correo electrónico.';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.correoElectronico.trim()))
    e.correoElectronico = 'Ingresá un correo válido, por ejemplo nombre@mail.com.';
  if (!d.telefono.trim()) e.telefono = 'Ingresá tu teléfono.';
  else if (!/^[0-9+()\-\s]{6,20}$/.test(d.telefono.trim())) e.telefono = 'Ingresá solo números, espacios o guiones.';
  if (!d.contrasenia) e.contrasenia = 'Ingresá una contraseña.';
  else if (d.contrasenia.length < 8 || !/[A-Za-z]/.test(d.contrasenia) || !/\d/.test(d.contrasenia))
    e.contrasenia = 'La contraseña debe tener al menos 8 caracteres, con letras y números.';
  if (!d.confirmarContrasenia) e.confirmarContrasenia = 'Repetí la contraseña.';
  else if (d.contrasenia && d.contrasenia !== d.confirmarContrasenia) e.confirmarContrasenia = 'Las contraseñas no coinciden.';
  return e;
}

/** CU-01 Registrar cliente (RF-01). Todos los datos son obligatorios. */
export default function RegistrarmePagina() {
  const { usuario, cargando, registrarse } = useSesion();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const volver = rutaSegura(params.get('volver'));

  const [datos, setDatos] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (!cargando && usuario && !enviando) {
    return <Navigate to={INICIO_POR_ROL[usuario.rol] ?? '/'} replace />;
  }

  const cambiar = (campo) => (e) => {
    setDatos((d) => ({ ...d, [campo]: e.target.value }));
    if (errores[campo]) setErrores(({ [campo]: _x, ...resto }) => resto);
  };

  const enviar = async (e) => {
    e.preventDefault();
    const encontrados = validar(datos);
    setErrores(encontrados);
    setError('');
    if (Object.keys(encontrados).length) {
      setError('Revisá los datos marcados.');
      setTimeout(() => document.querySelector('[aria-invalid="true"]')?.focus(), 30);
      return;
    }
    setEnviando(true);
    try {
      await registrarse({
        ...datos,
        nombre: datos.nombre.trim(),
        apellido: datos.apellido.trim(),
        dni: datos.dni.trim(),
        correoElectronico: datos.correoElectronico.trim(),
        telefono: datos.telefono.trim(),
      });
      navigate(volver ?? '/', { replace: true, state: { bienvenida: true } });
    } catch (err) {
      setErrores(err.errores ?? {});
      setError(err.message);
      setEnviando(false);
    }
  };

  return (
    <PantallaAcceso>
      <main className="w-full max-w-[560px] rounded-2xl bg-background px-6 py-8 shadow-2xl sm:px-9">
        <h1 className="text-center font-serif text-3xl font-semibold text-foreground">Crear cuenta</h1>
        <p className="mt-2 text-center text-sm text-muted-foreground">
          Registrate como cliente para reservar excursiones.
        </p>

        {error && (
          <p className="mt-4 flex items-start gap-2 rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive" role="alert">
            <AlertCircle className="mt-0.5 size-4 shrink-0" aria-hidden="true" /> {error}
          </p>
        )}

        <form onSubmit={enviar} noValidate className="mt-5 flex flex-col gap-4">
          <RequiredLegend />
          <div className="grid gap-4 sm:grid-cols-2">
            {CAMPOS.map(({ campo, label, hint, ancho, ...resto }) => (
              <FormField
                key={campo}
                id={`r-${campo}`}
                label={label}
                required
                error={errores[campo]}
                hint={hint}
                className={ancho ? 'sm:col-span-2' : undefined}
              >
                <Input
                  id={`r-${campo}`}
                  name={campo}
                  type={resto.type ?? 'text'}
                  value={datos[campo]}
                  onChange={cambiar(campo)}
                  aria-invalid={errores[campo] ? true : undefined}
                  aria-required="true"
                  className="h-10 bg-card"
                  {...resto}
                />
              </FormField>
            ))}
          </div>
          <Button type="submit" className="mt-2 h-11 font-semibold" disabled={enviando}>
            {enviando && <Loader2 className="animate-spin" aria-hidden="true" />}
            Crear cuenta
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-foreground">
          ¿Ya tenés cuenta?{' '}
          <Link
            to={`/ingresar${volver ? `?volver=${encodeURIComponent(volver)}` : ''}`}
            className="font-semibold text-warning hover:underline"
          >
            Iniciá sesión
          </Link>
        </p>
      </main>
    </PantallaAcceso>
  );
}
