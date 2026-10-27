// Generado con v0 y adaptado a React + Vite (react-router-dom).
import { Link } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import logo from '@/assets/logo.png';
import { Button } from '@/components/ui/button';
export function GuiaLayout({ guia, onCerrarSesion, children }) {
  const iniciales = `${guia.apellido.charAt(0)}${guia.nombre.charAt(0)}`.toUpperCase();
  return (
    <div className="min-h-dvh bg-background">
      <header className="border-b border-border bg-card">
        <div className="flex items-center justify-between gap-3 px-4 py-3 sm:px-8 sm:py-4">
          <Link to="/guia/salidas" className="shrink-0 rounded-full">
            <img
              src={logo}
              alt="PataTrek — Senderismo y aventura"
              width={54}
              height={54}
              className="size-11 rounded-full sm:size-[54px]"
            />
          </Link>

          <div className="flex min-w-0 items-center gap-2 sm:gap-4">
            <div className="flex min-w-0 items-center gap-2.5">
              <span
                aria-hidden="true"
                className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground"
              >
                {iniciales}
              </span>
              <div className="flex min-w-0 flex-col leading-tight">
                <span className="truncate text-sm font-semibold text-foreground sm:text-base">
                  {guia.apellido}, {guia.nombre}
                </span>
                <span className="text-xs text-muted-foreground">Guía</span>
              </div>
            </div>
            <Button
              variant="ghost"
              onClick={onCerrarSesion}
              className="shrink-0 text-muted-foreground hover:text-foreground"
            >
              <LogOut aria-hidden="true" />
              <span className="sr-only sm:not-sr-only">Cerrar sesión</span>
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-[880px] px-4 py-8 sm:px-6 sm:py-12">{children}</main>
    </div>
  );
}
