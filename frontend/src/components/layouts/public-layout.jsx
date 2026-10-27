// Layout de Visitante / Cliente. Generado con v0 y adaptado a React + Vite (react-router-dom).
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import logo from '@/assets/logo.png';

/**
 * onIniciarSesion / onRegistrarme: se conectan en el módulo de Autenticación.
 * Mientras no estén, los botones se muestran deshabilitados.
 */
export function PublicLayout({ onIniciarSesion, onRegistrarme, children }) {
  return (
    <div className="flex min-h-dvh flex-col bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex w-full max-w-[1120px] items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <Link to="/" aria-label="PataTrek, ir al inicio" className="shrink-0">
            <img src={logo} alt="PataTrek" className="size-[54px] rounded-full" />
          </Link>
          <div className="flex items-center gap-1 sm:gap-2">
            <Button
              variant="ghost"
              onClick={onIniciarSesion}
              disabled={!onIniciarSesion}
              title={onIniciarSesion ? undefined : 'Disponible en el módulo de Autenticación'}
            >
              Iniciar sesión
            </Button>
            <Button
              onClick={onRegistrarme}
              disabled={!onRegistrarme}
              title={onRegistrarme ? undefined : 'Disponible en el módulo de Autenticación'}
            >
              Registrarme
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-[1120px] flex-1 px-4 py-8 sm:px-6 sm:py-12">{children}</main>

      <footer className="border-t border-border">
        <p className="mx-auto max-w-[1120px] px-4 py-6 text-center text-sm text-muted-foreground sm:px-6">
          PataTrek · Senderismo &amp; Aventura · Patagonia argentina
        </p>
      </footer>
    </div>
  );
}
