import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import logo from '@/assets/logo.png';

/**
 * Fondo de las pantallas de ingreso y registro (prototipo pág. 3).
 * Si existe frontend/public/fondo-login.jpg (la foto del prototipo) se usa como fondo;
 * si no, queda el degradé de montaña.
 */
export function PantallaAcceso({ children }) {
  return (
    <div
      className="relative flex min-h-dvh flex-col items-center justify-center px-4 py-10"
      style={{
        backgroundImage:
          "linear-gradient(rgba(20,30,28,0.25), rgba(20,30,28,0.35)), url('/fondo-login.jpg'), linear-gradient(160deg, #4b3f72 0%, #8a5a7a 28%, #c7795a 48%, #2d6a55 72%, #1e4a3d 100%)",
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }}
    >
      <Link
        to="/"
        className="absolute top-4 left-4 inline-flex items-center gap-1.5 rounded-lg bg-black/25 px-3 py-1.5 text-sm text-white backdrop-blur-sm hover:bg-black/40"
      >
        <ArrowLeft className="size-4" aria-hidden="true" /> Volver al catálogo
      </Link>
      {children}
      <img
        src={logo}
        alt=""
        aria-hidden="true"
        className="pointer-events-none absolute right-4 bottom-4 hidden size-24 rounded-full shadow-lg sm:block"
      />
    </div>
  );
}

/** Solo se aceptan rutas internas para volver después de ingresar (evita redirecciones a otros sitios). */
export function rutaSegura(volver) {
  return volver && volver.startsWith('/') && !volver.startsWith('//') ? volver : null;
}
