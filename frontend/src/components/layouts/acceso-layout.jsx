// Marco simple para el Cliente y el Participante (acceso por ID de reserva + DNI).
import { Link } from 'react-router-dom';
import logo from '@/assets/logo.png';

/** derecha: contenido del extremo derecho del encabezado (usuario o texto de acceso). */
export function AccesoLayout({ derecha, children }) {
  return (
    <div className="min-h-dvh bg-background">
      <header className="border-b border-border bg-card">
        <div className="flex items-center justify-between gap-3 px-4 py-3 sm:px-8 sm:py-4">
          <Link to="/" aria-label="PataTrek, ir al inicio" className="shrink-0 rounded-full">
            <img src={logo} alt="" width={54} height={54} className="size-11 rounded-full sm:size-[54px]" />
          </Link>
          <div className="min-w-0">{derecha}</div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-[680px] px-4 py-8 sm:px-6 sm:py-12">{children}</main>
    </div>
  );
}
