// Layout de Visitante / Cliente. Generado con v0 y adaptado a React + Vite (react-router-dom).
import { Link, NavLink } from 'react-router-dom';
import { LayoutDashboard, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/button';
import logo from '@/assets/logo.png';

const PANEL_POR_ROL = { ADMINISTRADOR: '/admin/excursiones', GUIA: '/guia/salidas' };

/**
 * onIniciarSesion / onRegistrarme: CU-02 y CU-01 (si no se pasan, los botones quedan deshabilitados).
 * usuario: { rol, nombre, apellido } de quien inició sesión (null si es un Visitante).
 * onCerrarSesion: CU-03 Cerrar sesión.
 */
export function PublicLayout({ usuario, onIniciarSesion, onRegistrarme, onCerrarSesion, children }) {
  return (
    <div className="flex min-h-dvh flex-col bg-background">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex w-full max-w-[1120px] items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <Link to="/" aria-label="PataTrek, ir al inicio" className="shrink-0">
            <img src={logo} alt="PataTrek" className="size-[54px] rounded-full" />
          </Link>
          {usuario ? (
            <nav className="flex items-center gap-1 sm:gap-3" aria-label="Menú del usuario">
              <NavLink
                to="/"
                end
                className={({ isActive }) =>
                  `hidden rounded-md px-2.5 py-1.5 text-sm hover:bg-muted sm:inline ${isActive ? 'font-semibold text-foreground' : 'text-muted-foreground'}`
                }
              >
                Excursiones
              </NavLink>
              {usuario.rol === 'CLIENTE' ? (
                <NavLink
                  to="/mis-reservas"
                  className={({ isActive }) =>
                    `rounded-md px-2.5 py-1.5 text-sm hover:bg-muted ${isActive ? 'font-semibold text-foreground' : 'text-muted-foreground'}`
                  }
                >
                  Mis reservas
                </NavLink>
              ) : (
                <Link
                  to={PANEL_POR_ROL[usuario.rol] ?? '/'}
                  className="inline-flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-sm text-muted-foreground hover:bg-muted"
                >
                  <LayoutDashboard className="size-4" aria-hidden="true" /> Mi panel
                </Link>
              )}
              <span className="ml-1 flex items-center gap-2">
                <span
                  className="flex size-8 items-center justify-center rounded-full bg-warning text-xs font-semibold text-white"
                  aria-hidden="true"
                >
                  {(usuario.nombre?.[0] ?? '') + (usuario.apellido?.[0] ?? '')}
                </span>
                <span className="hidden text-sm text-foreground md:inline">
                  {usuario.apellido}, {usuario.nombre}
                </span>
              </span>
              {onCerrarSesion && (
                <Button
                  variant="ghost"
                  size="icon-lg"
                  onClick={onCerrarSesion}
                  aria-label="Cerrar sesión"
                  title="Cerrar sesión"
                >
                  <LogOut aria-hidden="true" />
                </Button>
              )}
            </nav>
          ) : (
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
          )}
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
